
package com.dineflow;

public class CartLine {

    // Food item associated with this cart entry
    private MenuItem menuItem;

    // Quantity selected by the customer
    private int quantity;

    // Constructor
    public CartLine(MenuItem menuItem, int quantity) {
        this.menuItem = menuItem;
        this.quantity = quantity;
    }

    // Get food item
    public MenuItem getMenuItem() {
        return menuItem;
    }

    // Get quantity
    public int getQuantity() {
        return quantity;
    }

    // Update quantity
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // Calculate total amount for this item
    public double getLineTotal() {
        return menuItem.getPrice() * quantity;
    }

    // Increase quantity
    public void increaseQuantity() {
        quantity++;
    }

    // Decrease quantity
    public void decreaseQuantity() {
        if (quantity > 1) {
            quantity--;
        }
    }

}
