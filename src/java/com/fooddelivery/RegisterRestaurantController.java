package com.fooddelivery;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.SQLException;

public class RegisterRestaurantController {

    @FXML private TextField     ownerNameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private TextField     phoneField;
    @FXML private TextField     restaurantNameField;
    @FXML private TextField     addressField;
    @FXML private TextField     cuisineField;
    @FXML private TextField     hoursField;
    @FXML private Label         errorLabel;
    @FXML private Label         successLabel;

    private final UserDAO       userDAO       = new UserDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();

    @FXML
    private void handleRegister(ActionEvent event) {
        errorLabel.setText("");
        successLabel.setText("");

        String ownerName   = ownerNameField.getText().trim();
        String email       = emailField.getText().trim();
        String password    = passwordField.getText().trim();
        String confirm     = confirmField.getText().trim();
        String phone       = phoneField.getText().trim();
        String restName    = restaurantNameField.getText().trim();
        String address     = addressField.getText().trim();
        String cuisine     = cuisineField.getText().trim();
        String hours       = hoursField.getText().trim();

        // ── Validation ────────────────────────────────────────────────
        if (ownerName.isEmpty() || email.isEmpty() ||
                password.isEmpty() || restName.isEmpty() || address.isEmpty()) {
            errorLabel.setText(
                "Owner name, email, password, restaurant name " +
                "and address are all required."
            );
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            errorLabel.setText("Please enter a valid email address.");
            return;
        }
        if (password.length() < 4) {
            errorLabel.setText("Password must be at least 4 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        // ── Save to database 
        try {
            if (userDAO.emailExists(email)) {
                errorLabel.setText(
                    "An account with this email already exists."
                );
                return;
            }

            // Step 1 — create the user account
            int userID = userDAO.createUser(
                email, password, ownerName, phone, "RESTAURANT"
            );

            if (userID < 0) {
                errorLabel.setText("Registration failed. Please try again.");
                return;
            }

            // Step 2 — create the restaurant record linked to the user
            restaurantDAO.addRestaurant(
                userID, restName, address, cuisine, hours
            );

            successLabel.setText(
                "Restaurant registered! You can now go back and log in."
            );
            showPopup("Registration Successful",
                      "Welcome, " + ownerName + "!\n" +
                      "Your restaurant '" + restName + "' has been registered.\n" +
                      "You can now log in with your email and password.");
            clearForm();

        } catch (SQLException e) {
            errorLabel.setText("System error. Please try again.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        loadScreen(event,
                "/com/fooddelivery/views/ChooseRole.fxml",
                "Register — Choose Role");
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        loadScreen(event,
                "/com/fooddelivery/views/Login.fxml",
                "Login");
    }

    private void showPopup(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        ownerNameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmField.clear();
        phoneField.clear();
        restaurantNameField.clear();
        addressField.clear();
        cuisineField.clear();
        hoursField.clear();
    }

    private void loadScreen(ActionEvent event,
                             String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(fxmlPath)
            );
            Stage stage = (Stage)((Node) event.getSource())
                              .getScene().getWindow();
            stage.setTitle("Food Delivery — " + title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}