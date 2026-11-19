
package com.dineflow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardService {

    // Retrieve all dashboard statistics
    public static Map<String, Object> getDashboardStats() {

        Map<String, Object> stats = new LinkedHashMap<>();

        // Default values
        stats.put("revenue", 0.0);
        stats.put("orders", 0);
        stats.put("menuItems", 0);
        stats.put("lowStock", 0);
        stats.put("inStock", 0);
        stats.put("outOfStock", 0);

        String sql =
            "SELECT " +

            // Today's revenue
            "(SELECT COALESCE(SUM(total_amount), 0) " +
            " FROM restaurant_orders " +
            " WHERE DATE(order_date) = CURDATE()) AS revenue, " +

            // Today's total orders
            "(SELECT COUNT(*) " +
            " FROM restaurant_orders " +
            " WHERE DATE(order_date) = CURDATE()) AS orders, " +

            // Active menu items
            "(SELECT COUNT(*) " +
            " FROM menu_items " +
            " WHERE is_active = TRUE) AS menu_items, " +

            // Low stock items (1 to 5)
            "(SELECT COUNT(*) " +
            " FROM menu_items " +
            " WHERE is_active = TRUE " +
            " AND stock_quantity BETWEEN 1 AND 5) AS low_stock, " +

            // Items in stock
            "(SELECT COUNT(*) " +
            " FROM menu_items " +
            " WHERE is_active = TRUE " +
            " AND stock_quantity > 5) AS in_stock, " +

            // Items out of stock
            "(SELECT COUNT(*) " +
            " FROM menu_items " +
            " WHERE is_active = TRUE " +
            " AND stock_quantity = 0) AS out_of_stock";

        try (Connection con = DB.getConnection()) {

            if (con == null) {
                System.out.println(
                    "Dashboard: Database connection unavailable."
                );
                return stats;
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    stats.put("revenue",
                        rs.getDouble("revenue"));

                    stats.put("orders",
                        rs.getInt("orders"));

                    stats.put("menuItems",
                        rs.getInt("menu_items"));

                    stats.put("lowStock",
                        rs.getInt("low_stock"));

                    stats.put("inStock",
                        rs.getInt("in_stock"));

                    stats.put("outOfStock",
                        rs.getInt("out_of_stock"));
                }
            }

            System.out.println(
                "Dashboard statistics retrieved successfully!"
            );

        } catch (SQLException e) {

            System.out.println(
                "Unable to retrieve dashboard statistics."
            );

            e.printStackTrace();
        }

        return stats;
    }
    // Retrieve the latest 10 restaurant orders
    public static String getRecentOrdersJson() {

        StringBuilder json = new StringBuilder("[");

        String sql =
            "SELECT o.order_id, " +
            "COALESCE(o.customer_name, 'Walk-in Customer') AS customer_name, " +
            "DATE_FORMAT(o.order_date, '%d %b %Y %h:%i %p') AS order_date, " +
            "o.total_amount, " +
            "COALESCE(p.payment_method, 'N/A') AS payment_method, " +
            "o.order_status " +
            "FROM restaurant_orders o " +
            "LEFT JOIN payments p ON o.order_id = p.order_id " +
            "ORDER BY o.order_date DESC " +
            "LIMIT 10";

        boolean first = true;

        try (Connection con = DB.getConnection()) {

            if (con == null) {
                return "[]";
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    json.append("{");

                    json.append("\"orderId\":")
                        .append(rs.getInt("order_id")).append(",");

                    json.append("\"customer\":\"")
                        .append(escapeJson(
                            rs.getString("customer_name")
                        )).append("\",");

                    json.append("\"date\":\"")
                        .append(escapeJson(
                            rs.getString("order_date")
                        )).append("\",");

                    json.append("\"amount\":")
                        .append(rs.getBigDecimal("total_amount")).append(",");

                    json.append("\"payment\":\"")
                        .append(escapeJson(
                            rs.getString("payment_method")
                        )).append("\",");

                    json.append("\"status\":\"")
                        .append(escapeJson(
                            rs.getString("order_status")
                        )).append("\"");

                    json.append("}");

                    first = false;
                }
            }

        } catch (SQLException e) {

            System.out.println("Unable to retrieve recent orders.");
            e.printStackTrace();

            return "[]";
        }

        json.append("]");

        return json.toString();
    }


    // Escape special characters for JSON
    private static String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
    public static List<MenuItem> getAllMenuItems() {

        List<MenuItem> menuItems = new ArrayList<>();

        String sql = """
            SELECT item_id, item_name, category, price,
                   stock_quantity, vendor_name, vendor_phone, is_active
            FROM menu_items
            WHERE is_active = 1
            ORDER BY category, item_name
            """;

        try (Connection con = DB.getConnection()) {

            if (con == null) {
                return menuItems;
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    MenuItem item = new MenuItem(
                        rs.getInt("item_id"),
                        rs.getString("item_name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity"),
                        rs.getString("vendor_name"),
                        rs.getString("vendor_phone"),
                        rs.getBoolean("is_active")
                    );

                    menuItems.add(item);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving menu items: " + e.getMessage());
        }

        return menuItems;
    }
    

}
