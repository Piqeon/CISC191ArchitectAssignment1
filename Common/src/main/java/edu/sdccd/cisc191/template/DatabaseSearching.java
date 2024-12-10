package edu.sdccd.cisc191.template;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class DatabaseSearching {

    private BufferedReader in;
    private PrintWriter out;

    public DatabaseSearching(BufferedReader in, PrintWriter out) {
        this.in = in;
        this.out = out;
    }

    // Method to search for orders by customer name
    public String searchOrderByCustomerName(String customerName) throws IOException {
        // Send the search request to the server
        out.println("SEARCH_NAME");
        out.println(customerName);

        // Get the response from the server
        String response = readServerResponse();
        System.out.println("Server Response: " + response);

        // If orders exist, format them into a readable string
        if (response.startsWith("No orders found.")) {
            return "No orders found.";
        } else {
            return formatOrders(response);  // Format the orders if available
        }
    }

    public String readServerResponse() {
        StringBuilder response = new StringBuilder();
        try {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
                // Stop reading when the server sends a response (assuming no more data follows)
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR: Could not read response from server.";
        }
        return response.toString().trim();  // Return the complete response
    }

    // Formats the raw response to a readable string
    private String formatOrders(String rawOrders) {
        StringBuilder formatted = new StringBuilder();

        // Split the raw orders by line
        String[] orders = rawOrders.split("\n");

        for (String orderData : orders) {
            String[] fields = orderData.split(",", 4); // Split into 4 parts: id, customer, items (JSON), total

            if (fields.length < 4) {
                formatted.append("Invalid order format.\n");
                continue; // Skip malformed orders
            }

            String customerName = fields[1];  // Customer name
            String itemsJson = fields[2];     // JSON string of items
            double totalPrice = Double.parseDouble(fields[3]); // Total price

            formatted.append("Customer: ").append(customerName).append("\n");

            try {
                // Parse the JSON array containing items
                JSONArray itemsArray = new JSONArray(itemsJson);
                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject item = itemsArray.getJSONObject(j);
                    String itemName = item.getString("itemName");
                    int quantity = item.getInt("quantity");
                    double price = item.getDouble("totalPrice");

                    formatted.append("  Item: ").append(itemName).append("\n");
                    formatted.append("    Quantity: ").append(quantity).append("\n");
                    formatted.append("    Price: $").append(price).append("\n");
                }

                formatted.append("Total: $").append(totalPrice).append("\n");
            } catch (Exception e) {
                formatted.append("Error parsing items JSON.\n");
            }

            formatted.append("\n"); // Add space between orders
        }

        return formatted.toString();
    }
}
