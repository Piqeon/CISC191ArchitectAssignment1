package edu.sdccd.cisc191.template;

import java.io.Serializable;

public class Item implements Serializable{

    private String itemName;
    private int quantity;
    private double totalPrice;

    public Item(String itemName, int quantity, double totalPrice) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    // No-argument constructor required by JPA
    public Item() {
        // Optionally, initialize fields to default values
        this.itemName = "";
        this.quantity = 0;
        this.totalPrice = 0.0;
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
    @Override
    public String toString() {
        return "Item{" +
                "itemName='" + itemName + '\'' +
                ", quantity=" + quantity +
                ", totalPrice=" + totalPrice +
                '}';
    }
}