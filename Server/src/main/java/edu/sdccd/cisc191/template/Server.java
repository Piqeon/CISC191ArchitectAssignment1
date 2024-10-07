package edu.sdccd.cisc191.template;

import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.concurrent.*;

public class Server {
    private ServerSocket serverSocket;
    private static final int PORT = 8080;
    private static ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }

    private void startServer() {
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

    private static class ClientHandler implements Runnable {
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

        private void sendMenu() {
            out.println("MENU_START");
            for (Map.Entry<String, Double> item : MenuUtils.getAllItems().entrySet()) {
                out.println(item.getKey() + ":" + item.getValue());
            }
            out.println("MENU_END");
        }

        private void handleAdminMode(String command) {
            String[] parts = command.split(" ");
            String action = parts[0];

            switch (action) {
                case "ADD":
                    if (parts.length >= 3) {
                        String itemName = parts[1];
                        double itemPrice;
                        try {
                            itemPrice = Double.parseDouble(parts[2]);
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
                    if (parts.length == 2) {
                        String itemName = parts[1];
                        if (MenuUtils.itemExists(itemName)) {
                            MenuUtils.removeItem(itemName);
                            out.println("ITEM_REMOVED:" + itemName);
                        } else {
                            out.println("ERROR: Item '" + itemName + "' not found.");
                        }
                    } else {
                        out.println("ERROR: Invalid REMOVE command format.");
                    }
                    break;

                case "UPDATE":
                    if (parts.length == 3) {
                        String itemName = parts[1];
                        try {
                            double newPrice = Double.parseDouble(parts[2]);
                            if (MenuUtils.itemExists(itemName)) {
                                MenuUtils.addItem(itemName, newPrice); // Update item by adding it again
                                out.println("ITEM_UPDATED:" + itemName);
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