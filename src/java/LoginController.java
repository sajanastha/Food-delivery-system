package java;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

        // Don't hit the database if fields are empty
        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter your email and password.");
            return;
        }

        // Admin shortcut — no database check needed
        if (email.equals("admin@gmail.com") && password.equals("admin123")) {
            Admin admin = new Admin(0, "admin@gmail.com", "admin123", "Administrator", "");
            SessionManager.getInstance().setCurrentUser(admin);
            SceneNavigator.navigateTo(event, SceneNavigator.ADMIN_DASHBOARD);
            return;
        }

        // Normal login — check against the database
        try {
            User user = userDAO.validateLogin(email, password);

            if (user == null) {
                errorLabel.setText("Incorrect email or password.");
                passwordField.clear();
                return;
            }

            // Save the logged in user so all screens can access it
            SessionManager.getInstance().setCurrentUser(user);

            // Route to the correct dashboard based on role
            String destination = switch (user.getRole()) {
                case CUSTOMER   -> SceneNavigator.CUSTOMER_DASHBOARD;
                case RESTAURANT -> SceneNavigator.RESTAURANT_DASHBOARD;
                case DRIVER     -> SceneNavigator.DRIVER_DASHBOARD;
                case ADMIN      -> SceneNavigator.ADMIN_DASHBOARD;
            };

            SceneNavigator.navigateTo(event, destination);

        } catch (SQLException e) {
            errorLabel.setText("Could not connect to database. Try again.");
            e.printStackTrace();
        }
    }

    // Register button — goes to role selection screen
    @FXML
    private void handleRegister(ActionEvent event) {
        SceneNavigator.navigateTo(event, SceneNavigator.CHOOSE_ROLE);
    }
}