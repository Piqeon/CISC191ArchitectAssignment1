package edu.sdccd.cisc191.template;

import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.*;


public class RestaurantClient extends Application {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 8080;
    private DialogPromt dialog;

    private ObservableList<String> menuItems = FXCollections.observableArrayList();
    private List<Order> orders = new ArrayList<>(); // List to store orders
    private Button adminButton = new Button("Admin Mode");
    private boolean isAdminMode = false;
    private Button refreshButton = new Button("Refresh Menu"); // New Refresh Button

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private AdminController adminController;
    private ListView<String> orderListView = new ListView<>(FXCollections.observableArrayList());


    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize connection to the server
        initializeConnection();
        adminController = new AdminController(out, this);

        // Fetch the menu items from the server
        fetchMenuItems();

        adminButton.setOnAction(e -> adminMode());
        refreshButton.setOnAction(e -> refreshMenuItems()); // Set action for Refresh button

        // Create UI components
        Label titleLabel = new Label("Restaurant Menu");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // List to display menu items
        ListView<String> menuListView = new ListView<>(menuItems);
        menuListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        menuListView.setPrefHeight(150);

        // Button to place an order
        Button placeOrderButton = new Button("Place Order");
        placeOrderButton.setOnAction(e -> {
            String selectedItem = menuListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !isAdminMode) {
                placeOrder(selectedItem.split(" - ")[0]); // Extract item name
            } else if (isAdminMode) {
                dialog.showAlert("Admin Mode", "You cannot place orders in Admin Mode.");
            }
        });

        Button removeItemButton = new Button("Remove Item");
        removeItemButton.setOnAction(e -> {
            String selectedItem = orderListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !isAdminMode) {
                orders.removeIf(order -> order.getItemName().equals(selectedItem.split(" - ")[0])); // Remove item based on name
                orderListView.getItems().remove(selectedItem);
                updateOrderListView(); // Update the display after removal

            } else if (isAdminMode) {
                dialog.showAlert("Admin Mode", "You cannot edit orders in Admin Mode.");
            }
        });

        // List to display placed orders
        orderListView.setPrefHeight(100);

        // Button to get the total cost of orders
        Button getTotalButton = new Button("Get Total");
        getTotalButton.setOnAction(e -> {
            try {
                getOrderTotal();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Layout for buttons
        HBox buttonBox = new HBox(10, placeOrderButton, getTotalButton, refreshButton, removeItemButton);
        buttonBox.setPadding(new Insets(10, 0, 10, 0));

        // Main layout
        VBox mainLayout = new VBox(10, titleLabel, menuListView, buttonBox, new Label("Placed Orders"), orderListView, adminButton);
        mainLayout.setPadding(new Insets(20));

        // Setup the scene and stage
        Scene scene = new Scene(mainLayout, 500, 400);
        primaryStage.setTitle("Restaurant Menu - Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Add a shutdown hook to close the socket gracefully
        primaryStage.setOnCloseRequest(event -> closeConnection());
    }
    public static void main(String[] args) {
        launch(args);
    }

    /**
     *
     * Server connection methods for opening and closing the connection
     */

    private void initializeConnection() throws IOException {
        System.out.println("Connecting to server...");
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Could not connect to server.");
        }
    }
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Connection closed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String readServerResponse() {
        try {
            // Read a line from the server (assuming buffered input)
            return in.readLine();  // 'in' is a BufferedReader connected to the server's input stream
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR: Could not read response from server.";
        }
    }


    /**
     *
     * Fetching and refreshing menu
     */
    public void fetchMenuItems() throws IOException {
        menuItems.clear();  // Clear current items to avoid duplication
        out.println("GET_MENU");  // Send GET_MENU request to server

        String response;
        boolean menuStarted = false;

        while ((response = in.readLine()) != null) {
            if (response.equals("MENU_START")) {
                menuStarted = true;
                continue;  // Skip "MENU_START" line
            }
            if (response.equals("MENU_END")) {
                break;  // Stop reading once "MENU_END" is reached
            }
            if (menuStarted == true) {
                String[] parts = response.split(":");
                if (parts.length == 2) {
                    menuItems.add(parts[0] + ":" + parts[1]);
                }
            }
        }
    }

    public void refreshMenuItems() {
        try {
            fetchMenuItems();  // Call fetchMenuItems to get the updated menu from server
        } catch (IOException ex) {
            ex.printStackTrace();
            dialog.showAlert("Error", "Unable to refresh menu. Please try again.");
        }
    }

    /**
     *
     * Placing orders, updating order view, and getting total.
     */

    private void placeOrder(String itemName) {
        // Split the input to find the item in the menu
        String[] parts = itemName.split(":");

        // Check if the menu item format is valid
        if (parts.length < 2) {
            System.err.println("ERROR: Invalid item format. Expected format: itemName:price");
            out.println("ERROR: Invalid item format. Expected format: itemName:price");
            return; // Exit the method early
        }

        // Retrieve the item name and price
        String name = parts[0].trim(); // The name is before the colon
        double price;

        try {
            price = Double.parseDouble(parts[1].trim()); // The price is after the colon
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Invalid price format.");
            out.println("ERROR: Invalid price format.");
            return; // Exit the method early
        }
        for (Order order : orders) {
            if (order.getItemName().equals(name)) {
                // If it exists, increase the quantity and update the order
                order.setQuantity(order.getQuantity() + 1);
                double totalPrice = order.getQuantity() * price;
                order.setPrice(totalPrice);
                out.println("PLACE_ORDER " + name + " (updated quantity)");
                updateOrderListView(); // Update the order list view after modifying an order
                return; // Exit the method early since we're updating an existing order
            }
        }

        // Create an Order instance with a quantity of 1
        Order order = new Order(name, 1, price); // Use name for the item name
        orders.add(order); // Add the order to the list
        out.println("PLACE_ORDER " + itemName);
        updateOrderListView(); // Update the order list view after placing an order
    }
    private void updateOrderListView() {
        // Clear the current items
        orderListView.getItems().clear();
        // Add each order to the order list view
        for (Order order : orders) {
            orderListView.getItems().add(order.getItemName() + " - Quantity: " + order.getQuantity() + " - Price: $" + order.getTotalPrice());
        }
    }

    private void getOrderTotal() throws IOException {
        double total = orders.stream().mapToDouble(Order::getTotalPrice).sum(); // Use streams to calculate total
        String formattedTotal = String.format("%.2f", total);
        dialog.showAlert("Total Order Cost", "Total Order Cost: $" + formattedTotal);
    }

    /**
     * Admin mode handling and password validation.
     */
    private void adminMode() {
        // Create a dialog to prompt for password
        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("Admin Mode");
        passwordDialog.setHeaderText("Enter Admin Password");
        passwordDialog.setContentText("Password:");

        // Show the dialog and wait for user response
        Optional<String> result = passwordDialog.showAndWait();
        result.ifPresent(password -> {
            if (validatePassword(password)) {
                // Proceed to admin features
                adminController.showAdminFeatures();
            } else {
                dialog.showAlert("Access Denied", "Incorrect password. Please try again.");
            }
        });
    }

    private boolean validatePassword(String password) {
        // Check if the entered password is correct
        return "admin".equals(password); // Hardcoded password for demonstration
    }

}