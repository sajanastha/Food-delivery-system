package com.fooddelivery; 

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


public class CustomerDashboardController {

    @FXML private Label            welcomeLabel;
    @FXML private TextField        searchField;
    @FXML private ListView<String> restaurantListView;
    @FXML private Label            restaurantInfoLabel;
    @FXML private ListView<String> menuListView;
    @FXML private Label            browseStatus;
    @FXML private ListView<String> cartListView;
    @FXML private TextArea         pricingArea;
    @FXML private TextField        addressField;
    @FXML private Label            cartStatus;
    @FXML private ListView<String> historyListView;

    private final RestaurantDAO  restaurantDAO  = new RestaurantDAO();
    private final MenuItemDAO    menuItemDAO    = new MenuItemDAO();
    private final OrderDAO       orderDAO       = new OrderDAO();
    private final PricingService pricingService = new PricingService();

    private List<Restaurant> allRestaurants  = new ArrayList<>();
    private List<MenuItem>   currentMenu     = new ArrayList<>();
    private List<OrderItem>  cart            = new ArrayList<>();
    private Restaurant       selectedRest;
    private Customer         me;

    // Runs when screen loads
    @FXML
    public void initialize() {
        me = (Customer) SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Welcome, " + me.getFullName() + "!");
        loadRestaurants();
        loadHistory();
    }

    // Browse
    private void loadRestaurants() {
        try {
            allRestaurants = restaurantDAO.getAll();
            List<String> display = new ArrayList<>();
            for (Restaurant r : allRestaurants)
                display.add("🍴  " + r.getRestaurantName()
                        + "   |   " + r.getCuisineType()
                        + "   |   ⭐ " + String.format("%.1f", r.getRating()));
            restaurantListView.setItems(
                FXCollections.observableArrayList(display)
            );
            browseStatus.setText(allRestaurants.size() + " restaurants available");
        } catch (SQLException e) {
            browseStatus.setText("Could not load restaurants.");
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) { loadRestaurants(); return; }
        try {
            List<Restaurant> found = restaurantDAO.searchByName(kw);
            allRestaurants = found;
            List<String> display = new ArrayList<>();
            for (Restaurant r : found)
                display.add("🍴  " + r.getRestaurantName()
                        + "   |   " + r.getCuisineType()
                        + "   |   ⭐ " + String.format("%.1f", r.getRating()));
            restaurantListView.setItems(
                FXCollections.observableArrayList(display)
            );
            browseStatus.setText("Found " + found.size()
                    + " result(s) for \"" + kw + "\"");
        } catch (SQLException e) {
            browseStatus.setText("Search failed.");
        }
    }

    @FXML
    private void handleShowAll(ActionEvent event) {
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
            + "  |  " + selectedRest.getCuisineType()
            + "  |  " + selectedRest.getAddress()
            + "  |  Hours: " + selectedRest.getOperatingHours()
        );

        try {
            currentMenu = menuItemDAO.getAvailable(
                selectedRest.getRestaurantID()
            );
            List<String> display = new ArrayList<>();
            for (MenuItem m : currentMenu)
                display.add("[" + m.getCategory() + "]  "
                        + m.getName() + "   —   "
                        + m.getFormattedPrice());
            menuListView.setItems(
                FXCollections.observableArrayList(display)
            );
        } catch (SQLException e) {
            browseStatus.setText("Could not load menu.");
        }
    }

    //  Cart 
    @FXML
    private void handleAddToCart(ActionEvent event) {
        int idx = menuListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= currentMenu.size()) {
            browseStatus.setText("Please select a menu item first.");
            return;
        }
        MenuItem m = currentMenu.get(idx);
        for (OrderItem existing : cart) {
            if (existing.getMenuItemID() == m.getItemID()) {
                existing.setQuantity(existing.getQuantity() + 1);
                refreshCart();
                browseStatus.setText(m.getName() + " quantity updated.");
                return;
            }
        }
        cart.add(new OrderItem(
            m.getItemID(), m.getName(), m.getPrice(), 1
        ));
        refreshCart();
        browseStatus.setText("✓ " + m.getName() + " added to cart.");
    }

    private void refreshCart() {
        List<String> display = new ArrayList<>();
        for (OrderItem i : cart)
            display.add(i.getSummary());
        cartListView.setItems(
            FXCollections.observableArrayList(display)
        );
        pricingArea.setText(pricingService.breakdown(cart));
    }

    @FXML
    private void handleRemoveFromCart(ActionEvent event) {
        int idx = cartListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= cart.size()) return;
        cart.remove(idx);
        refreshCart();
        cartStatus.setText("Item removed from cart.");
    }

    @FXML
    private void handlePlaceOrder(ActionEvent event) {
        if (cart.isEmpty()) {
            cartStatus.setText("Your cart is empty. Add items first.");
            return;
        }
        String address = addressField.getText().trim();
        if (address.isEmpty()) {
            cartStatus.setText("Please enter your delivery address.");
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
                pricingService.getDeliveryFee()
            );
            order.setItems(new ArrayList<>(cart));
            int newID = orderDAO.createOrder(order);
            if (newID > 0) {
                showPopup("Order Placed!",
                    "Order #" + newID + " placed successfully!\n"
                    + "The restaurant will confirm it shortly.\n"
                    + "Total: NPR "
                    + String.format("%.2f",
                        pricingService.orderTotal(cart)));
                cart.clear();
                refreshCart();
                addressField.clear();
                cartStatus.setText("✓ Order #" + newID
                        + " placed — Status: PENDING");
                loadHistory();
            }
        } catch (SQLException e) {
            cartStatus.setText("Failed to place order. Try again.");
            e.printStackTrace();
        }
    }

    //History 
    @FXML
    private void handleRefreshHistory(ActionEvent event) {
        loadHistory();
    }

    private void loadHistory() {
        try {
            List<Order> orders = orderDAO.getByCustomer(
                me.getUserID()
            );
            List<String> display = new ArrayList<>();
            for (Order o : orders)
                display.add("Order #" + o.getOrderID()
                        + "  |  " + o.getStatus()
                        + "  |  NPR "
                        + String.format("%.2f", o.getTotalAmount())
                        + "  |  " + o.getDeliveryAddress());
            historyListView.setItems(
                FXCollections.observableArrayList(display)
            );
        } catch (SQLException e) {
            historyListView.setItems(
                FXCollections.observableArrayList(
                    "Could not load order history."
                )
            );
        }
    }

    // Logout 
    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        loadScreen(event,
                "/com/fooddelivery/views/Login.fxml",
                "Login");
    }

    private void showPopup(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
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
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
