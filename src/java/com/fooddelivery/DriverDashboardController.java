package com.fooddelivery;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DriverDashboardController {

    @FXML private Label    welcomeLabel;
    @FXML private Label    availabilityLabel;
    @FXML private CheckBox availabilityToggle;
    @FXML private Label    summaryLabel;

    @FXML private ListView<String> requestListView;
    @FXML private TextArea         requestDetailArea;
    @FXML private Label            requestStatus;

    @FXML private ListView<String> activeDeliveryListView;
    @FXML private TextArea         activeDetailArea;
    @FXML private Label            activeStatus;

    @FXML private ListView<String> historyListView;
    @FXML private TextArea         historyDetailArea;
    @FXML private Label            historyStatus;

    @FXML private TextArea profileArea;
    @FXML private Button requestsNavButton;
    @FXML private Button activeNavButton;
    @FXML private Button historyNavButton;
    @FXML private Button profileNavButton;
    @FXML private Button feedbackNavButton;

    // Panels for bottom nav switching
    @FXML private VBox requestsPanel;
    @FXML private VBox activePanel;
    @FXML private VBox driverHistoryPanel;
    @FXML private VBox driverProfilePanel;
    @FXML private VBox driverFeedbackPanel;
    @FXML private ListView<FeedbackEntry> driverFeedbackListView;
    @FXML private Label driverFeedbackCountLabel;
    @FXML private Label driverFeedbackStatus;

    private final OrderDAO      orderDAO      = new OrderDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final FeedbackDAO   feedbackDAO   = new FeedbackDAO();

    private Driver       me;
    private List<Order>  availableRequests  = new ArrayList<>();
    private List<Order>  activeDeliveries   = new ArrayList<>();
    private List<Order>  completedDeliveries= new ArrayList<>();
    private final Set<Integer> skippedRequestIds = new HashSet<>();

    private static final String ACTIVE_NAV_STYLE =
            "-fx-background-color: #FF7518;"
            + "-fx-text-fill: white;"
            + "-fx-font-size: 13px;"
            + "-fx-font-weight: bold;"
            + "-fx-cursor: hand;"
            + "-fx-background-radius: 0;";

    private static final String INACTIVE_NAV_STYLE =
            "-fx-background-color: #F5F5DC;"
            + "-fx-text-fill: #555;"
            + "-fx-font-size: 13px;"
            + "-fx-cursor: hand;"
            + "-fx-background-radius: 0;";

    @FXML
    public void initialize() {
        me = (Driver) SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Driver: " + me.getFullName());
        availabilityToggle.setSelected(me.isAvailable());
        updateAvailabilityLabel();
        refreshAll();
        loadProfile();
        showPanel("requests");
    }

    // ── Bottom nav panel switching ────────────────────────────────────────────
    private void showPanel(String name) {
        requestsPanel.setVisible(false);
        requestsPanel.setManaged(false);
        activePanel.setVisible(false);
        activePanel.setManaged(false);
        driverHistoryPanel.setVisible(false);
        driverHistoryPanel.setManaged(false);
        driverProfilePanel.setVisible(false);
        driverProfilePanel.setManaged(false);
        driverFeedbackPanel.setVisible(false);
        driverFeedbackPanel.setManaged(false);

        switch (name) {
            case "requests" -> {
                requestsPanel.setVisible(true);
                requestsPanel.setManaged(true);
                setActiveNav(requestsNavButton);
            }
            case "active" -> {
                activePanel.setVisible(true);
                activePanel.setManaged(true);
                setActiveNav(activeNavButton);
            }
            case "history" -> {
                driverHistoryPanel.setVisible(true);
                driverHistoryPanel.setManaged(true);
                setActiveNav(historyNavButton);
            }
            case "profile" -> {
                driverProfilePanel.setVisible(true);
                driverProfilePanel.setManaged(true);
                setActiveNav(profileNavButton);
            }
            case "feedback" -> {
                driverFeedbackPanel.setVisible(true);
                driverFeedbackPanel.setManaged(true);
                setActiveNav(feedbackNavButton);
                loadDriverFeedback();
            }
        }
    }

    private void setActiveNav(Button activeButton) {
        requestsNavButton.setStyle(INACTIVE_NAV_STYLE);
        activeNavButton.setStyle(INACTIVE_NAV_STYLE);
        historyNavButton.setStyle(INACTIVE_NAV_STYLE);
        profileNavButton.setStyle(INACTIVE_NAV_STYLE);
        feedbackNavButton.setStyle(INACTIVE_NAV_STYLE);
        if (activeButton != null) {
            activeButton.setStyle(ACTIVE_NAV_STYLE);
        }
    }

    @FXML public void goRequests(ActionEvent e) { showPanel("requests"); }
    @FXML public void goActive(ActionEvent e)   { showPanel("active"); }
    @FXML public void goHistory(ActionEvent e)  { showPanel("history"); }
    @FXML public void goProfile(ActionEvent e)  { showPanel("profile"); }
    @FXML public void goFeedback(ActionEvent e) { showPanel("feedback"); }

    private void loadDriverFeedback() {
        driverFeedbackListView.setCellFactory(
                lv -> new FeedbackCardCell(FeedbackCardCell.Mode.RECEIVED));
        try {
            List<FeedbackEntry> entries = feedbackDAO.getByDriver(me.getUserID());
            if (entries.isEmpty()) {
                driverFeedbackListView.setItems(FXCollections.observableArrayList());
                driverFeedbackCountLabel.setText("0 feedback(s)");
                driverFeedbackStatus.setText("No feedback received yet. Feedback appears here after customers rate delivered orders.");
                return;
            }
            driverFeedbackListView.setItems(FXCollections.observableArrayList(entries));
            driverFeedbackCountLabel.setText(entries.size() + " feedback(s)");
            driverFeedbackStatus.setText("");
        } catch (SQLException ex) {
            ex.printStackTrace();
            driverFeedbackListView.setItems(FXCollections.observableArrayList());
            driverFeedbackStatus.setText("Could not load feedback: " + ex.getMessage());
        }
    }

    // ── Availability ──────────────────────────────────────────────────────────
    @FXML
    private void handleAvailabilityChanged(ActionEvent e) {
        me.setAvailable(availabilityToggle.isSelected());
        updateAvailabilityLabel();
        loadRequests();
    }

    private void updateAvailabilityLabel() {
        availabilityLabel.setText(
            me.isAvailable() ? "🟢 Available" : "🔴 Busy");
    }

    // ── Load data ─────────────────────────────────────────────────────────────
    private void refreshAll() {
        loadRequests();
        loadActiveDeliveries();
        loadHistory();
        updateSummary();
    }

    private void loadRequests() {
        try {
            // If driver already has an active delivery, show no new requests
            List<Order> active = orderDAO.getByDriverAndStatus(
                    me.getUserID(), OrderStatus.IN_DELIVERY);
            if (!active.isEmpty() || !me.isAvailable()) {
                requestListView.setItems(FXCollections.observableArrayList());
                requestStatus.setText("You have an active delivery in progress.");
                summaryLabel.setText("0 request(s) waiting");
                return;
            }

            List<Order> all = orderDAO.getAvailableForDrivers();
            availableRequests = new ArrayList<>();
            for (Order o : all)
                if (!skippedRequestIds.contains(o.getOrderID()))
                    availableRequests.add(o);

            List<String> display = new ArrayList<>();
            for (Order o : availableRequests)
                display.add("Order #" + o.getOrderID()
                    + "  →  " + o.getDeliveryAddress()
                    + "  |  NPR "
                    + String.format("%.2f", o.getTotalAmount()));
            requestListView.setItems(
                FXCollections.observableArrayList(display));

            requestStatus.setText(availableRequests.isEmpty()
                    ? "No new requests right now." : "");
            summaryLabel.setText(
                availableRequests.size() + " request(s) waiting");
        } catch (SQLException ex) {
            requestStatus.setText("Error loading requests.");
        }
    }

    private void loadActiveDeliveries() {
        try {
            List<Order> all = orderDAO.getByDriverAndStatus(
                    me.getUserID(), OrderStatus.IN_DELIVERY);
            activeDeliveries = all;

            List<String> display = new ArrayList<>();
            for (Order o : all)
                display.add("Order #" + o.getOrderID()
                    + "  →  " + o.getDeliveryAddress());
            activeDeliveryListView.setItems(
                FXCollections.observableArrayList(display));
        } catch (SQLException ex) {
            activeStatus.setText("Error loading active deliveries.");
        }
    }

    private void loadHistory() {
        try {
            List<Order> delivered = orderDAO.getByDriverAndStatus(
                    me.getUserID(), OrderStatus.DELIVERED);
            List<Order> cancelled = orderDAO.getByDriverAndStatus(
                    me.getUserID(), OrderStatus.CANCELLED);
            completedDeliveries = new ArrayList<>();
            completedDeliveries.addAll(delivered);
            completedDeliveries.addAll(cancelled);

            List<String> display = new ArrayList<>();
            for (Order o : completedDeliveries)
                display.add("Order #" + o.getOrderID()
                    + "  [" + o.getStatus() + "]"
                    + "  →  " + o.getDeliveryAddress());
            historyListView.setItems(
                FXCollections.observableArrayList(display));
        } catch (SQLException ex) {
            historyStatus.setText("Error loading history.");
        }
    }

    private void updateSummary() {
        summaryLabel.setText(
            availableRequests.size() + " available  ·  "
            + activeDeliveries.size() + " active");
    }

    private void loadProfile() {
        profileArea.setText(
            "Name:          " + me.getFullName() + "\n"
            + "Email:         " + me.getEmail() + "\n"
            + "Phone:         "
            + (me.getPhone() == null || me.getPhone().isEmpty()
                ? "Not provided" : me.getPhone()) + "\n"
            + "Licence:       " + me.getLicenseNumber() + "\n"
            + "Vehicle:       " + me.getVehicleType() + "\n"
            + "Status:        "
            + (me.isAvailable() ? "Available" : "On Delivery"));
    }

    // ── Request actions ───────────────────────────────────────────────────────
    @FXML
    private void handleRequestSelected() {
        int idx = requestListView.getSelectionModel()
                                  .getSelectedIndex();
        if (idx < 0 || idx >= availableRequests.size()) return;
        Order o = availableRequests.get(idx);
        requestDetailArea.setText(o.getOrderSummary());
    }

    @FXML
    private void handleAcceptRequest(ActionEvent e) {
        int idx = requestListView.getSelectionModel()
                                  .getSelectedIndex();
        if (idx < 0) {
            requestStatus.setText("Select a request first.");
            return;
        }
        Order o = availableRequests.get(idx);
        try {
            orderDAO.assignDriver(o.getOrderID(),
                    me.getUserID());
            me.acceptOrder(o.getOrderID());
            updateAvailabilityLabel();
            requestStatus.setText(
                "✔ Accepted Order #" + o.getOrderID());
            showAlert("Order Accepted",
                "You accepted Order #" + o.getOrderID()
                + "\nDeliver to: " + o.getDeliveryAddress());
            refreshAll();
            showPanel("active");
        } catch (SQLException ex) {
            requestStatus.setText("Error accepting order.");
        }
    }

    @FXML
    private void handleRejectRequest(ActionEvent e) {
        int idx = requestListView.getSelectionModel()
                                  .getSelectedIndex();
        if (idx < 0) {
            requestStatus.setText("Select a request first.");
            return;
        }
        skippedRequestIds.add(
            availableRequests.get(idx).getOrderID());
        loadRequests();
        requestDetailArea.clear();
        requestStatus.setText("Skipped.");
    }

    // ── Active delivery actions ───────────────────────────────────────────────
    @FXML
    private void handleActiveSelected() {
        int idx = activeDeliveryListView.getSelectionModel()
                                         .getSelectedIndex();
        if (idx < 0 || idx >= activeDeliveries.size()) return;
        activeDetailArea.setText(
            activeDeliveries.get(idx).getOrderSummary());
    }

    @FXML
    private void handleMarkDelivered(ActionEvent e) {
        int idx = activeDeliveryListView.getSelectionModel()
                                         .getSelectedIndex();
        if (idx < 0) {
            activeStatus.setText("Select a delivery first.");
            return;
        }
        Order o = activeDeliveries.get(idx);
        try {
            orderDAO.updateStatus(o.getOrderID(),
                    OrderStatus.DELIVERED);
            me.completeDelivery();
            activeStatus.setText(
                "✔ Order #" + o.getOrderID() + " delivered!");
            showAlert("Delivered!",
                "Order #" + o.getOrderID()
                + " marked as delivered.\nGreat work!");
            refreshAll();
            showPanel("history");
        } catch (SQLException ex) {
            activeStatus.setText("Error updating status.");
        }
    }

    @FXML
    private void handleCancelDelivery(ActionEvent e) {
        int idx = activeDeliveryListView.getSelectionModel()
                                         .getSelectedIndex();
        if (idx < 0) {
            activeStatus.setText("Select a delivery first.");
            return;
        }
        Order o = activeDeliveries.get(idx);
        try {
            orderDAO.updateStatus(o.getOrderID(),
                    OrderStatus.CANCELLED);
            me.completeDelivery();
            activeStatus.setText(
                "Order #" + o.getOrderID() + " cancelled.");
            refreshAll();
        } catch (SQLException ex) {
            activeStatus.setText("Error cancelling.");
        }
    }

    // ── History ───────────────────────────────────────────────────────────────
    @FXML
    private void handleHistorySelected() {
        int idx = historyListView.getSelectionModel()
                                  .getSelectedIndex();
        if (idx < 0 || idx >= completedDeliveries.size()) return;
        historyDetailArea.setText(
            completedDeliveries.get(idx).getOrderSummary());
    }

    // ── Refresh ───────────────────────────────────────────────────────────────
    @FXML
    private void handleRefresh(ActionEvent e) {
        refreshAll();
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @FXML
    private void handleLogout(ActionEvent e) {
        SessionManager.getInstance().logout();
        loadScreen(e,
            "/com/fooddelivery/views/Login.fxml", "Login");
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void loadScreen(ActionEvent e,
                             String path, String title) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(path));
            Stage stage = (Stage) ((Node) e.getSource())
                              .getScene().getWindow();
            stage.setTitle("Food Delivery — " + title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}