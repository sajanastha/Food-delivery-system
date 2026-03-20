package java;


import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {

    @FXML private Label            welcomeLabel;
    @FXML private ListView<String> userListView;
    @FXML private TextArea         userDetailArea;
    @FXML private Label            userStatus;
    @FXML private ListView<String> restaurantListView;
    @FXML private TextArea         restaurantDetailArea;
    @FXML private Label            globalStatus;

    private final UserDAO       userDAO       = new UserDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();

    private List<User>       allUsers       = new ArrayList<>();
    private List<Restaurant> allRestaurants = new ArrayList<>();

    @FXML
    public void initialize() {
        User admin = SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText(
            "Admin Panel — " + admin.getFullName()
        );
        loadUsers();
        loadRestaurants();
    }

    // ── Users 
    private void loadUsers() {
        try {
            allUsers = userDAO.getAllUsers();
            List<String> display = new ArrayList<>();
            for (User u : allUsers)
                display.add("[" + u.getRole() + "]  "
                        + u.getFullName()
                        + "  —  " + u.getEmail()
                        + (u.isActive() ? "" : "  ⛔ INACTIVE"));
            userListView.setItems(
                FXCollections.observableArrayList(display)
            );
            globalStatus.setText(
                "Total users: " + allUsers.size()
            );
        } catch (SQLException e) {
            globalStatus.setText("Error loading users.");
        }
    }

    @FXML
    private void handleUserSelected() {
        int idx = userListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= allUsers.size()) return;
        User u = allUsers.get(idx);
        userDetailArea.setText(
            "Name:    " + u.getFullName()   + "\n"
            + "Email:   " + u.getEmail()    + "\n"
            + "Phone:   " + u.getPhone()    + "\n"
            + "Role:    " + u.getRole()     + "\n"
            + "Active:  " + (u.isActive() ? "Yes" : "No")
        );
    }

    @FXML
    private void handleDeactivate(ActionEvent event) {
        int idx = userListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            userStatus.setText("Select a user first.");
            return;
        }
        User target = allUsers.get(idx);
        if (target.getEmail().equals("admin@gmail.com")) {
            userStatus.setText(
                "Cannot deactivate the built-in admin account."
            );
            return;
        }
        try {
            userDAO.setUserActive(target.getUserID(), false);
            userStatus.setText(
                "✓ " + target.getFullName() + " deactivated."
            );
            loadUsers();
        } catch (SQLException e) {
            userStatus.setText("Error deactivating user.");
        }
    }

    @FXML
    private void handleReactivate(ActionEvent event) {
        int idx = userListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            userStatus.setText("Select a user first.");
            return;
        }
        try {
            userDAO.setUserActive(allUsers.get(idx).getUserID(), true);
            userStatus.setText("✓ User reactivated.");
            loadUsers();
        } catch (SQLException e) {
            userStatus.setText("Error reactivating user.");
        }
    }

    @FXML
    private void handleDeleteUser(ActionEvent event) {
        int idx = userListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            userStatus.setText("Select a user first.");
            return;
        }
        User target = allUsers.get(idx);
        // Confirm before deleting
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText(null);
        confirm.setContentText(
            "Are you sure you want to permanently delete "
            + target.getFullName() + "?"
        );
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userDAO.deleteUser(target.getUserID());
                    userStatus.setText("✓ User deleted.");
                    userDetailArea.clear();
                    loadUsers();
                } catch (SQLException e) {
                    userStatus.setText("Error deleting user.");
                }
            }
        });
    }

    // Restaurants
    private void loadRestaurants() {
        try {
            allRestaurants = restaurantDAO.getAll();
            List<String> display = new ArrayList<>();
            for (Restaurant r : allRestaurants)
                display.add("🍴  " + r.getRestaurantName()
                        + "  |  " + r.getCuisineType()
                        + "  |  ⭐ "
                        + String.format("%.1f", r.getRating()));
            restaurantListView.setItems(
                FXCollections.observableArrayList(display)
            );
        } catch (SQLException e) {
            globalStatus.setText("Error loading restaurants.");
        }
    }

    @FXML
    private void handleRestaurantSelected() {
        int idx = restaurantListView.getSelectionModel()
                                    .getSelectedIndex();
        if (idx < 0 || idx >= allRestaurants.size()) return;
        Restaurant r = allRestaurants.get(idx);
        restaurantDetailArea.setText(
            "Name:     " + r.getRestaurantName()    + "\n"
            + "Owner:    " + r.getFullName()         + "\n"
            + "Address:  " + r.getAddress()           + "\n"
            + "Cuisine:  " + r.getCuisineType()       + "\n"
            + "Rating:   "
            + String.format("%.1f", r.getRating())    + "\n"
            + "Hours:    " + r.getOperatingHours()
        );
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadUsers();
        loadRestaurants();
        globalStatus.setText("Data refreshed.");
    }

    // Logout 
    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        loadScreen(event,
                "/com/fooddelivery/views/Login.fxml", "Login");
    }

    private void loadScreen(ActionEvent event,
                             String path, String title) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(path)
            );
            Stage stage = (Stage)((Node) event.getSource())
                              .getScene().getWindow();
            stage.setTitle("Food Delivery — " + title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
