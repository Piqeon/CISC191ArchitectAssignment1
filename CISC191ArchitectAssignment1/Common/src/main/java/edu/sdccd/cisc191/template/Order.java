package edu.sdccd.cisc191.template;

public class Order {
    private String itemName;
    private int quantity;
    private double totalPrice;

    public Order(String itemName, int quantity, double totalPrice) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    // Getters and setters...
    public String getItemName() {
        return itemName;
    }
    public int getQuantity() {
        return quantity;
    }
    public double getTotalPrice() {
        return totalPrice;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public void setPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}