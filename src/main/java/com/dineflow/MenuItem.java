
package com.dineflow;

public class MenuItem {

    // Food item details
    private int itemId;
    private String itemName;
    private String category;
    private double price;
    private int stockQuantity;
    private String vendorName;
    private String vendorPhone;
    private boolean active;

    // Constructor
    public MenuItem(int itemId, String itemName,
                    String category, double price,
                    int stockQuantity, String vendorName,
                    String vendorPhone, boolean active) {

        this.itemId = itemId;
        this.itemName = itemName;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.vendorName = vendorName;
        this.vendorPhone = vendorPhone;
        this.active = active;
    }

    // Getters
    public int getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getVendorPhone() {
        return vendorPhone;
    }

    public boolean isActive() {
        return active;
    }

    // Setters
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public void setVendorPhone(String vendorPhone) {
        this.vendorPhone = vendorPhone;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Check inventory status
    public String getStockStatus() {

        if (stockQuantity == 0) {
            return "OUT OF STOCK";
        } else if (stockQuantity <= 5) {
            return "LOW STOCK";
        } else {
            return "IN STOCK";
        }
    }

}
