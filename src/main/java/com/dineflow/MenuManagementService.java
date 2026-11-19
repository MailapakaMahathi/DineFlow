
package com.dineflow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MenuManagementService {

    // 1. Retrieve all menu items
    public static List<MenuItem> getAllMenuItems() {

        List<MenuItem> items = new ArrayList<>();

        String sql = """
            SELECT item_id, item_name, category, price,
                   stock_quantity, vendor_name, vendor_phone, is_active
            FROM menu_items
            ORDER BY category, item_name
            """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                items.add(new MenuItem(
                    rs.getInt("item_id"),
                    rs.getString("item_name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("stock_quantity"),
                    rs.getString("vendor_name"),
                    rs.getString("vendor_phone"),
                    rs.getBoolean("is_active")
                ));
            }

            System.out.println("Menu items retrieved: " + items.size());

        } catch (SQLException e) {
            System.err.println("Unable to retrieve menu items.");
            e.printStackTrace();
        }

        return items;
    }


    // 2. Add a new menu item

 // 2. Add a new menu item with duplicate prevention.
 public static boolean addMenuItem(
         String itemName,
         String category,
         double price,
         int stockQuantity,
         String vendorName,
         String vendorPhone) {

     if (itemName == null || itemName.isBlank()
             || category == null || category.isBlank()
             || !Double.isFinite(price) || price <= 0
             || stockQuantity < 0) {

         System.err.println("Invalid menu item details.");
         return false;
     }

     String cleanName = itemName.trim();

     String checkSql = """
         SELECT item_id, is_active
         FROM menu_items
         WHERE LOWER(TRIM(item_name)) = LOWER(?)
         ORDER BY is_active DESC, item_id ASC
         FOR UPDATE
         """;

     String reactivateSql = """
         UPDATE menu_items
         SET category = ?,
             price = ?,
             stock_quantity = ?,
             vendor_name = ?,
             vendor_phone = ?,
             is_active = TRUE
         WHERE item_id = ?
         """;

     String insertSql = """
         INSERT INTO menu_items
         (item_name, category, price, stock_quantity,
          vendor_name, vendor_phone, is_active)
         VALUES (?, ?, ?, ?, ?, ?, TRUE)
         """;

     try (Connection con = DB.getConnection()) {

         if (con == null) {
             return false;
         }

         con.setAutoCommit(false);

         try {

             Integer inactiveItemId = null;

             try (PreparedStatement ps = con.prepareStatement(checkSql)) {

                 ps.setString(1, cleanName);

                 try (ResultSet rs = ps.executeQuery()) {

                     while (rs.next()) {

                         int existingId = rs.getInt("item_id");
                         boolean active = rs.getBoolean("is_active");

                         if (active) {

                             System.err.println(
                                 "Menu item already exists: " + cleanName
                             );

                             con.rollback();
                             return false;
                         }

                         if (inactiveItemId == null) {
                             inactiveItemId = existingId;
                         }
                     }
                 }
             }

             // Reactivate an existing inactive item.
             if (inactiveItemId != null) {

                 try (PreparedStatement ps =
                         con.prepareStatement(reactivateSql)) {

                     ps.setString(1, category.trim());
                     ps.setDouble(2, price);
                     ps.setInt(3, stockQuantity);
                     ps.setString(4, vendorName);
                     ps.setString(5, vendorPhone);
                     ps.setInt(6, inactiveItemId);

                     if (ps.executeUpdate() != 1) {
                         con.rollback();
                         return false;
                     }
                 }

                 con.commit();

                 System.out.println(
                     "Existing menu item reactivated. ID: "
                     + inactiveItemId
                 );

                 return true;
             }

             // Insert a completely new item.
             try (PreparedStatement ps =
                     con.prepareStatement(insertSql)) {

                 ps.setString(1, cleanName);
                 ps.setString(2, category.trim());
                 ps.setDouble(3, price);
                 ps.setInt(4, stockQuantity);
                 ps.setString(5, vendorName);
                 ps.setString(6, vendorPhone);

                 if (ps.executeUpdate() != 1) {
                     con.rollback();
                     return false;
                 }
             }

             con.commit();

             System.out.println("New menu item added: " + cleanName);
             return true;

         } catch (SQLException e) {

             con.rollback();
             throw e;

         }

     } catch (SQLException e) {

         System.err.println("Unable to add menu item.");
         e.printStackTrace();
         return false;
     }
 }


    // 3. Edit an existing menu item
    public static boolean updateMenuItem(
            int itemId,
            String itemName,
            String category,
            double price,
            int stockQuantity,
            String vendorName,
            String vendorPhone) {

        if (itemId <= 0
                || itemName == null || itemName.isBlank()
                || category == null || category.isBlank()
                || !Double.isFinite(price) || price <= 0
                || stockQuantity < 0) {

            System.err.println("Invalid menu item details.");
            return false;
        }

        String sql = """
            UPDATE menu_items
            SET item_name = ?,
                category = ?,
                price = ?,
                stock_quantity = ?,
                vendor_name = ?,
                vendor_phone = ?
            WHERE item_id = ? AND is_active = TRUE
            """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, itemName.trim());
            ps.setString(2, category.trim());
            ps.setDouble(3, price);
            ps.setInt(4, stockQuantity);
            ps.setString(5, vendorName);
            ps.setString(6, vendorPhone);
            ps.setInt(7, itemId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Unable to update menu item.");
            e.printStackTrace();
        }

        return false;
    }


    // 4. Deactivate a menu item (soft delete)
    public static boolean deactivateMenuItem(int itemId) {

        if (itemId <= 0) {
            return false;
        }

        String sql = """
            UPDATE menu_items
            SET is_active = FALSE
            WHERE item_id = ? AND is_active = TRUE
            """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, itemId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Unable to deactivate menu item.");
            e.printStackTrace();
        }

        return false;
    }


    // 5. Update stock quantity

public static boolean updateStock(int itemId, int newQuantity) {

    if (itemId <= 0 || newQuantity < 0) {
        return false;
    }

    String sql = """
        SELECT item_name, stock_quantity, vendor_phone
        FROM menu_items
        WHERE item_id = ? AND is_active = TRUE
        """;

    try (Connection con = DB.getConnection()) {

        if (con == null) {
            return false;
        }

        String itemName;
        String vendorPhone;
        int oldQuantity;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return false;
                }

                itemName = rs.getString("item_name");
                oldQuantity = rs.getInt("stock_quantity");
                vendorPhone = rs.getString("vendor_phone");
            }
        }

        String updateSql = """
            UPDATE menu_items
            SET stock_quantity = ?
            WHERE item_id = ? AND is_active = TRUE
            """;

        try (PreparedStatement ps = con.prepareStatement(updateSql)) {

            ps.setInt(1, newQuantity);
            ps.setInt(2, itemId);

            if (ps.executeUpdate() != 1) {
                return false;
            }
        }

        // Trigger notification only when stock crosses the threshold.
        if (oldQuantity > 5 && newQuantity <= 5
                && vendorPhone != null && !vendorPhone.isBlank()) {

            String message =
                    "DineFlow Alert: " + itemName
                    + " is low on stock. Current quantity: "
                    + newQuantity
                    + ". Please arrange new stock.";

            boolean saved = SmsNotificationService.saveNotification(
                    itemId,
                    vendorPhone,
                    message,
                    "PENDING"
            );

            if (saved) {
                SmsNotificationService.processPendingNotifications();
            }
        }

        return true;

    } catch (SQLException e) {

        System.err.println("Unable to update stock.");
        e.printStackTrace();
        return false;
    }
}
}
