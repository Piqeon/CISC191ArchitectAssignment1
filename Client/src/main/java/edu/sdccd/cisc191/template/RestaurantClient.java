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
    private static final int PORT = 8081;
    private DialogPromt dialog;

    private ObservableList<String> menuItems = FXCollections.observableArrayList();
    private List<Item> items = new LinkedList<>(); // List to store orders
    private Button adminButton = new Button("Admin Mode");
    private boolean isAdminMode = false;
    private Button refreshButton = new Button("Refresh Menu"); // New Refresh Button

    private Socket socket;
    private BufferedReader in;
    private DatabaseSearching databaseSearching;
    private PrintWriter out;
    private AdminController adminController;
    private ListView<String> orderListView = new ListView<>(FXCollections.observableArrayList());


    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize connection to the server
        initializeConnection();
        adminController = new AdminController(out, this);
        databaseSearching = new DatabaseSearching(in, out);

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
        Button addToOrder = new Button("Add To Order");
        addToOrder.setOnAction(e -> {
            String selectedItem = menuListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !isAdminMode) {
                addToOrder(selectedItem.split(" - ")[0]); // Extract item name
            } else if (isAdminMode) {
                dialog.showAlert("Admin Mode", "You cannot place orders in Admin Mode.");
            }
        });

        Button removeItemButton = new Button("Remove Item");
        removeItemButton.setOnAction(e -> {
            String selectedItem = orderListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !isAdminMode) {
                items.removeIf(item -> item.getItemName().equals(selectedItem.split(" - ")[0])); // Remove item based on name
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

        Button placeOrderButton = new Button("Place Order");
        placeOrderButton.setOnAction(e -> {
            if (!isAdminMode) {
                sendOrderToServer(); // Call the method to send the entire order to the server
            } else {
                dialog.showAlert("Admin Mode", "You cannot place orders in Admin Mode.");
            }
        });
        Button SearchButton = new Button("Search");
        SearchButton.setOnAction(e -> {
            if (!isAdminMode) {
                searchPastOrder(); // Call the method to send the entire order to the server
            } else {
                dialog.showAlert("Admin Mode", "You cannot place orders in Admin Mode.");
            }
        });

        // Layout for buttons
        HBox buttonBox = new HBox(10, addToOrder, getTotalButton, refreshButton, removeItemButton, placeOrderButton, SearchButton);
        buttonBox.setPadding(new Insets(10, 0, 10, 0));

        // Main layout
        VBox mainLayout = new VBox(10, titleLabel, menuListView, buttonBox, new Label("Order Total"), orderListView, adminButton);
        mainLayout.setPadding(new Insets(20));

        // Setup the scene and stage
        Scene scene = new Scene(mainLayout, 650,400);
        primaryStage.setTitle("Restaurant Menu - RestaurantClient");
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

    private void addToOrder(String itemName) {
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
        for (Item item : items) {
            if (item.getItemName().equals(name)) {
                // If it exists, increase the quantity and update the item
                item.setQuantity(item.getQuantity() + 1);
                double totalPrice = item.getQuantity() * price;
                item.setPrice(totalPrice);
                out.println("ADD_TO_ORDER " + name + " (updated quantity)");
                updateOrderListView(); // Update the order list view after modifying an order
                return; // Exit the method early since we're updating an existing order
            }
        }

        // Create an Order instance with a quantity of 1
        Item item = new Item(name, 1, price); // Use name for the item name
        items.add(item); // Add the item to the list
        out.println("ADD_TO_ORDER " + itemName);
        updateOrderListView(); // Update the order list view after placing an order
    }
    private void updateOrderListView() {
        // Clear the current items
        orderListView.getItems().clear();
        // Add each item to the item list view
        for (Item item : items) {
            orderListView.getItems().add(item.getItemName() + " - Quantity: " + item.getQuantity() + " - Price: $" + item.getTotalPrice());
        }
    }

    private void getOrderTotal() throws IOException {
        double total = items.stream().mapToDouble(Item::getTotalPrice).sum(); // Use streams to calculate total
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

    private void sendOrderToServer() {
        if (items.isEmpty()) {
            dialog.showAlert("Empty Order", "Please add items to your order before placing it.");
            return;
        }
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Customer Name");
        nameDialog.setHeaderText("Enter your name:");
        nameDialog.setContentText("Name:");

        Optional<String> result = nameDialog.showAndWait();
        String customerName = result.orElse("Unknown");

        // Convert the list of items to a string format
        StringBuilder orderDetails = new StringBuilder(customerName);
        for (Item item : items) {
            orderDetails.append(",").append(item.getItemName())
                    .append(":").append(item.getQuantity())
                    .append(":").append(item.getTotalPrice());
        }


        // Send the PLACE_ORDER command
        out.println("PLACE_ORDER");
        // Send the order details as a string
        out.println(orderDetails.toString());

        // Flush the output stream to ensure the data is sent
        out.flush();

        String response = readServerResponse();
        if (response.startsWith("ORDER_PLACED")){
            dialog.showAlert("Sucess! ","Order Placed with server.");
            items.clear();
            updateOrderListView();
        }
        else {
            dialog.showAlert("ERROR", "Order failed to place! ");
        }

    }

    private void searchPastOrder() {
        System.out.println("Searching for order...");
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Customer Lookup");
        nameDialog.setHeaderText("Enter name:");
        nameDialog.setContentText("Name:");

        Optional<String> result = nameDialog.showAndWait();

        if (result.isPresent()) {
            String name = result.get();
            try {
                // Use DatabaseSearching to search for orders by customer name
                String response = databaseSearching.searchOrderByCustomerName(name);
                System.out.println(response);

                if (response.startsWith("NOT_FOUND")) {
                    dialog.showAlert("Search Result", "No orders found for: " + name);
                } else if (response.startsWith("FOUND")) {
                    dialog.showAlert("Orders Found", response);
                } else {
                    dialog.showAlert("Error", "Error occurred while searching for orders.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                dialog.showAlert("Error", "Error occurred while trying to search database. ");
            }
        } else {
            dialog.showAlert("Input Error", "Please enter a name to search.");
        }
    }


}