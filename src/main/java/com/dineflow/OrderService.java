
package com.dineflow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OrderService {

    private static final double GST_RATE = 0.05;

    private static final Gson gson = new Gson();

    public static String placeOrder(
            String customerName,
            String cartJson,
            String paymentMethod,
            double discountAmount) {

        Connection con = null;

        try {

            // Validate customer details
            if (customerName == null || customerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer name is required.");
            }

            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                throw new IllegalArgumentException("Payment method is required.");
            }

            // Parse cart JSON
            JsonArray cart = JsonParser.parseString(cartJson).getAsJsonArray();

            if (cart.isEmpty()) {
                throw new IllegalArgumentException("Shopping cart is empty.");
            }

            if (discountAmount < 0) {
                throw new IllegalArgumentException("Invalid discount amount.");
            }

            con = DB.getConnection();

            if (con == null) {
                throw new SQLException("Unable to connect to database.");
            }

            con.setAutoCommit(false);

            BigDecimal subtotal = BigDecimal.ZERO;
            Map<Integer, Integer> originalStock = new LinkedHashMap<>();
            Map<Integer, Integer> totalOrderedQuantity = new LinkedHashMap<>();

            // Calculate subtotal using actual database prices
            for (JsonElement element : cart) {

                JsonObject cartItem = element.getAsJsonObject();

                int itemId = cartItem.get("itemId").getAsInt();
                int quantity = cartItem.get("quantity").getAsInt();

                if (quantity <= 0) {
                    throw new IllegalArgumentException("Invalid item quantity.");
                }

                String sql = """
                    SELECT price, stock_quantity, is_active
                    FROM menu_items
                    WHERE item_id = ?
                    FOR UPDATE
                    """;

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, itemId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                "Menu item not found: " + itemId
                            );
                        }

                        if (!rs.getBoolean("is_active")) {
                            throw new IllegalArgumentException(
                                "This menu item is unavailable."
                            );
                        }

                        int availableStock = rs.getInt("stock_quantity");

                        int alreadyRequested =
                                totalOrderedQuantity.getOrDefault(itemId, 0);

                        if (availableStock < alreadyRequested + quantity) {
                            throw new IllegalArgumentException(
                                "Insufficient stock for item ID: " + itemId
                            );
                        }

                        originalStock.putIfAbsent(itemId, availableStock);

                        totalOrderedQuantity.merge(itemId, quantity, Integer::sum);

                        BigDecimal price = rs.getBigDecimal("price");

                        subtotal = subtotal.add(
                            price.multiply(BigDecimal.valueOf(quantity))
                        );
                    }
                }
            }

            subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

            BigDecimal tax = subtotal
                    .multiply(BigDecimal.valueOf(GST_RATE))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal discount = BigDecimal.valueOf(discountAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            if (discount.compareTo(subtotal.add(tax)) > 0) {
                throw new IllegalArgumentException(
                    "Discount cannot exceed the bill amount."
                );
            }

            BigDecimal total = subtotal.add(tax).subtract(discount)
                    .setScale(2, RoundingMode.HALF_UP);

            // Insert restaurant order
            String orderSql = """
                INSERT INTO restaurant_orders
                (customer_name, order_date, subtotal, tax_amount,
                 discount_amount, total_amount, order_status)
                VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?)
                """;

            int orderId;

            try (PreparedStatement ps = con.prepareStatement(
                    orderSql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, customerName.trim());
                ps.setBigDecimal(2, subtotal);
                ps.setBigDecimal(3, tax);
                ps.setBigDecimal(4, discount);
                ps.setBigDecimal(5, total);
                ps.setString(6, "COMPLETED");

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {

                    if (!keys.next()) {
                        throw new SQLException("Order ID was not generated.");
                    }

                    orderId = keys.getInt(1);
                }
            }

            // Insert order items and reduce inventory
            String itemSql = """
                INSERT INTO order_items
                (order_id, item_id, item_name, quantity,
                 unit_price, line_total)
                SELECT ?, item_id, item_name, ?, price, price * ?
                FROM menu_items
                WHERE item_id = ?
                """;

            String stockSql = """
                UPDATE menu_items
                SET stock_quantity = stock_quantity - ?
                WHERE item_id = ? AND stock_quantity >= ?
                """;

            for (JsonElement element : cart) {

                JsonObject cartItem = element.getAsJsonObject();

                int itemId = cartItem.get("itemId").getAsInt();
                int quantity = cartItem.get("quantity").getAsInt();

                try (PreparedStatement ps = con.prepareStatement(itemSql)) {

                    ps.setInt(1, orderId);
                    ps.setInt(2, quantity);
                    ps.setInt(3, quantity);
                    ps.setInt(4, itemId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Unable to save order item.");
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(stockSql)) {

                    ps.setInt(1, quantity);
                    ps.setInt(2, itemId);
                    ps.setInt(3, quantity);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Unable to update stock.");
                    }
                }
            }
         // Check for low-stock vendor notifications
            String itemDetailsSql = """
                SELECT item_name, vendor_phone
                FROM menu_items
                WHERE item_id = ?
                """;

            String notificationSql = """
                INSERT INTO vendor_sms_notifications
                (item_id, vendor_phone, message, notification_status)
                VALUES (?, ?, ?, 'PENDING')
                """;

            for (Integer itemId : originalStock.keySet()) {

                int oldStock = originalStock.get(itemId);

                int orderedQuantity = totalOrderedQuantity.get(itemId);

                int newStock = oldStock - orderedQuantity;

                // Notify only when stock crosses from above 5 to 5 or below.
                if (oldStock > 5 && newStock <= 5) {

                    try (PreparedStatement ps =
                            con.prepareStatement(itemDetailsSql)) {

                        ps.setInt(1, itemId);

                        try (ResultSet rs = ps.executeQuery()) {

                            if (rs.next()) {

                                String itemName = rs.getString("item_name");
                                String vendorPhone = rs.getString("vendor_phone");

                                if (vendorPhone != null && !vendorPhone.isBlank()) {

                                    String message =
                                            "DineFlow Alert: " + itemName
                                            + " is low on stock. Current quantity: "
                                            + newStock
                                            + ". Please arrange new stock.";

                                    try (PreparedStatement insert =
                                            con.prepareStatement(notificationSql)) {

                                        insert.setInt(1, itemId);
                                        insert.setString(2, vendorPhone);
                                        insert.setString(3, message);

                                        insert.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save payment record
            String paymentSql = """
                INSERT INTO payments
                (order_id, payment_method, amount,
                 payment_status, payment_date)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

            try (PreparedStatement ps = con.prepareStatement(paymentSql)) {

                ps.setInt(1, orderId);
                ps.setString(2, paymentMethod);
                ps.setBigDecimal(3, total);
                ps.setString(4, "PAID");

                ps.executeUpdate();
            }

            // Commit all database changes
            con.commit();
         // Process vendor notifications only after transaction commits.
            SmsNotificationService.processPendingNotifications();

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("success", true);
            response.put("orderId", orderId);
            response.put("message", "Order placed successfully.");
            response.put("subtotal", subtotal);
            response.put("tax", tax);
            response.put("discount", discount);
            response.put("total", total);

            return gson.toJson(response);

        } catch (Exception e) {

            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    rollbackError.printStackTrace();
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("success", false);
            response.put("message", e.getMessage());

            return gson.toJson(response);

        } finally {

            if (con != null) {

                try {
                    con.setAutoCommit(true);
                    con.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
