package com.fooddelivery;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin(ActionEvent event) {
        errorLabel.setText("");

        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        // Empty field check
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            return;
        }

        // Admin shortcut — no database needed
        if (email.equals("admin@gmail.com") && password.equals("admin123")) {
            Admin admin = new Admin(0, "admin@gmail.com",
                                   "admin123", "Administrator", "");
            SessionManager.getInstance().setCurrentUser(admin);
            loadScreen(event, "/com/fooddelivery/views/AdminDashboard.fxml",
                       "Admin Dashboard");
            return;
        }

        // Check database
        try {
            User user = userDAO.validateLogin(email, password);

            if (user == null) {
                // Show a pop-up alert
                showPopup(AlertType.ERROR,
                          "User Not Found",
                          "No account matches that email and password.\n" +
                          "Please check your details or register first.");
                passwordField.clear();
                return;
            }

            // Save logged-in user to session
            SessionManager.getInstance().setCurrentUser(user);

            // Route to correct dashboard
            String fxml = switch (user.getRole()) {
                case CUSTOMER    -> "/com/fooddelivery/views/CustomerDashboard.fxml";
                case RESTAURANT  -> "/com/fooddelivery/views/RestaurantDashboard.fxml";
                case DRIVER      -> "/com/fooddelivery/views/DriverDashboard.fxml";
                case ADMIN       -> "/com/fooddelivery/views/AdminDashboard.fxml";
            };

            showPopup(AlertType.INFORMATION,
                      "Welcome back!",
                      "Logged in as: " + user.getFullName());

            loadScreen(event, fxml, user.getRole().toString());

        } catch (SQLException e) {
            showError("Could not connect to database. Please try again.");
            e.printStackTrace();
        }
    }

    // Go to role selection screen
    @FXML
    private void handleRegister(ActionEvent event) {
        loadScreen(event,
                   "/com/fooddelivery/views/ChooseRole.fxml",
                   "Register — Choose Role");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void showPopup(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadScreen(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(fxmlPath)
            );
            Stage stage = (Stage)((Node) event.getSource())
                              .getScene().getWindow();
            stage.setTitle("Food Delivery — " + title);
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            showError("Screen failed to load: " + fxmlPath);
            e.printStackTrace();
        }
    }
}