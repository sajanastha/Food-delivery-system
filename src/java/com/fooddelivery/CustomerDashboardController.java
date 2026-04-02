package com.fooddelivery;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TextField searchField;
    @FXML private ListView<String> restaurantListView;
    @FXML private Label restaurantInfoLabel;
    @FXML private ListView<String> menuListView;
    @FXML private Label browseStatus;
    @FXML private ListView<String> cartListView;
    @FXML private TextArea pricingArea;
    @FXML private TextField addressField;
    @FXML private Label cartStatus;
    @FXML private ListView<String> historyListView;

    // Panels for manual tab switching
    @FXML private VBox browsePanel;
    @FXML private VBox cartPanel;
    @FXML private VBox historyPanel;

    private final RestaurantDAO  restaurantDAO  = new RestaurantDAO();
    private final MenuItemDAO    menuItemDAO    = new MenuItemDAO();
    private final OrderDAO       orderDAO       = new OrderDAO();
    private final PricingService pricingService = new PricingService();

    private List<Restaurant> allRestaurants = new ArrayList<>();
    private List<MenuItem>   currentMenu    = new ArrayList<>();
    private List<OrderItem>  cart           = new ArrayList<>();
    private Restaurant       selectedRest;
    private Customer         me;

    @FXML
    public void initialize() {
        me = (Customer) SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Hello, " + me.getFullName());
        loadRestaurants();
        loadHistory();
        showBrowse(null);
    }

    // ── Panel switching ───────────────────────────────────────────────────────
    @FXML
    public void showBrowse(ActionEvent e) {
        browsePanel.setVisible(true);
        cartPanel.setVisible(false);
        historyPanel.setVisible(false);
    }

    @FXML
    public void showCart(ActionEvent e) {
        browsePanel.setVisible(false);
        cartPanel.setVisible(true);
        historyPanel.setVisible(false);
        refreshCart();
    }

    @FXML
    public void showHistory(ActionEvent e) {
        browsePanel.setVisible(false);
        cartPanel.setVisible(false);
        historyPanel.setVisible(true);
        loadHistory();
    }

    // ── Browse ────────────────────────────────────────────────────────────────
    private void loadRestaurants() {
        try {
            allRestaurants = restaurantDAO.getAll();
            List<String> display = new ArrayList<>();
            for (Restaurant r : allRestaurants)
                display.add(r.getRestaurantName()
                        + "  —  " + r.getCuisineType()
                        + "  ·  ⭐ "
                        + String.format("%.1f", r.getRating()));
            restaurantListView.setItems(
                FXCollections.observableArrayList(display));
            browseStatus.setText(allRestaurants.size()
                    + " restaurants available");
        } catch (SQLException ex) {
            browseStatus.setText("Could not load restaurants.");
        }
    }

    @FXML
    private void handleSearch(ActionEvent e) {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { loadRestaurants(); return; }
        try {
            allRestaurants = restaurantDAO.searchByName(kw);
            List<String> display = new ArrayList<>();
            for (Restaurant r : allRestaurants)
                display.add(r.getRestaurantName()
                        + "  —  " + r.getCuisineType()
                        + "  ·  ⭐ "
                        + String.format("%.1f", r.getRating()));
            restaurantListView.setItems(
                FXCollections.observableArrayList(display));
            browseStatus.setText("Found " + allRestaurants.size()
                    + " result(s)");
        } catch (SQLException ex) {
            browseStatus.setText("Search failed.");
        }
    }

    @FXML
    private void handleShowAll(ActionEvent e) {
        searchField.clear();
        loadRestaurants();
    }

    @FXML
    private void handleRestaurantSelected() {
        int idx = restaurantListView.getSelectionModel()
                                    .getSelectedIndex();
        if (idx < 0 || idx >= allRestaurants.size()) return;
        selectedRest = allRestaurants.get(idx);
        restaurantInfoLabel.setText(
            selectedRest.getRestaurantName()
            + "  ·  " + selectedRest.getAddress()
            + "  ·  Hours: " + selectedRest.getOperatingHours());
        try {
            currentMenu = menuItemDAO.getAvailable(
                    selectedRest.getRestaurantID());
            List<String> display = new ArrayList<>();
            for (MenuItem m : currentMenu)
                display.add(m.getName()
                        + "  ·  " + m.getCategory()
                        + "  —  NPR "
                        + String.format("%.0f", m.getPrice()));
            menuListView.setItems(
                FXCollections.observableArrayList(display));
        } catch (SQLException ex) {
            browseStatus.setText("Could not load menu.");
        }
    }

    // ── Cart ──────────────────────────────────────────────────────────────────
    @FXML
    private void handleAddToCart(ActionEvent e) {
        int idx = menuListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= currentMenu.size()) {
            browseStatus.setText("Select a menu item first.");
            return;
        }
        MenuItem m = currentMenu.get(idx);
        for (OrderItem existing : cart) {
            if (existing.getMenuItemID() == m.getItemID()) {
                existing.setQuantity(existing.getQuantity() + 1);
                browseStatus.setText(m.getName()
                        + " quantity updated.");
                return;
            }
        }
        cart.add(new OrderItem(
            m.getItemID(), m.getName(), m.getPrice(), 1));
        browseStatus.setText(m.getName() + " added to cart.");
    }

    private void refreshCart() {
        List<String> display = new ArrayList<>();
        for (OrderItem i : cart)
            display.add(i.getSummary());
        cartListView.setItems(
            FXCollections.observableArrayList(display));
        pricingArea.setText(pricingService.breakdown(cart));
    }

    @FXML
    private void handleRemoveFromCart(ActionEvent e) {
        int idx = cartListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= cart.size()) return;
        cart.remove(idx);
        refreshCart();
        cartStatus.setText("Item removed.");
    }

    @FXML
    private void handlePlaceOrder(ActionEvent e) {
        if (cart.isEmpty()) {
            cartStatus.setText("Your cart is empty.");
            return;
        }
        String address = addressField.getText().trim();
        if (address.isEmpty()) {
            cartStatus.setText(
                "Please enter a delivery address.");
            return;
        }
        if (selectedRest == null) {
            cartStatus.setText("No restaurant selected.");
            return;
        }
        try {
            Order order = new Order(
                me.getUserID(),
                selectedRest.getRestaurantID(),
                address,
                pricingService.getDeliveryFee());
            order.setItems(new ArrayList<>(cart));
            int newID = orderDAO.createOrder(order);
            if (newID > 0) {
                showAlert("Order Placed",
                    "Order #" + newID + " placed!\n"
                    + "Total: NPR "
                    + String.format("%.2f",
                        pricingService.orderTotal(cart)));
                cart.clear();
                refreshCart();
                addressField.clear();
                cartStatus.setText("Order #" + newID
                        + " — Status: PENDING");
                loadHistory();
            }
        } catch (SQLException ex) {
            cartStatus.setText("Failed to place order.");
            ex.printStackTrace();
        }
    }

    // ── History ───────────────────────────────────────────────────────────────
    @FXML
    private void handleRefreshHistory(ActionEvent e) {
        loadHistory();
    }

    private void loadHistory() {
        try {
            List<Order> orders = orderDAO
                    .getByCustomer(me.getUserID());
            List<String> display = new ArrayList<>();
            for (Order o : orders)
                display.add("Order #" + o.getOrderID()
                        + "   " + o.getStatus()
                        + "   NPR "
                        + String.format("%.2f",
                            o.getTotalAmount())
                        + "   → " + o.getDeliveryAddress());
            historyListView.setItems(
                FXCollections.observableArrayList(display));
        } catch (SQLException ex) {
            historyListView.setItems(
                FXCollections.observableArrayList(
                    "Could not load order history."));
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @FXML
    private void handleLogout(ActionEvent e) {
        SessionManager.getInstance().logout();
        loadScreen(e, "/com/fooddelivery/views/Login.fxml",
                "Login");
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