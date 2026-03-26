package com.fooddelivery;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> menuListView;
    @FXML private TextField itemNameField;
    @FXML private TextField itemPriceField;
    @FXML private TextField itemCategoryField;
    @FXML private TextArea itemDescField;
    @FXML private Label menuStatus;

    @FXML private ListView<String> orderListView;
    @FXML private TextArea orderDetailArea;
    @FXML private Label orderStatus;

    @FXML private TextArea reportArea;

    // 🔥 NEW (for bottom navigation)
    @FXML private TabPane tabPane;

    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final ReportService reportService = new ReportService();

    private Restaurant myRestaurant;
    private List<MenuItem> menuItems = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();

    @FXML
    public void initialize() {
        Restaurant owner =
                (Restaurant) SessionManager.getInstance().getCurrentUser();

        welcomeLabel.setText("Restaurant Portal — " + owner.getFullName());

        try {
            myRestaurant = restaurantDAO.getByOwner(owner.getUserID());

            if (myRestaurant != null) {
                loadMenu();
                loadOrders();
            } else {
                menuStatus.setText("No restaurant linked to this account.");
            }
        } catch (SQLException e) {
            menuStatus.setText("Error loading data.");
        }
    }

    // ================= MENU =================

    private void loadMenu() throws SQLException {
        menuItems = menuItemDAO.getByRestaurant(
                myRestaurant.getRestaurantID()
        );

        List<String> display = new ArrayList<>();

        for (MenuItem m : menuItems) {
            display.add("[" + m.getCategory() + "] "
                    + m.getName()
                    + " — NPR " + String.format("%.2f", m.getPrice())
                    + (m.isAvailable() ? " ✓" : " ✗"));
        }

        menuListView.setItems(FXCollections.observableArrayList(display));
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
            menuStatus.setText("Select an item first.");
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
            menuStatus.setText("Select an item first.");
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
            menuStatus.setText("Error updating.");
        }
    }

    private MenuItem buildItemFromForm(int id) {
        String name = itemNameField.getText().trim();
        String price = itemPriceField.getText().trim();
        String cat = itemCategoryField.getText().trim();
        String desc = itemDescField.getText().trim();

        if (name.isEmpty())
            throw new IllegalArgumentException("Name required");

        double p = Double.parseDouble(price);

        return new MenuItem(
                id,
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

    // ================= ORDERS =================

    private void loadOrders() throws SQLException {
        orders = orderDAO.getByRestaurant(
                myRestaurant.getRestaurantID()
        );

        List<String> display = new ArrayList<>();

        for (Order o : orders) {
            display.add("Order #" + o.getOrderID()
                    + " | " + o.getStatus()
                    + " | NPR " + String.format("%.2f", o.getTotalAmount()));
        }

        orderListView.setItems(FXCollections.observableArrayList(display));
    }

    @FXML
    private void handleOrderSelected() {
        int idx = orderListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= orders.size()) return;

        orderDetailArea.setText(orders.get(idx).getOrderSummary());
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        updateStatus(OrderStatus.CONFIRMED, "Confirmed");
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        updateStatus(OrderStatus.CANCELLED, "Cancelled");
    }

    @FXML
    private void handleRefreshOrders(ActionEvent event) {
        try {
            loadOrders();
            orderStatus.setText("Refreshed.");
        } catch (SQLException e) {
            orderStatus.setText("Error.");
        }
    }

    private void updateStatus(OrderStatus s, String msg) {
        int idx = orderListView.getSelectionModel().getSelectedIndex();

        if (idx < 0) {
            orderStatus.setText("Select order first.");
            return;
        }

        try {
            orderDAO.updateStatus(
                    orders.get(idx).getOrderID(), s
            );

            orderStatus.setText("✓ " + msg);
            loadOrders();

        } catch (SQLException e) {
            orderStatus.setText("Error updating.");
        }
    }

    // ================= REPORTS =================

    @FXML
    private void handleWeekly(ActionEvent event) {
        try {
            reportArea.setText(
                    reportService.weeklyReport(
                            myRestaurant.getRestaurantID()
                    ).getSummary()
            );
        } catch (SQLException e) {
            reportArea.setText("Error.");
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
            reportArea.setText("Error.");
        }
    }

    // ================= NAVIGATION (NEW) =================

    @FXML
    private void goHome() {
        tabPane.getSelectionModel().select(0);
    }

    @FXML
    private void goOrders() {
        tabPane.getSelectionModel().select(1);
    }

    @FXML
    private void goReports() {
        tabPane.getSelectionModel().select(2);
    }

    @FXML
    private void goBookings() {
        menuStatus.setText("Bookings coming soon.");
    }

    @FXML
    private void goProfile() {
        menuStatus.setText("Profile coming soon.");
    }

    // ================= HOVER EFFECT (NEW) =================

    @FXML
    private void hoverIn(MouseEvent event) {
        Button btn = (Button) event.getSource();

        btn.setStyle(
                "-fx-background-color: #F5F5DC;" +
                "-fx-text-fill: #FF7518;" +
                "-fx-background-radius: 8;" +
                "-fx-scale-x: 1.1;" +
                "-fx-scale-y: 1.1;" +
                "-fx-cursor: hand;"
        );
    }

    @FXML
    private void hoverOut(MouseEvent event) {
        Button btn = (Button) event.getSource();

        btn.setStyle(
                "-fx-background-color: #FF7518;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
        );
    }

    // ================= LOGOUT =================

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        loadScreen(event,
                "/com/fooddelivery/views/Login.fxml",
                "Login");
    }

    private void loadScreen(ActionEvent event,
                            String path, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));

            Stage stage = (Stage)((Node) event.getSource())
                    .getScene().getWindow();

            stage.setTitle("Food Delivery — " + title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}