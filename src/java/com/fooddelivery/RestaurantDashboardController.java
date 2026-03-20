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

public class RestaurantDashboardController {

    @FXML private Label            welcomeLabel;
    @FXML private ListView<String> menuListView;
    @FXML private TextField        itemNameField;
    @FXML private TextField        itemPriceField;
    @FXML private TextField        itemCategoryField;
    @FXML private TextArea         itemDescField;
    @FXML private Label            menuStatus;
    @FXML private ListView<String> orderListView;
    @FXML private TextArea         orderDetailArea;
    @FXML private Label            orderStatus;
    @FXML private TextArea         reportArea;

    private final MenuItemDAO   menuItemDAO   = new MenuItemDAO();
    private final OrderDAO      orderDAO      = new OrderDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final ReportService reportService = new ReportService();

    private Restaurant         myRestaurant;
    private List<MenuItem>     menuItems = new ArrayList<>();
    private List<Order>        orders    = new ArrayList<>();

    @FXML
    public void initialize() {
        Restaurant owner =
            (Restaurant) SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText(
            "Restaurant Portal — " + owner.getFullName()
        );
        try {
            myRestaurant = restaurantDAO.getByOwner(owner.getUserID());
            if (myRestaurant != null) {
                loadMenu();
                loadOrders();
            } else {
                menuStatus.setText(
                    "No restaurant linked to this account. Contact admin."
                );
            }
        } catch (SQLException e) {
            menuStatus.setText("Error loading data.");
        }
    }

    // Menu 
    private void loadMenu() throws SQLException {
        menuItems = menuItemDAO.getByRestaurant(
            myRestaurant.getRestaurantID()
        );
        List<String> display = new ArrayList<>();
        for (MenuItem m : menuItems)
            display.add("[" + m.getCategory() + "]  "
                    + m.getName()
                    + "  —  NPR " + String.format("%.2f", m.getPrice())
                    + (m.isAvailable() ? "  ✓" : "  ✗ unavailable"));
        menuListView.setItems(
            FXCollections.observableArrayList(display)
        );
    }

    @FXML
    private void handleMenuSelected() {
        int idx = menuListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= menuItems.size()) return;
        MenuItem m = menuItems.get(idx);
        itemNameField.setText(m.getName());
        itemPriceField.setText(String.valueOf(m.getPrice()));
        itemCategoryField.setText(m.getCategory());
        itemDescField.setText(m.getDescription());
    }

    @FXML
    private void handleAddItem(ActionEvent event) {
        try {
            MenuItem m = buildItemFromForm(0);
            menuItemDAO.addItem(m);
            menuStatus.setText("✓ Item added.");
            clearMenuForm();
            loadMenu();
        } catch (Exception e) {
            menuStatus.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateItem(ActionEvent event) {
        int idx = menuListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            menuStatus.setText("Select an item to update.");
            return;
        }
        try {
            MenuItem m = buildItemFromForm(
                menuItems.get(idx).getItemID()
            );
            menuItemDAO.updateItem(m);
            menuStatus.setText("✓ Item updated.");
            loadMenu();
        } catch (Exception e) {
            menuStatus.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteItem(ActionEvent event) {
        int idx = menuListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            menuStatus.setText("Select an item to delete.");
            return;
        }
        try {
            menuItemDAO.deleteItem(menuItems.get(idx).getItemID());
            menuStatus.setText("✓ Item deleted.");
            clearMenuForm();
            loadMenu();
        } catch (SQLException e) {
            menuStatus.setText("Error deleting item.");
        }
    }

    @FXML
    private void handleToggle(ActionEvent event) {
        int idx = menuListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            menuStatus.setText("Select an item first.");
            return;
        }
        try {
            MenuItem m = menuItems.get(idx);
            menuItemDAO.toggleAvailability(
                m.getItemID(), !m.isAvailable()
            );
            menuStatus.setText("✓ Availability updated.");
            loadMenu();
        } catch (SQLException e) {
            menuStatus.setText("Error updating availability.");
        }
    }

    private MenuItem buildItemFromForm(int existingID) {
        String name  = itemNameField.getText().trim();
        String price = itemPriceField.getText().trim();
        String cat   = itemCategoryField.getText().trim();
        String desc  = itemDescField.getText().trim();
        if (name.isEmpty())
            throw new IllegalArgumentException("Item name is required.");
        double p = Double.parseDouble(price);
        return new MenuItem(
            existingID,
            myRestaurant.getRestaurantID(),
            name, desc, p, cat
        );
    }

    private void clearMenuForm() {
        itemNameField.clear();
        itemPriceField.clear();
        itemCategoryField.clear();
        itemDescField.clear();
    }

    //  Orders 
    private void loadOrders() throws SQLException {
        orders = orderDAO.getByRestaurant(
            myRestaurant.getRestaurantID()
        );
        List<String> display = new ArrayList<>();
        for (Order o : orders)
            display.add("Order #" + o.getOrderID()
                    + "  |  " + o.getStatus()
                    + "  |  NPR "
                    + String.format("%.2f", o.getTotalAmount()));
        orderListView.setItems(
            FXCollections.observableArrayList(display)
        );
    }

    @FXML
    private void handleOrderSelected() {
        int idx = orderListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= orders.size()) return;
        orderDetailArea.setText(orders.get(idx).getOrderSummary());
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        updateStatus(OrderStatus.CONFIRMED, "Order confirmed!");
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        updateStatus(OrderStatus.CANCELLED, "Order cancelled.");
    }

    @FXML
    private void handleRefreshOrders(ActionEvent event) {
        try { loadOrders(); orderStatus.setText("Refreshed."); }
        catch (SQLException e) { orderStatus.setText("Error refreshing."); }
    }

    private void updateStatus(OrderStatus s, String msg) {
        int idx = orderListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            orderStatus.setText("Select an order first.");
            return;
        }
        try {
            orderDAO.updateStatus(orders.get(idx).getOrderID(), s);
            orderStatus.setText("✓ " + msg);
            loadOrders();
        } catch (SQLException e) {
            orderStatus.setText("Error updating order.");
        }
    }

    // Reports
    @FXML
    private void handleWeekly(ActionEvent event) {
        try {
            reportArea.setText(
                reportService.weeklyReport(
                    myRestaurant.getRestaurantID()
                ).getSummary()
            );
        } catch (SQLException e) {
            reportArea.setText("Error generating report.");
        }
    }

    @FXML
    private void handleMonthly(ActionEvent event) {
        try {
            reportArea.setText(
                reportService.monthlyReport(
                    myRestaurant.getRestaurantID()
                ).getSummary()
            );
        } catch (SQLException e) {
            reportArea.setText("Error generating report.");
        }
    }

    // Logout 
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