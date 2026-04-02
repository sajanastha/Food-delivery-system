package com.fooddelivery;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DriverDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label availabilityLabel;
    @FXML private CheckBox availabilityToggle;
    @FXML private Label summaryLabel;

    @FXML private ListView<String> requestListView;
    @FXML private TextArea requestDetailArea;
    @FXML private Label requestStatus;

    @FXML private ListView<String> activeDeliveryListView;
    @FXML private TextArea activeDetailArea;
    @FXML private Label activeStatus;

    @FXML private ListView<String> historyListView;
    @FXML private TextArea historyDetailArea;
    @FXML private Label historyStatus;

    @FXML private TextArea profileArea;

    private final OrderDAO orderDAO = new OrderDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();

    private Driver me;
    private List<Order> availableRequests = new ArrayList<>();
    private List<Order> activeDeliveries = new ArrayList<>();
    private List<Order> completedDeliveries = new ArrayList<>();
    private final Set<Integer> skippedRequestIds = new HashSet<>();

    @FXML
    public void initialize() {
        me = (Driver) SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Driver Dashboard - " + me.getFullName());
        availabilityToggle.setSelected(me.isAvailable());
        updateAvailabilityLabel();
        refreshAll();
        loadProfile();
    }

    @FXML
    private void handleAvailabilityChanged(ActionEvent event) {
        me.setAvailable(availabilityToggle.isSelected());
        updateAvailabilityLabel();
        refreshRequests();
        requestStatus.setText(me.isAvailable()
                ? "You are available for delivery requests."
                : "You are offline. New requests are hidden.");
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshAll();
        requestStatus.setText("Driver data refreshed.");
    }

    @FXML
    private void handleRequestSelected() {
        int idx = requestListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= availableRequests.size()) {
            requestDetailArea.clear();
            return;
        }
        requestDetailArea.setText(buildOrderDetails(availableRequests.get(idx)));
    }

    @FXML
    private void handleAcceptRequest(ActionEvent event) {
        int idx = requestListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= availableRequests.size()) {
            requestStatus.setText("Select a delivery request first.");
            return;
        }
        if (!me.isAvailable()) {
            requestStatus.setText("Turn availability on before accepting a delivery.");
            return;
        }

        Order order = availableRequests.get(idx);
        try {
            orderDAO.assignDriver(order.getOrderID(), me.getUserID());
            me.acceptOrder(order.getOrderID());
            activeStatus.setText("Accepted order #" + order.getOrderID() + ".");
            requestStatus.setText("Delivery request accepted.");
            refreshAll();
        } catch (SQLException e) {
            requestStatus.setText("Could not accept the delivery.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRejectRequest(ActionEvent event) {
        int idx = requestListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= availableRequests.size()) {
            requestStatus.setText("Select a delivery request first.");
            return;
        }

        Order order = availableRequests.get(idx);
        skippedRequestIds.add(order.getOrderID());
        requestStatus.setText("Skipped order #" + order.getOrderID() + " for now.");
        refreshRequests();
    }

    @FXML
    private void handleActiveSelected() {
        int idx = activeDeliveryListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= activeDeliveries.size()) {
            activeDetailArea.clear();
            return;
        }
        activeDetailArea.setText(buildOrderDetails(activeDeliveries.get(idx)));
    }

    @FXML
    private void handleMarkDelivered(ActionEvent event) {
        int idx = activeDeliveryListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= activeDeliveries.size()) {
            activeStatus.setText("Select an active delivery first.");
            return;
        }

        Order order = activeDeliveries.get(idx);
        try {
            orderDAO.updateStatus(order.getOrderID(), OrderStatus.DELIVERED);
            if (me.getCurrentOrderID() == order.getOrderID()) {
                me.completeDelivery();
                availabilityToggle.setSelected(me.isAvailable());
                updateAvailabilityLabel();
            }
            activeStatus.setText("Order #" + order.getOrderID() + " marked as delivered.");
            refreshAll();
        } catch (SQLException e) {
            activeStatus.setText("Could not update delivery status.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelDelivery(ActionEvent event) {
        int idx = activeDeliveryListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= activeDeliveries.size()) {
            activeStatus.setText("Select an active delivery first.");
            return;
        }

        Order order = activeDeliveries.get(idx);
        try {
            orderDAO.updateStatus(order.getOrderID(), OrderStatus.CANCELLED);
            if (me.getCurrentOrderID() == order.getOrderID()) {
                me.completeDelivery();
                availabilityToggle.setSelected(me.isAvailable());
                updateAvailabilityLabel();
            }
            activeStatus.setText("Order #" + order.getOrderID() + " cancelled.");
            refreshAll();
        } catch (SQLException e) {
            activeStatus.setText("Could not cancel the delivery.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleHistorySelected() {
        int idx = historyListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= completedDeliveries.size()) {
            historyDetailArea.clear();
            return;
        }
        historyDetailArea.setText(buildOrderDetails(completedDeliveries.get(idx)));
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        loadScreen(event, "/com/fooddelivery/views/LogIn.fxml", "Login");
    }

    private void refreshAll() {
        refreshRequests();
        refreshActiveDeliveries();
        refreshHistory();
        loadProfile();
        summaryLabel.setText(buildSummary());
    }

    private void refreshRequests() {
        try {
            availableRequests = new ArrayList<>();
            if (me.isAvailable()) {
                for (Order order : orderDAO.getAvailableForDrivers()) {
                    if (!skippedRequestIds.contains(order.getOrderID())) {
                        availableRequests.add(order);
                    }
                }
            }

            List<String> display = new ArrayList<>();
            for (Order order : availableRequests) {
                display.add("Order #" + order.getOrderID()
                        + "  " + getRestaurantName(order.getRestaurantID())
                        + "  NPR " + String.format("%.2f", order.getTotalAmount()));
            }

            requestListView.setItems(FXCollections.observableArrayList(display));
            if (availableRequests.isEmpty()) {
                requestDetailArea.setText(me.isAvailable()
                        ? "No open delivery requests right now."
                        : "Set yourself as available to receive delivery requests.");
            } else {
                requestDetailArea.clear();
            }
        } catch (SQLException e) {
            requestStatus.setText("Could not load delivery requests.");
        }
    }

    private void refreshActiveDeliveries() {
        try {
            activeDeliveries = orderDAO.getByDriverAndStatus(
                    me.getUserID(), OrderStatus.IN_DELIVERY);

            List<String> display = new ArrayList<>();
            for (Order order : activeDeliveries) {
                display.add("Order #" + order.getOrderID()
                        + "  ->  " + order.getDeliveryAddress());
            }

            activeDeliveryListView.setItems(FXCollections.observableArrayList(display));
            if (activeDeliveries.isEmpty()) {
                activeDetailArea.setText("No ongoing deliveries.");
                if (me.getCurrentOrderID() != 0) {
                    me.setCurrentOrderID(0);
                    me.setAvailable(true);
                    availabilityToggle.setSelected(true);
                    updateAvailabilityLabel();
                }
            } else {
                me.setCurrentOrderID(activeDeliveries.get(0).getOrderID());
                me.setAvailable(false);
                availabilityToggle.setSelected(false);
                updateAvailabilityLabel();
            }
        } catch (SQLException e) {
            activeStatus.setText("Could not load active deliveries.");
        }
    }

    private void refreshHistory() {
        try {
            completedDeliveries = orderDAO.getByDriver(me.getUserID());
            completedDeliveries.removeIf(order ->
                    order.getStatus() == OrderStatus.IN_DELIVERY);

            List<String> display = new ArrayList<>();
            for (Order order : completedDeliveries) {
                display.add("Order #" + order.getOrderID()
                        + "  " + order.getStatus()
                        + "  NPR " + String.format("%.2f", order.getTotalAmount()));
            }

            historyListView.setItems(FXCollections.observableArrayList(display));
            historyStatus.setText(completedDeliveries.size()
                    + " past delivery record(s)");
            if (completedDeliveries.isEmpty()) {
                historyDetailArea.setText("Your completed or cancelled deliveries will appear here.");
            }
        } catch (SQLException e) {
            historyStatus.setText("Could not load delivery history.");
        }
    }

    private void loadProfile() {
        profileArea.setText(
                "Name: " + me.getFullName() + "\n"
                        + "Email: " + me.getEmail() + "\n"
                        + "Phone: " + safe(me.getPhone()) + "\n"
                        + "Availability: " + (me.isAvailable() ? "Available" : "Busy") + "\n"
                        + "Current Order ID: "
                        + (me.getCurrentOrderID() == 0 ? "None" : me.getCurrentOrderID())
        );
    }

    private void updateAvailabilityLabel() {
        availabilityLabel.setText(me.isAvailable()
                ? "Available for requests"
                : "Busy with delivery");
    }

    private String buildSummary() {
        return availableRequests.size() + " open requests   |   "
                + activeDeliveries.size() + " active deliveries   |   "
                + completedDeliveries.size() + " delivery history";
    }

    private String buildOrderDetails(Order order) {
        StringBuilder items = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            if (items.length() > 0) {
                items.append("\n");
            }
            items.append("- ")
                    .append(item.getItemName())
                    .append(" x")
                    .append(item.getQuantity())
                    .append("  (NPR ")
                    .append(String.format("%.2f", item.getSubtotal()))
                    .append(")");
        }

        return "Order ID: " + order.getOrderID() + "\n"
                + "Restaurant: " + getRestaurantName(order.getRestaurantID()) + "\n"
                + "Customer ID: " + order.getCustomerID() + "\n"
                + "Driver ID: " + order.getDriverID() + "\n"
                + "Status: " + order.getStatus() + "\n"
                + "Delivery Address: " + order.getDeliveryAddress() + "\n"
                + "Delivery Fee: NPR " + String.format("%.2f", order.getDeliveryFee()) + "\n"
                + "Total: NPR " + String.format("%.2f", order.getTotalAmount()) + "\n"
                + "Items:\n" + (items.isEmpty() ? "- No items found" : items);
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

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText(null);
            alert.setContentText("Could not open " + title + ".");
            alert.showAndWait();
            e.printStackTrace();
        }
    }
}
