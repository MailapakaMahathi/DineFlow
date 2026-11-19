
package com.dineflow;

import java.util.List;
import java.util.Map;

public class DashboardBridge {

    public String getDashboardStatsJson() {

        Map<String, Object> stats =
                DashboardService.getDashboardStats();

        return "{"
                + "\"revenue\":" + stats.get("revenue") + ","
                + "\"orders\":" + stats.get("orders") + ","
                + "\"menuItems\":" + stats.get("menuItems") + ","
                + "\"lowStock\":" + stats.get("lowStock") + ","
                + "\"inStock\":" + stats.get("inStock") + ","
                + "\"outOfStock\":" + stats.get("outOfStock")
                + "}";
    }

    public String getRecentOrdersJson() {
        return DashboardService.getRecentOrdersJson();
    }

    public String getMenuItemsJson() {

        List<MenuItem> items = DashboardService.getAllMenuItems();

        System.out.println("FOOD ORDERING: Items retrieved = " + items.size());

        return convertMenuItemsToJson(items);
    }

    public String getAllMenuItemsForManagementJson() {

        System.out.println("MENU MANAGEMENT: Method called.");

        List<MenuItem> items =
                MenuManagementService.getAllMenuItems();

        System.out.println(
                "MENU MANAGEMENT: Items retrieved = " + items.size()
        );

        String json = convertMenuItemsToJson(items);

        System.out.println("MENU MANAGEMENT JSON: " + json);

        return json;
    }

    private String convertMenuItemsToJson(List<MenuItem> items) {

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < items.size(); i++) {

            MenuItem item = items.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                .append("\"itemId\":").append(item.getItemId()).append(",")
                .append("\"itemName\":\"").append(escapeJson(item.getItemName())).append("\",")
                .append("\"category\":\"").append(escapeJson(item.getCategory())).append("\",")
                .append("\"price\":").append(item.getPrice()).append(",")
                .append("\"stockQuantity\":").append(item.getStockQuantity()).append(",")
                .append("\"vendorName\":\"").append(escapeJson(item.getVendorName())).append("\",")
                .append("\"vendorPhone\":\"").append(escapeJson(item.getVendorPhone())).append("\",")
                .append("\"active\":").append(item.isActive())
                .append("}");
        }

        json.append("]");

        return json.toString();
    }

    public boolean addMenuItem(
            String itemName,
            String category,
            double price,
            int stockQuantity,
            String vendorName,
            String vendorPhone) {

        return MenuManagementService.addMenuItem(
                itemName, category, price,
                stockQuantity, vendorName, vendorPhone
        );
    }

    public boolean updateMenuItem(
            int itemId,
            String itemName,
            String category,
            double price,
            int stockQuantity,
            String vendorName,
            String vendorPhone) {

        return MenuManagementService.updateMenuItem(
                itemId, itemName, category, price,
                stockQuantity, vendorName, vendorPhone
        );
    }

    public boolean deactivateMenuItem(int itemId) {
        return MenuManagementService.deactivateMenuItem(itemId);
    }

    public boolean updateStock(int itemId, int newQuantity) {
        return MenuManagementService.updateStock(itemId, newQuantity);
    }

    public String placeOrder(
            String customerName,
            String cartJson,
            String paymentMethod,
            double discountAmount) {

        try {
            return OrderService.placeOrder(
                    customerName, cartJson,
                    paymentMethod, discountAmount
            );
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"success\":false,\"message\":\"Unable to process order.\"}";
        }
    }

    private String escapeJson(String value) {
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
    public String testMenuManagementBridge() {

        System.out.println("MENU MANAGEMENT BRIDGE TEST SUCCESSFUL!");

        return "Menu Management bridge is working!";
    }
    public String testInventoryBridge() {
        System.out.println("========== INVENTORY BRIDGE TEST ==========");

        List<MenuItem> items = DashboardService.getAllMenuItems();

        System.out.println("Inventory item count: " + items.size());

        String json = convertMenuItemsToJson(items);

        System.out.println("Inventory JSON: " + json);

        return json;
    }

    // ================= BILLING MODULE =================

    public String getBillingOrdersJson() {
        return BillingService.getBillingOrdersJson();
    }

    public String getBillingStatsJson() {
        return BillingService.getBillingStatsJson();
    }

    public String generateInvoice(int orderId) {
        return BillingService.generateInvoice(orderId);
    }
    public String getVendorNotificationsJson() {
        return SmsNotificationService.getNotificationsJson();
    }
    public String getSalesReportJson(
            String period, String startDate, String endDate) {

        return SalesReportService.getSalesReportJson(
                period, startDate, endDate);
    }

}
