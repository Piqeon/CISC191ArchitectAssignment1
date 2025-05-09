package edu.sdccd.cisc191.template;

import java.io.*;
import java.util.*;

public class MenuUtils {
    private static final String MENU_FILE = "menu.csv";
    private static String menuFilePath = MENU_FILE;
    private static Map<String, Double> menuItems = new HashMap<>(); //Creates a hashmap called menuItems, each entry has a String (name) and a Double (price)

    // Static initializer block to load menu items when the class is first accessed
    static {
        loadMenu();
    }

    public static void setMenuFilePath(String filePath) {
        menuFilePath = filePath;
    }
    /**
     * Reads the menu items from the 'menu.csv' file and stores them in the menuItems map.
     */
    public static void loadMenu() {    //Reads teh menu items from the file using a BufferedReader.
        menuItems.clear(); // Clear the existing map before loading
        try (BufferedReader reader = new BufferedReader(new FileReader(menuFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {    //While loop sets the current readLine() to the String line and then splits the line with ":"
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    menuItems.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));    //Sets the now split Line String to be a new entry to the hashmap
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading menu file: " + e.getMessage());
        }
    }
    public static void updateMenu(List<String> menuData) {
        menuItems.clear();
        for (String item : menuData) {
            String[] parts = item.split(":");
            if (parts.length == 2) {
                menuItems.put(parts[0], Double.parseDouble(parts[1]));
            }
        }
    }


    public static boolean itemExists(String itemName) { //Checks if the current item exists in the menuItems map.
        return menuItems.containsKey(itemName);
    }


    public static void addItem(String itemName, double itemPrice) { //Adds a new item using itemName and itemPrice, calls saveMenu method.
        menuItems.put(itemName, itemPrice);
        saveMenu();
    }

    public static void removeItem(String itemName) {    //Removes item from itemName String and then calls the saveMenu() method.
        menuItems.remove(itemName);
        saveMenu();
    }

    private static void saveMenu() {    // saves the menuItems map to the MENU_FILE "menu.csv"
        try (PrintWriter writer = new PrintWriter(new FileWriter(menuFilePath))) {
            for (Map.Entry<String, Double> entry : menuItems.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("Error writing to menu file: " + e.getMessage());
        }
    }

    public static Map<String, Double> getAllItems() {   //Returns the menuItems map
        return menuItems;
    }
}