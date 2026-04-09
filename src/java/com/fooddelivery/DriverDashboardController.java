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
import javafx.collections.ObservableList;
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

    // Active delivery card fields
    @FXML private VBox  noActiveBox;
    @FXML private VBox  activeDeliveryCard;
    @FXML private Label activeOrderBadge;
    @FXML private Label activeAddressLabel;
    @FXML private Label activeRestaurantLabel;
    @FXML private Label activeItemsLabel;
    @FXML private Label activeTotalLabel;
    @FXML private Label activeStatus;

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
        loadDriverFeedback(null);
    }

    public void loadDriverFeedback(Integer highlightOrderID) {
        driverFeedbackListView.setCellFactory(
                lv -> new FeedbackCardCell(FeedbackCardCell.Mode.RECEIVED));
        try {
            List<FeedbackEntry> entries = feedbackDAO.getDriverFeedbackByDriver(me.getUserID());
            if (entries.isEmpty()) {
                driverFeedbackListView.setItems(FXCollections.observableArrayList());
                driverFeedbackCountLabel.setText("0 feedback(s)");
                driverFeedbackStatus.setText("No feedback received yet.");
                return;
            }
            // If a specific order is highlighted, filter to just that one
            List<FeedbackEntry> toShow = entries;
            if (highlightOrderID != null) {
                toShow = new ArrayList<>();
                for (FeedbackEntry e : entries)
                    if (e.getOrderItemID() == highlightOrderID) toShow.add(e);
                driverFeedbackStatus.setText(
                    "Showing feedback for Order #" + highlightOrderID
                    + " — click Feedback tab again to see all.");
            } else {
                driverFeedbackStatus.setText("");
            }
            driverFeedbackListView.setItems(FXCollections.observableArrayList(toShow));
            driverFeedbackCountLabel.setText(entries.size() + " feedback(s)");
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
            if (all.isEmpty()) {
                noActiveBox.setVisible(true);
                noActiveBox.setManaged(true);
                activeDeliveryCard.setVisible(false);
                activeDeliveryCard.setManaged(false);
            } else {
                Order o = all.get(0);
                noActiveBox.setVisible(false);
                noActiveBox.setManaged(false);
                activeDeliveryCard.setVisible(true);
                activeDeliveryCard.setManaged(true);
                activeOrderBadge.setText("Order #" + o.getOrderID());
                activeAddressLabel.setText(o.getDeliveryAddress());
                // Restaurant name
                try {
                    Restaurant rest = restaurantDAO.getById(o.getRestaurantID());
                    activeRestaurantLabel.setText(rest != null ? rest.getRestaurantName()
                            : "Restaurant #" + o.getRestaurantID());
                } catch (SQLException ex2) {
                    activeRestaurantLabel.setText("Restaurant #" + o.getRestaurantID());
                }
                // Items
                StringBuilder items = new StringBuilder();
                for (OrderItem item : o.getItems()) {
                    if (items.length() > 0) items.append("  •  ");
                    items.append(item.getItemName()).append(" x").append(item.getQuantity());
                }
                activeItemsLabel.setText(items.length() == 0 ? "No items" : items.toString());
                activeTotalLabel.setText("NPR " + String.format("%.2f", o.getTotalAmount()));
            }
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
        // No-op: active delivery is shown as a single card, no list selection needed
    }

    @FXML
    private void handleMarkDelivered(ActionEvent e) {
        if (activeDeliveries.isEmpty()) {
            activeStatus.setText("No active delivery.");
            return;
        }
        Order o = activeDeliveries.get(0);
        try {
            orderDAO.updateStatus(o.getOrderID(), OrderStatus.DELIVERED);
            me.completeDelivery();
            activeStatus.setText("✔ Delivered!");
            showAlert("Delivered!",
                "Order #" + o.getOrderID() + " marked as delivered.\nGreat work!");
            refreshAll();
            showPanel("history");
        } catch (SQLException ex) {
            activeStatus.setText("Error updating status.");
        }
    }

    @FXML
    private void handleCancelDelivery(ActionEvent e) {
        if (activeDeliveries.isEmpty()) {
            activeStatus.setText("No active delivery.");
            return;
        }
        Order o = activeDeliveries.get(0);
        try {
            orderDAO.updateStatus(o.getOrderID(), OrderStatus.CANCELLED);
            me.completeDelivery();
            activeStatus.setText("Order #" + o.getOrderID() + " cancelled.");
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