package edu.sdccd.cisc191.template;

import javafx.scene.control.*;
import javafx.util.Pair;
import javafx.event.*;
import java.io.PrintWriter;
import java.util.*;

public class AdminController {
    private PrintWriter out; // Print writer to send commands to Server
    private RestaurantClient client; // Client call to interact with client
    private String response;

    public AdminController(PrintWriter out, RestaurantClient client) {
        this.out = out;
        this.client = client;
    }

    public void showAdminFeatures() {
        // Stage for admin mode
        javafx.stage.Stage adminStage = new javafx.stage.Stage();
        adminStage.setTitle("Admin Mode - Restaurant Management");

        // Create buttons for Admin actions.
        Button addButton = createButton("Add Item", e -> showAddItemDialog()); // Add item button (showAddItemDialog method)
        Button removeButton = createButton("Remove Item", e -> showRemoveItemDialog()); // Remove item button (showRemoveItemDialog method)
        Button updateButton = createButton("Update Item", e -> showUpdateItemDialog()); // Update item button (showUpdateItemDialog method)
        Button refreshButton = createButton("Refresh Menu", e -> client.refreshMenuItems()); // Refresh button calls client method refreshMenuItems()

        // Set up actions for buttons
        removeButton.setOnAction(e -> showRemoveItemDialog()); // RemoveButton action (showRemoveItemDialog method)

        // Layout for buttons
        javafx.scene.layout.VBox adminLayout = new javafx.scene.layout.VBox(10, addButton, removeButton, updateButton, refreshButton);
        adminLayout.setPadding(new javafx.geometry.Insets(20));

        // Set up the scene and show the stage
        javafx.scene.Scene adminScene = new javafx.scene.Scene(adminLayout, 300, 200);
        adminStage.setScene(adminScene);
        adminStage.show();
    }

    private Button createButton(String text, EventHandler<ActionEvent> eventHandler) {
        Button button = new Button(text);
        button.setOnAction(eventHandler);
        return button;
    }

    private void showAddItemDialog() {
        Optional<Pair<String, String>> result = DialogPromt.DualInputDialog("Add Menu Item", "Enter new menu Item details", "Item Name", "Item Price", "(format item_name", "price");

        result.ifPresent(itemDetails -> {
            String itemName = itemDetails.getKey();

            // Check if the item already exists in the local menu using MenuUtils
            if (MenuUtils.itemExists(itemName)) {
                DialogPromt.showAlert("Error", "Item '" + itemName + "' already exists in the local menu.");
                return; // Exit early if the item already exists
            }

            try {
                // Parse the price string to double
                double itemPrice = Double.parseDouble(itemDetails.getValue());

                try {
                    // Send the ADD command to the server
                    out.println("ADMIN_MODE ADD " + itemName + " " + itemPrice);

                    // Listen for the response from the server
                    String response = client.readServerResponse();

                    // Update the UI based on the server response
                    if (response.startsWith("ITEM_ADDED")) {
                        DialogPromt.showAlert("Success", "Item added: " + itemName + " - $" + itemPrice);
                        refreshMenuFromServer();
                    } else if (response.startsWith("ERROR")) {
                        DialogPromt.showAlert("Error", response);
                    }
                } catch (Exception e) {
                    // Handle communication errors by showing an alert
                    DialogPromt.showAlert("Error", "Failed to communicate with server.");
                }
            } catch (NumberFormatException e) {
                // Handle invalid price input by showing an alert
                DialogPromt.showAlert("Invalid Input", "Price must be a number.");
            }
        });
    }

    private void showRemoveItemDialog() {
        DialogPromt.SingleInputDialog("Remove Menu Item", "Enter menu item name to remove", "Item name:")
                .ifPresent(itemName -> {
                    // Check if the item exists using MenuUtils before communicating with the server
                    try {
                        // Send the REMOVE command to the server
                        out.println("ADMIN_MODE REMOVE " + itemName);

                        // Listen for the response from the server (this might block)
                        response = client.readServerResponse();

                        // Once we get the response, update the UI on the JavaFX Application Thread
                        if (response.startsWith("ITEM_REMOVED")) {
                            DialogPromt.showAlert("Success", "Item removed: " + itemName);
                            refreshMenuFromServer();
                        } else if (response.startsWith("ERROR")) {
                            DialogPromt.showAlert("Error", response);
                        }
                    } catch (Exception e) {
                        // If there's an error, show an alert on the UI thread
                        DialogPromt.showAlert("Error", "Failed to communicate with server.");
                    }
                });
    }

    private void showUpdateItemDialog() {
        Optional<Pair<String, String>> result = DialogPromt.DualInputDialog("Update Menu Item", "Enter updated menu item details", "Item Name:", "New Price:", "(format item_name", "price");

        result.ifPresent(itemDetails -> {
            String itemName = itemDetails.getKey();
            String priceString = itemDetails.getValue();

            try {
                // Convert price string to double
                double itemPrice = Double.parseDouble(priceString);

                // Send the update command to the server
                out.println("ADMIN_MODE UPDATE " + itemName + " " + itemPrice);
                response = client.readServerResponse();

                if (response.startsWith("ITEM_UPDATED")) {
                    DialogPromt.showAlert("Success", "Item updated: " + itemName);
                    refreshMenuFromServer();
                } else if (response.startsWith("ERROR")) {
                    DialogPromt.showAlert("Error", response);
                }
            } catch (NumberFormatException e) {
                // Handle invalid price input
                DialogPromt.showAlert("Invalid Input", "Price must be a number.");
            }
        });
    }

    private void refreshMenuFromServer() {
        // Send the request to get the updated menu from the server
        out.println("GET_MENU");

        // Read the menu from the server response and update MenuUtils
        String response;
        List<String> menuItems = new ArrayList<>();

        while (!(response = client.readServerResponse()).equals("MENU_END")) {
            if (!response.equals("MENU_START")) {
                menuItems.add(response);
            }
        }

        // Update MenuUtils with the fetched menu
        MenuUtils.updateMenu(menuItems);
    }
}
