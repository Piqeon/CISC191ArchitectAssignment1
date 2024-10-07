package edu.sdccd.cisc191.template;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.util.Pair;

import java.util.Optional;

public class DialogPromt {

    //Show a dialog with two input fields and return a Pair of results
    public static Optional<Pair<String, String>> DualInputDialog(String title, String headerText, String label1, String label2, String prompt1, String prompt2) {
        /**
            title = Title for dialog box
            headerText = Any desired header text for box
            label1 = Label for next to text input window 1
            label2 = Label next to text input window 2
            prompt1 = Prompt for window 1
            prompt2 = Prompt for window 2
        */

        //Creates a Dialog to return two strings called doubleDialog
        Dialog<Pair<String, String>> doubleDialog = new Dialog<>();
        doubleDialog.setTitle(title);   //Title of this Dialog
        doubleDialog.setHeaderText(headerText); //Dialog header text (if needed)

        //Create two TextFields for input
        TextField textField1 = new TextField();
        textField1.setPromptText(prompt1);
        TextField textField2 = new TextField();
        textField2.setPromptText(prompt2);

        //GridPane for format of the two textFields.
        GridPane gridPane = createDialogLayout(new Label(label1), textField1, new Label(label2), textField2);
        doubleDialog.getDialogPane().setContent(gridPane);

        // Configure buttons
        ButtonType submitButton = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);   //Create a submit button using the default OK_DONE for buttons
        doubleDialog.getDialogPane().getButtonTypes().addAll(submitButton, ButtonType.CANCEL);  //Predefined Cancel button
        Node submitButtonReference = doubleDialog.getDialogPane().lookupButton(submitButton);            //Reference to the button submit button called submitButton
        configureButtonState(submitButtonReference, textField1, textField2);                       //Creates an object of configureButtonState and passes the reference submitButtonReference and textFields to it

        // Set result converter
        doubleDialog.setResultConverter(dialogButton ->
                (dialogButton == submitButton) ? new Pair<>(textField1.getText(), textField2.getText()) : null //If Submit button is pressed the doubleDialog Strings are set to textField1 and textField2.
                    //If any other button is pressed or if submitButton is not pressed return null.
        );

        return doubleDialog.showAndWait();
    }

    //Show a single input dialog that returns a single String (It gets three strings for its title, header, and context).
    public static Optional<String> SingleInputDialog(String title, String header, String contentText) {
        TextInputDialog dialog = new TextInputDialog(); //Create a TextInputDialog called dialog
        dialog.setTitle(title);                         //Set the title of dialog to 'title'
        dialog.setHeaderText(header);                   //Set header of dialog box to be 'header'
        dialog.setContentText(contentText);
        return dialog.showAndWait();
    }

    //Show an alert dialog with specified title and message
    public static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //Method to create a GridPane layout for dialog content
    private static GridPane createDialogLayout(Node... nodes) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 15, 20, 10));

        for (int i = 0; i < nodes.length; i++) {
            gridPane.add(nodes[i], i % 2, i / 2);
        }

        return gridPane;
    }

    // Method to enable or disable buttons depending on the state of the reference node.
    private static void configureButtonState(Node button, TextField... fields) {
        button.setDisable(true);
        for (TextField field : fields) {
            field.textProperty().addListener((observable, oldValue, newValue) -> {
                boolean disable = false;
                for (TextField f : fields) {
                    disable |= f.getText().trim().isEmpty();
                }
                button.setDisable(disable);
            });
        }
    }
}