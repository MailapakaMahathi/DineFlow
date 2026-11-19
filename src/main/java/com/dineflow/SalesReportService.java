package com.dineflow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SalesReportService {

    private static final String PAID_STATUSES =
            "('PAID', 'SUCCESS')";

    // Main method called by the JavaScript bridge
    public static String getSalesReportJson(
            String period, String startDate, String endDate) {

        JsonObject report = new JsonObject();

        try (Connection conn = DB.getConnection()) {

            if (conn == null) {
                throw new SQLException("Database connection failed.");
            }

            DateRange range = getDateRange(period, startDate, endDate);

            report.addProperty("success", true);
            report.addProperty("period", period == null ? "WEEK" : period);
            report.addProperty("startDate",
                    range.start == null ? "" : range.start.toString());
            report.addProperty("endDate",
                    range.endExclusive == null ? "" :
                            range.endExclusive.minusDays(1).toString());

            report.add("summary", getSummary(conn, range));
            report.add("trend", getRevenueTrend(conn, range));
            report.add("topSellingItems", getTopSellingItems(conn, range));
            report.add("paymentAnalysis", getPaymentAnalysis(conn, range));
            report.add("breakdown", getSalesBreakdown(conn, range));

        } catch (Exception e) {
            report = new JsonObject();
            report.addProperty("success", false);
            report.addProperty("message", e.getMessage());
            e.printStackTrace();
        }

        return report.toString();
    }

    // --------------------------------------------------
    // DATE RANGE
    // --------------------------------------------------

    private static DateRange getDateRange(
            String period, String startDate, String endDate) {

        // Custom date range takes priority when both dates are supplied.
        if (startDate != null && !startDate.isBlank()
                && endDate != null && !endDate.isBlank()) {

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (end.isBefore(start)) {
                throw new IllegalArgumentException(
                        "End date cannot be before start date.");
            }

            return new DateRange(start, end.plusDays(1));
        }

        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate endExclusive = today.plusDays(1);

        String selectedPeriod =
                period == null ? "WEEK" : period.toUpperCase();

        switch (selectedPeriod) {

            case "TODAY":
                start = today;
                break;

            case "WEEK":
                start = today.with(DayOfWeek.MONDAY);
                break;

            case "MONTH":
                start = today.withDayOfMonth(1);
                break;

            case "YEAR":
                start = today.withDayOfYear(1);
                break;

            case "ALL":
                return new DateRange(null, null);

            default:
                start = today.with(DayOfWeek.MONDAY);
                break;
        }

        return new DateRange(start, endExclusive);
    }

    private static class DateRange {
        LocalDate start;
        LocalDate endExclusive;

        DateRange(LocalDate start, LocalDate endExclusive) {
            this.start = start;
            this.endExclusive = endExclusive;
        }
    }

    private static String addDateCondition(
            String sql, String tableAlias, DateRange range) {

        if (range.start != null) {
            sql += " AND " + tableAlias + ".order_date >= ?"
                 + " AND " + tableAlias + ".order_date < ?";
        }

        return sql;
    }

    private static void bindDateRange(
            PreparedStatement ps, int index, DateRange range)
            throws SQLException {

        if (range.start != null) {
            ps.setTimestamp(index,
                    java.sql.Timestamp.valueOf(range.start.atStartOfDay()));

            ps.setTimestamp(index + 1,
                    java.sql.Timestamp.valueOf(
                            range.endExclusive.atStartOfDay()));
        }
    }

    // --------------------------------------------------
    // 1. SUMMARY
    // --------------------------------------------------

    private static JsonObject getSummary(
            Connection conn, DateRange range) throws SQLException {

        JsonObject summary = new JsonObject();

        String sql =
                "SELECT COUNT(DISTINCT o.order_id) AS total_orders, "
              + "COALESCE(SUM(p.amount), 0) AS total_revenue "
              + "FROM restaurant_orders o "
              + "JOIN payments p ON o.order_id = p.order_id "
              + "AND UPPER(p.payment_status) IN " + PAID_STATUSES
              + " WHERE 1=1";

        sql = addDateCondition(sql, "o", range);

        double revenue = 0;
        int orders = 0;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            bindDateRange(ps, 1, range);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    orders = rs.getInt("total_orders");
                    revenue = rs.getDouble("total_revenue");
                }
            }
        }

        // Calculate items sold separately to avoid multiplying revenue
        // when an order contains multiple food items.
        String itemSql =
                "SELECT COALESCE(SUM(oi.quantity), 0) AS items_sold "
              + "FROM restaurant_orders o "
              + "JOIN payments p ON o.order_id = p.order_id "
              + "AND UPPER(p.payment_status) IN " + PAID_STATUSES
              + " JOIN order_items oi ON o.order_id = oi.order_id "
              + "WHERE 1=1";

        itemSql = addDateCondition(itemSql, "o", range);

        int itemsSold = 0;

        try (PreparedStatement ps = conn.prepareStatement(itemSql)) {

            bindDateRange(ps, 1, range);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    itemsSold = rs.getInt("items_sold");
                }
            }
        }

        summary.addProperty("totalRevenue", revenue);
        summary.addProperty("totalOrders", orders);
        summary.addProperty("averageOrderValue",
                orders == 0 ? 0 : revenue / orders);
        summary.addProperty("itemsSold", itemsSold);

        return summary;
    }

    // --------------------------------------------------
    // 2. REVENUE TREND
    // --------------------------------------------------

    private static JsonArray getRevenueTrend(
            Connection conn, DateRange range) throws SQLException {

        JsonArray result = new JsonArray();

        String sql =
                "SELECT DATE(o.order_date) AS sales_date, "
              + "COUNT(DISTINCT o.order_id) AS total_orders, "
              + "COALESCE(SUM(p.amount), 0) AS revenue "
              + "FROM restaurant_orders o "
              + "JOIN payments p ON o.order_id = p.order_id "
              + "AND UPPER(p.payment_status) IN " + PAID_STATUSES
              + " WHERE 1=1";

        sql = addDateCondition(sql, "o", range);
        sql += " GROUP BY DATE(o.order_date) ORDER BY sales_date";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            bindDateRange(ps, 1, range);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    JsonObject row = new JsonObject();

                    row.addProperty("date",
                            rs.getDate("sales_date").toString());
                    row.addProperty("orders",
                            rs.getInt("total_orders"));
                    row.addProperty("revenue",
                            rs.getDouble("revenue"));

                    result.add(row);
                }
            }
        }

        return result;
    }

    // --------------------------------------------------
    // 3. TOP-SELLING ITEMS
    // --------------------------------------------------

    private static JsonArray getTopSellingItems(
            Connection conn, DateRange range) throws SQLException {

        JsonArray result = new JsonArray();

        String sql =
                "SELECT oi.item_name, "
              + "SUM(oi.quantity) AS quantity_sold, "
              + "SUM(oi.line_total) AS item_revenue "
              + "FROM restaurant_orders o "
              + "JOIN payments p ON o.order_id = p.order_id "
              + "AND UPPER(p.payment_status) IN " + PAID_STATUSES
              + " JOIN order_items oi ON o.order_id = oi.order_id "
              + "WHERE 1=1";

        sql = addDateCondition(sql, "o", range);

        sql += " GROUP BY oi.item_name "
             + "ORDER BY quantity_sold DESC LIMIT 10";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            bindDateRange(ps, 1, range);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    JsonObject row = new JsonObject();

                    row.addProperty("itemName",
                            rs.getString("item_name"));
                    row.addProperty("quantitySold",
                            rs.getInt("quantity_sold"));
                    row.addProperty("revenue",
                            rs.getDouble("item_revenue"));

                    result.add(row);
                }
            }
        }

        return result;
    }

    // --------------------------------------------------
    // 4. PAYMENT ANALYSIS
    // --------------------------------------------------

    private static JsonArray getPaymentAnalysis(
            Connection conn, DateRange range) throws SQLException {

        JsonArray result = new JsonArray();

        String sql =
                "SELECT p.payment_method, "
              + "COUNT(DISTINCT o.order_id) AS total_orders, "
              + "COALESCE(SUM(p.amount), 0) AS total_revenue "
              + "FROM restaurant_orders o "
              + "JOIN payments p ON o.order_id = p.order_id "
              + "AND UPPER(p.payment_status) IN " + PAID_STATUSES
              + " WHERE 1=1";

        sql = addDateCondition(sql, "o", range);

        sql += " GROUP BY p.payment_method "
             + "ORDER BY total_revenue DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            bindDateRange(ps, 1, range);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    JsonObject row = new JsonObject();

                    row.addProperty("paymentMethod",
                            rs.getString("payment_method"));
                    row.addProperty("orders",
                            rs.getInt("total_orders"));
                    row.addProperty("revenue",
                            rs.getDouble("total_revenue"));

                    result.add(row);
                }
            }
        }

        return result;
    }

    // --------------------------------------------------
    // 5. SALES BREAKDOWN
    // --------------------------------------------------

    private static JsonArray getSalesBreakdown(
            Connection conn, DateRange range) throws SQLException {

        Map<String, JsonObject> dailyData = new LinkedHashMap<>();

        // First retrieve daily revenue and order count.
        String orderSql =
                "SELECT DATE(o.order_date) AS sales_date, "
              + "COUNT(DISTINCT o.order_id) AS total_orders, "
              + "COALESCE(SUM(p.amount), 0) AS revenue "
              + "FROM restaurant_orders o "
              + "JOIN payments p ON o.order_id = p.order_id "
              + "AND UPPER(p.payment_status) IN " + PAID_STATUSES
              + " WHERE 1=1";

        orderSql = addDateCondition(orderSql, "o", range);
        orderSql += " GROUP BY DATE(o.order_date) ORDER BY sales_date";

        try (PreparedStatement ps = conn.prepareStatement(orderSql)) {

            bindDateRange(ps, 1, range);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    String date = rs.getDate("sales_date").toString();

                    JsonObject row = new JsonObject();
                    row.addProperty("date", date);
                    row.addProperty("orders",
                            rs.getInt("total_orders"));
                    row.addProperty("revenue",
                            rs.getDouble("revenue"));
                    row.addProperty("itemsSold", 0);

                    dailyData.put(date, row);
                }
            }
        }

        // Retrieve daily item quantities separately.
        String itemSql =
                "SELECT DATE(o.order_date) AS sales_date, "
              + "COALESCE(SUM(oi.quantity), 0) AS items_sold "
              + "FROM restaurant_orders o "
              + "JOIN payments p ON o.order_id = p.order_id "
              + "AND UPPER(p.payment_status) IN " + PAID_STATUSES
              + " JOIN order_items oi ON o.order_id = oi.order_id "
              + "WHERE 1=1";

        itemSql = addDateCondition(itemSql, "o", range);
        itemSql += " GROUP BY DATE(o.order_date)";

        try (PreparedStatement ps = conn.prepareStatement(itemSql)) {

            bindDateRange(ps, 1, range);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    String date = rs.getDate("sales_date").toString();

                    if (dailyData.containsKey(date)) {
                        dailyData.get(date).addProperty(
                                "itemsSold", rs.getInt("items_sold"));
                    }
                }
            }
        }

        JsonArray result = new JsonArray();

        for (JsonObject row : dailyData.values()) {
            result.add(row);
        }

        return result;
    }
}