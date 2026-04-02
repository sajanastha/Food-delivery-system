package com.fooddelivery;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ComboBox<String> userCategoryCombo;
    @FXML private ListView<String> userListView;
    @FXML private TextArea userDetailArea;
    @FXML private Label userStatus;

    @FXML private ListView<String> restaurantListView;
    @FXML private TextArea restaurantDetailArea;

    @FXML private ListView<String> deliveryListView;
    @FXML private TextArea deliveryDetailArea;
    @FXML private Label deliveryStatus;
    @FXML private Label globalStatus;

    private final UserDAO userDAO = new UserDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    private List<User> filteredUsers = new ArrayList<>();
    private List<Restaurant> allRestaurants = new ArrayList<>();
    private List<Order> visibleOrders = new ArrayList<>();

    @FXML
    public void initialize() {
        User admin = SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Admin Panel - " + admin.getFullName());
        userCategoryCombo.setItems(FXCollections.observableArrayList(
                "Customers",
                "Drivers"
        ));
        userCategoryCombo.setValue("Customers");
        loadUsers();
        loadRestaurants();
        loadDeliveries(false);
    }

    private void loadUsers() {
        try {
            List<User> allUsers = userDAO.getAllUsers();
            String category = userCategoryCombo.getValue();
            filteredUsers = new ArrayList<>();

            for (User user : allUsers) {
                if ("Customers".equals(category) && user.getRole() == UserRole.CUSTOMER) {
                    filteredUsers.add(user);
                }
                if ("Drivers".equals(category) && user.getRole() == UserRole.DRIVER) {
                    filteredUsers.add(user);
                }
            }

            List<String> display = new ArrayList<>();
            for (User u : filteredUsers) {
                display.add(u.getFullName()
                        + " - " + u.getEmail()
                        + (u.isActive() ? "" : " - INACTIVE"));
            }

            userListView.setItems(FXCollections.observableArrayList(display));
            userDetailArea.clear();
            userStatus.setText("");
            globalStatus.setText(category + ": " + filteredUsers.size());
        } catch (SQLException e) {
            globalStatus.setText("Error loading users.");
        }
    }

    @FXML
    private void handleUserCategoryChanged(ActionEvent event) {
        loadUsers();
    }

    @FXML
    private void handleUserSelected() {
        int idx = userListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= filteredUsers.size()) return;
        User u = filteredUsers.get(idx);
        userDetailArea.setText(
                "Name:    " + u.getFullName() + "\n"
                        + "Email:   " + u.getEmail() + "\n"
                        + "Phone:   " + u.getPhone() + "\n"
                        + "Role:    " + u.getRole() + "\n"
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
        User target = filteredUsers.get(idx);
        try {
            userDAO.setUserActive(target.getUserID(), false);
            userStatus.setText(target.getFullName() + " deactivated.");
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
            userDAO.setUserActive(filteredUsers.get(idx).getUserID(), true);
            userStatus.setText("User reactivated.");
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

        User target = filteredUsers.get(idx);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to permanently delete " + target.getFullName() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userDAO.deleteUser(target.getUserID());
                    userStatus.setText("User deleted.");
                    userDetailArea.clear();
                    loadUsers();
                } catch (SQLException e) {
                    userStatus.setText("Error deleting user.");
                }
            }
        });
    }

    private void loadRestaurants() {
        try {
            allRestaurants = restaurantDAO.getAll();
            List<String> display = new ArrayList<>();
            for (Restaurant r : allRestaurants) {
                display.add(r.getRestaurantName()
                        + " | " + r.getCuisineType()
                        + " | Rating " + String.format("%.1f", r.getRating()));
            }
            restaurantListView.setItems(FXCollections.observableArrayList(display));
        } catch (SQLException e) {
            globalStatus.setText("Error loading restaurants.");
        }
    }

    @FXML
    private void handleRestaurantSelected() {
        int idx = restaurantListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= allRestaurants.size()) return;
        Restaurant r = allRestaurants.get(idx);
        restaurantDetailArea.setText(
                "Name:     " + r.getRestaurantName() + "\n"
                        + "Owner:    " + r.getFullName() + "\n"
                        + "Address:  " + r.getAddress() + "\n"
                        + "Cuisine:  " + r.getCuisineType() + "\n"
                        + "Rating:   " + String.format("%.1f", r.getRating()) + "\n"
                        + "Hours:    " + r.getOperatingHours()
        );
    }

    private void loadDeliveries(boolean ongoingOnly) {
        try {
            visibleOrders = ongoingOnly
                    ? orderDAO.getByStatus(OrderStatus.IN_DELIVERY)
                    : orderDAO.getAllOrders();

            List<String> display = new ArrayList<>();
            for (Order order : visibleOrders) {
                display.add("Order #" + order.getOrderID()
                        + " - " + getRestaurantName(order.getRestaurantID())
                        + " - " + order.getStatus());
            }

            deliveryListView.setItems(FXCollections.observableArrayList(display));
            deliveryDetailArea.clear();
            deliveryStatus.setText(ongoingOnly
                    ? "Showing ongoing deliveries only."
                    : "Showing all deliveries.");
        } catch (SQLException e) {
            deliveryStatus.setText("Error loading deliveries.");
        }
    }

    @FXML
    private void handleShowAllDeliveries(ActionEvent event) {
        loadDeliveries(false);
    }

    @FXML
    private void handleShowOngoingDeliveries(ActionEvent event) {
        loadDeliveries(true);
    }

    @FXML
    private void handleDeliverySelected() {
        int idx = deliveryListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= visibleOrders.size()) return;
        Order order = visibleOrders.get(idx);
        StringBuilder items = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            if (items.length() > 0) items.append("\n");
            items.append("- ")
                    .append(item.getItemName())
                    .append(" x")
                    .append(item.getQuantity());
        }

        deliveryDetailArea.setText(
                "Order ID:   " + order.getOrderID() + "\n"
                        + "Restaurant: " + getRestaurantName(order.getRestaurantID()) + "\n"
                        + "Customer ID: " + order.getCustomerID() + "\n"
                        + "Driver ID:   " + order.getDriverID() + "\n"
                        + "Status:      " + order.getStatus() + "\n"
                        + "Address:     " + order.getDeliveryAddress() + "\n"
                        + "Total:       NPR " + String.format("%.2f", order.getTotalAmount()) + "\n"
                        + "Items:\n" + items
        );
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadUsers();
        loadRestaurants();
        loadDeliveries(false);
        globalStatus.setText("Data refreshed.");
    }

    private String getRestaurantName(int restaurantID) {
        try {
            Restaurant restaurant = restaurantDAO.getById(restaurantID);
            if (restaurant != null) {
                return restaurant.getRestaurantName();
            }
        } catch (SQLException ignored) {
        }
        return "Restaurant #" + restaurantID;
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        loadScreen(event, "/com/fooddelivery/views/LogIn.fxml", "Login");
    }

    private void loadScreen(ActionEvent event, String path, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Food Delivery - " + title);
            stage.setMaximized(false);
            stage.setScene(new Scene(root, 480, 520));
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
