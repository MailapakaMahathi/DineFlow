
package com.dineflow;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

public class BillingService {

    private static final Gson gson = new Gson();

    // Retrieve all orders with their associated payment information.
    public static String getBillingOrdersJson() {

        List<Map<String, Object>> orders = new ArrayList<>();

        String sql = """
            SELECT
                o.order_id,
                o.customer_name,
                o.order_date,
                o.subtotal,
                o.tax_amount,
                o.discount_amount,
                o.total_amount,
                p.payment_method,
                p.payment_status
            FROM restaurant_orders o
            LEFT JOIN payments p ON o.order_id = p.order_id
            ORDER BY o.order_date DESC
            """;

        try (Connection con = DB.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection failed.");
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Map<String, Object> order = new LinkedHashMap<>();

                    order.put("orderId", rs.getInt("order_id"));
                    order.put("customerName", rs.getString("customer_name"));
                    order.put("orderDate", rs.getTimestamp("order_date").toString());
                    order.put("subtotal", rs.getBigDecimal("subtotal"));
                    order.put("tax", rs.getBigDecimal("tax_amount"));
                    order.put("discount", rs.getBigDecimal("discount_amount"));
                    order.put("total", rs.getBigDecimal("total_amount"));
                    order.put("paymentMethod", rs.getString("payment_method"));
                    order.put("paymentStatus", rs.getString("payment_status"));

                    orders.add(order);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }

        return gson.toJson(orders);
    }

    // Retrieve Billing dashboard statistics.
    public static String getBillingStatsJson() {

        Map<String, Object> stats = new LinkedHashMap<>();

        String sql = """
            SELECT
                COUNT(DISTINCT o.order_id) AS total_orders,
                COALESCE(SUM(o.total_amount), 0) AS total_revenue,
                COALESCE(SUM(
                    CASE
                        WHEN UPPER(p.payment_status) IN ('PAID', 'SUCCESS')
                        THEN 1 ELSE 0
                    END
                ), 0) AS successful_payments,
                COALESCE(SUM(
                    CASE
                        WHEN UPPER(p.payment_status) = 'PENDING'
                        THEN 1 ELSE 0
                    END
                ), 0) AS pending_payments
            FROM restaurant_orders o
            LEFT JOIN payments p ON o.order_id = p.order_id
            """;

        try (Connection con = DB.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection failed.");
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    stats.put("success", true);
                    stats.put("totalOrders", rs.getInt("total_orders"));
                    stats.put("totalRevenue", rs.getBigDecimal("total_revenue"));
                    stats.put("successfulPayments",
                            rs.getInt("successful_payments"));
                    stats.put("pendingPayments",
                            rs.getInt("pending_payments"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            stats.clear();
            stats.put("success", false);
            stats.put("message", e.getMessage());
        }

        return gson.toJson(stats);
    }

    // Generate an invoice using the original order item prices.
    public static String generateInvoice(int orderId) {

        try {

            String customerName;
            String paymentMethod;
            BigDecimal subtotal;
            BigDecimal tax;
            BigDecimal discount;
            BigDecimal total;

            List<CartLine> invoiceItems = new ArrayList<>();

            try (Connection con = DB.getConnection()) {

                if (con == null) {
                    throw new SQLException("Database connection failed.");
                }

                String orderSql = """
                    SELECT
                        o.customer_name,
                        o.subtotal,
                        o.tax_amount,
                        o.discount_amount,
                        o.total_amount,
                        p.payment_method
                    FROM restaurant_orders o
                    LEFT JOIN payments p ON o.order_id = p.order_id
                    WHERE o.order_id = ?
                    """;

                try (PreparedStatement ps = con.prepareStatement(orderSql)) {

                    ps.setInt(1, orderId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException("Order not found.");
                        }

                        customerName = rs.getString("customer_name");
                        subtotal = rs.getBigDecimal("subtotal");
                        tax = rs.getBigDecimal("tax_amount");
                        discount = rs.getBigDecimal("discount_amount");
                        total = rs.getBigDecimal("total_amount");
                        paymentMethod = rs.getString("payment_method");

                        if (paymentMethod == null) {
                            paymentMethod = "N/A";
                        }
                    }
                }

                String itemsSql = """
                    SELECT item_id, item_name, quantity, unit_price
                    FROM order_items
                    WHERE order_id = ?
                    ORDER BY order_item_id
                    """;

                try (PreparedStatement ps = con.prepareStatement(itemsSql)) {

                    ps.setInt(1, orderId);

                    try (ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {

                            int itemId = rs.getInt("item_id");
                            String itemName = rs.getString("item_name");
                            int quantity = rs.getInt("quantity");
                            double unitPrice =
                                    rs.getBigDecimal("unit_price").doubleValue();

                            MenuItem item = new MenuItem(
                                itemId,
                                itemName,
                                "",
                                unitPrice,
                                0,
                                "",
                                "",
                                true
                            );

                            invoiceItems.add(new CartLine(item, quantity));
                        }
                    }
                }
            }

            if (invoiceItems.isEmpty()) {
                throw new SQLException("No items found for this order.");
            }

            Path invoiceDirectory = Path.of(
                System.getProperty("user.home"),
                "DineFlowInvoices"
            );

            Files.createDirectories(invoiceDirectory);

            File invoiceFile = invoiceDirectory
                    .resolve("Invoice_" + orderId + ".pdf")
                    .toFile();

            InvoicePdfGenerator.generateInvoice(
                invoiceFile.getAbsolutePath(),
                orderId,
                customerName,
                invoiceItems,
                subtotal.doubleValue(),
                tax.doubleValue(),
                discount.doubleValue(),
                total.doubleValue(),
                paymentMethod
            );

            if (!invoiceFile.exists() || invoiceFile.length() == 0) {
                throw new SQLException("Invoice PDF could not be generated.");
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Invoice generated successfully.");
            response.put("filePath", invoiceFile.getAbsolutePath());

            return gson.toJson(response);

        } catch (Exception e) {

            e.printStackTrace();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return gson.toJson(response);
        }
    }
}
