package edu.sdccd.cisc191.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "items", columnDefinition = "TEXT")
    private String itemsJson; // JSON representation of the items

    @Column(name = "total_price")
    private double totalPrice;

    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Order() {
    }

    public Order(List<Item> items, String customerName) {
        this.customerName = customerName;
        setItems(items); // Serialize items to JSON
        calculateTotalPrice(items);
    }

    private void calculateTotalPrice(List<Item> items) {
        this.totalPrice = items.stream()
                .mapToDouble(Item::getTotalPrice)
                .sum();
    }

    public List<Item> getItems() {
        try {
            return objectMapper.readValue(itemsJson, objectMapper.getTypeFactory().constructCollectionType(List.class, Item.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize items JSON", e);
        }
    }

    public void setItems(List<Item> items) {
        try {
            this.itemsJson = objectMapper.writeValueAsString(items); // Serialize to JSON
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize items to JSON", e);
        }
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", customerName='" + customerName + '\'' +
                ", items=" + getItems() +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
