package edu.sdccd.cisc191.template;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class MenuUtilsTest {

    private static final String TEST_MENU_FILE = "test_menu.csv";

    @BeforeEach
    void setUp() throws Exception {
        MenuUtils.setMenuFilePath(TEST_MENU_FILE); // Set the file path for tests
        try (PrintWriter writer = new PrintWriter(TEST_MENU_FILE)) {
            writer.println("Pizza:12.99");
            writer.println("Burger:8.99");
        }
    }

    @AfterEach
    void tearDown() {
        new File(TEST_MENU_FILE).delete(); // Cleanup the test file after each test
    }

    @Test
    void testUpdateMenu() {
        List<String> menuData = Arrays.asList("Pizza:12.99", "Burger:8.99");

        MenuUtils.updateMenu(menuData);

        // Verify that the menu has been updated correctly
        assertTrue(MenuUtils.getAllItems().containsKey("Pizza"));
        assertEquals(12.99, MenuUtils.getAllItems().get("Pizza"));
        assertTrue(MenuUtils.getAllItems().containsKey("Burger"));
        assertEquals(8.99, MenuUtils.getAllItems().get("Burger"));
    }
    @Test
    void testLoadMenu() {
        // Call loadMenu method to populate the menuItems map
        MenuUtils.loadMenu();

        // Verify that the menuItems map contains the expected entries
        assertTrue(MenuUtils.getAllItems().containsKey("Pizza"));
        assertEquals(12.99, MenuUtils.getAllItems().get("Pizza"));
        assertTrue(MenuUtils.getAllItems().containsKey("Burger"));
        assertEquals(8.99, MenuUtils.getAllItems().get("Burger"));

        // Check that the size of the map is correct
        assertEquals(2, MenuUtils.getAllItems().size());
    }
}
