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

public class RegisterDriverController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private TextField     phoneField;
    @FXML private TextField     licenseField;
    @FXML private ComboBox<String> vehicleCombo;
    @FXML private Label         errorLabel;
    @FXML private Label         successLabel;

    private final UserDAO userDAO = new UserDAO();

    // Runs automatically when the screen loads
    @FXML
    public void initialize() {
        vehicleCombo.getItems().addAll(
            "Motorcycle",
            "Bicycle",
            "Car",
            "Scooter",
            "Van"
        );
        vehicleCombo.setValue("Motorcycle");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        errorLabel.setText("");
        successLabel.setText("");

        String fullName = fullNameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm  = confirmField.getText().trim();
        String phone    = phoneField.getText().trim();
        String license  = licenseField.getText().trim();
        String vehicle  = vehicleCombo.getValue();

        // ── Validation ────────────────────────────────────────────────
        if (fullName.isEmpty() || email.isEmpty() ||
                password.isEmpty() || license.isEmpty()) {
            errorLabel.setText(
                "Full name, email, password and licence " +
                "number are all required."
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

        // ── Save to database ──────────────────────────────────────────
        try {
            if (userDAO.emailExists(email)) {
                errorLabel.setText(
                    "An account with this email already exists."
                );
                return;
            }

            int newID = userDAO.createUser(
                email, password, fullName, phone, "DRIVER"
            );

            if (newID > 0) {
                successLabel.setText(
                    "Driver account created! You can now go back and log in."
                );
                showPopup("Registration Successful",
                          "Welcome, " + fullName + "!\n" +
                          "Your driver account has been created.\n" +
                          "Vehicle: " + vehicle + "\n" +
                          "You can now log in with your email and password.");
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
        licenseField.clear();
        vehicleCombo.setValue("Motorcycle");
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
