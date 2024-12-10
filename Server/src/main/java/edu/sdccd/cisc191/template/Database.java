package edu.sdccd.cisc191.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Database {

    private PastOrderRepository repository;
    @Autowired
    public Database(PastOrderRepository repository) {
        this.repository = repository;
    }
    // Method to save the order to the database
    public void saveOrder(Order order) {

        try {
            if(repository == null) {
                System.out.println("ERROR: Repository is null, cannot save.");
            }
            repository.save(order); // Save the order to the repository
            System.out.println("Order saved successfully.");
        } catch (Exception e) {
            System.out.println("ERROR: Failed to save order.");
            e.printStackTrace();
        }
    }
    public List<Order> findOrdersByCustomerName(String customerName) {
        try {
            List<Order> orders = repository.findByCustomerName(customerName);
            if (orders.isEmpty()) {
                System.out.println("No orders found for customer: " + customerName);
            } else {
                System.out.println("Found " + orders.size() + " order(s) for customer: " + customerName);
            }
            return orders;
        } catch (Exception e) {
            System.out.println("ERROR: Failed to search orders by customer name.");
            e.printStackTrace();
            return null;
        }
    }
}