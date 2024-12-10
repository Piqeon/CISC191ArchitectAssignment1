package edu.sdccd.cisc191.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@SpringBootApplication
public class Server implements CommandLineRunner {

    private ServerSocket serverSocket;
    private static final int PORT = 8081;
    private static ExecutorService pool = Executors.newFixedThreadPool(10);

    private final Database database;

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    @Autowired
    public Server(Database database) {
        this.database = database;
    }

    @Override
    public void run(String... args) throws Exception {
        startServer();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());
                    ClientHandler clientHandler = new ClientHandler(socket);
                    pool.execute(clientHandler);
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server starting error: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    public void stopServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server stopped.");
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }

    public class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String request;
                while ((request = in.readLine()) != null) {
                    if (request.startsWith("GET_MENU")) {
                        sendMenu();
                    } else if (request.startsWith("ADMIN_MODE")) {
                        String command = request.substring("ADMIN_MODE".length()).trim();
                        handleAdminMode(command);
                    } else if (request.startsWith("PLACE_ORDER")) {
                        handleOrder();
                    } else if (request.startsWith("SEARCH_NAME")) {
                        orderNameSearching();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMenu() {
            out.println("MENU_START");
            for (Map.Entry<String, Double> item : MenuUtils.getAllItems().entrySet()) {
                out.println(item.getKey() + ":" + item.getValue());
            }
            out.println("MENU_END");
        }

        public void orderNameSearching() {
            try {
                String name = in.readLine();
                if (name == null || name.trim().isEmpty()) {
                    out.println("ERROR: Customer name cannot be empty.");
                    return;
                }

                List<Order> orders = database.findOrdersByCustomerName(name);

                if (orders == null || orders.isEmpty()) {
                    out.println("No orders found.");
                } else {
                    // Send each order's details directly
                    for (Order order : orders) {
                        // Construct order data string in a simple format
                        String orderData = order.getId() + "," + order.getCustomerName() + ","
                                + order.getItems() + "," + order.getTotalPrice();
                        out.println(orderData);  // Send the order details to the client
                    }
                }
            } catch (IOException e) {
                out.println("ERROR: Failed to read input or search orders.");
                e.printStackTrace();
            }
        }


        private void handleOrder() {
            try {
                String orderDetails = in.readLine();
                if (orderDetails != null) {
                    Order newOrder = parseOrderDetails(orderDetails);
                    if (newOrder == null) {
                        out.println("ERROR: Failed to parse order details.");
                        return;
                    }
                    database.saveOrder(newOrder);
                    out.println("ORDER_PLACED: " + newOrder.getId());
                }
            } catch (IOException e) {
                out.println("ERROR: Unable to place order.");
            }
        }

        private Order parseOrderDetails(String orderDetails) {
            String[] parts = orderDetails.split(",", 2);
            String customerName = parts[0];
            String itemsString = parts[1];

            List<Item> items = parseItems(itemsString);

            return new Order(items, customerName);
        }

        private List<Item> parseItems(String itemsString) {
            List<Item> items = new ArrayList<>();
            String[] itemDetails = itemsString.split(",");
            for (String itemDetail : itemDetails) {
                String[] itemInfo = itemDetail.split(":");
                String itemName = itemInfo[0];
                int quantity = Integer.parseInt(itemInfo[1]);
                double price = Double.parseDouble(itemInfo[2]);
                items.add(new Item(itemName, quantity, price));
            }
            return items;
        }

        public void handleAdminMode(String command) {
            String[] parts = command.split(" ");
            String action = parts[0];
            String itemName = null;
            String priceString = null;

            if (parts.length >= 3) {
                if (parts[0].equals("REMOVE")) {
                    itemName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                } else {
                    itemName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1));
                    priceString = parts[parts.length - 1];
                }
            } else {
                itemName = parts[parts.length - 1];
            }

            switch (action) {
                case "ADD":
                    if (itemName != null && priceString != null) {
                        try {
                            double itemPrice = Double.parseDouble(priceString);
                            MenuUtils.addItem(itemName, itemPrice);
                            out.println("ITEM_ADDED: " + itemName);
                        } catch (NumberFormatException e) {
                            out.println("ERROR: Invalid price format.");
                        }
                    } else {
                        out.println("ERROR: Invalid ADD command format.");
                    }
                    break;

                case "REMOVE":
                    if (itemName != null) {
                        if (MenuUtils.itemExists(itemName)) {
                            MenuUtils.removeItem(itemName);
                            out.println("ITEM_REMOVED: " + itemName);
                        } else {
                            out.println("ERROR: Item '" + itemName + "' not found.");
                        }
                    } else {
                        out.println("ERROR: Invalid REMOVE command format.");
                    }
                    break;

                case "UPDATE":
                    if (itemName != null && priceString != null) {
                        try {
                            double newPrice = Double.parseDouble(priceString);
                            if (MenuUtils.itemExists(itemName)) {
                                MenuUtils.addItem(itemName, newPrice);
                                out.println("ITEM_UPDATED: " + itemName);
                            } else {
                                out.println("ERROR: Item '" + itemName + "' not found.");
                            }
                        } catch (NumberFormatException e) {
                            out.println("ERROR: Invalid price format.");
                        }
                    } else {
                        out.println("ERROR: Invalid UPDATE command format.");
                    }
                    break;

                default:
                    out.println("ERROR: Unknown command.");
                    break;
            }
        }
    }
}
