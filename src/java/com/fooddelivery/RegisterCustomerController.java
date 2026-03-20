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

public class RegisterCustomerController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private TextField     phoneField;
    @FXML private TextField     addressField;
    @FXML private Label         errorLabel;
    @FXML private Label         successLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleRegister(ActionEvent event) {
        errorLabel.setText("");
        successLabel.setText("");

        String fullName = fullNameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm  = confirmField.getText().trim();
        String phone    = phoneField.getText().trim();

        // ── Validation ────────────────────────────────────────────────
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Full name, email and password are required.");
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

        // ── Save to database ──────────────────────────────────────────
        try {
            if (userDAO.emailExists(email)) {
                errorLabel.setText("An account with this email already exists.");
                return;
            }

            int newID = userDAO.createUser(
                email, password, fullName, phone, "CUSTOMER"
            );

            if (newID > 0) {
                successLabel.setText(
                    "Account created! You can now go back and log in."
                );
                showPopup("Registration Successful",
                          "Welcome, " + fullName + "!\n" +
                          "Your customer account has been created.\n" +
                          "You can now log in.");
                clearForm();
            } else {
                errorLabel.setText("Registration failed. Please try again.");
            }

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
        fullNameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmField.clear();
        phoneField.clear();
        addressField.clear();
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