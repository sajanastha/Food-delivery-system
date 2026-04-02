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
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label menuStatus;

    // Menu panel
    @FXML private ListView<String> menuListView;
    @FXML private TextField itemNameField;
    @FXML private TextField itemPriceField;
    @FXML private TextField itemCategoryField;
    @FXML private TextArea  itemDescField;

    // Orders panel
    @FXML private ListView<String> orderListView;
    @FXML private TextArea orderDetailArea;
    @FXML private Label orderStatus;

    // Reports panel
    @FXML private TextArea reportArea;

    // Feedback panel
    @FXML private ListView<String> feedbackListView;

    // Content panels (for manual switching)
    @FXML private VBox menuPanel;
    @FXML private VBox ordersPanel;
    @FXML private VBox reportsPanel;
    @FXML private VBox feedbackPanel;

    private final MenuItemDAO   menuItemDAO   = new MenuItemDAO();
    private final OrderDAO      orderDAO      = new OrderDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final ReportService reportService = new ReportService();

    private Restaurant      myRestaurant;
    private List<MenuItem>  menuItems = new ArrayList<>();
    private List<Order>     orders    = new ArrayList<>();

    @FXML
    public void initialize() {
        Restaurant owner = (Restaurant) SessionManager
                .getInstance().getCurrentUser();
        welcomeLabel.setText(owner.getFullName());
        try {
            myRestaurant = restaurantDAO
                    .getByOwner(owner.getUserID());
            if (myRestaurant != null) {
                loadMenu();
                loadOrders();
            } else {
                menuStatus.setText(
                    "No restaurant linked to this account.");
            }
        } catch (SQLException e) {
            menuStatus.setText("Error loading data.");
        }
        showPanel("menu");
    }

    // ── Panel switching ───────────────────────────────────────────
    private void showPanel(String name) {
        menuPanel.setVisible(false);
        ordersPanel.setVisible(false);
        reportsPanel.setVisible(false);
        feedbackPanel.setVisible(false);
        switch (name) {
            case "menu"     -> menuPanel.setVisible(true);
            case "orders"   -> ordersPanel.setVisible(true);
            case "reports"  -> reportsPanel.setVisible(true);
            case "feedback" -> feedbackPanel.setVisible(true);
        }
    }

    @FXML private void goMenu(ActionEvent e)     { showPanel("menu"); }
    @FXML private void goOrders(ActionEvent e)   { showPanel("orders"); loadOrdersSafe(); }
    @FXML private void goReports(ActionEvent e)  { showPanel("reports"); }
    @FXML private void goFeedback(ActionEvent e) { showPanel("feedback"); }

    // ── Menu ─────────────────────────────────────────────────────
    private void loadMenu() throws SQLException {
        menuItems = menuItemDAO.getByRestaurant(
                myRestaurant.getRestaurantID());
        List<String> display = new ArrayList<>();
        for (MenuItem m : menuItems)
            display.add("[" + m.getCategory() + "]  "
                    + m.getName()
                    + "   NPR " + String.format("%.2f", m.getPrice())
                    + (m.isAvailable() ? "  ✓" : "  ✗ unavailable"));
        menuListView.setItems(
            FXCollections.observableArrayList(display));
    }

    @FXML
    private void handleMenuSelected() {
        int idx = menuListView.getSelectionModel()
                              .getSelectedIndex();
        if (idx < 0 || idx >= menuItems.size()) return;
        MenuItem m = menuItems.get(idx);
        itemNameField.setText(m.getName());
        itemPriceField.setText(String.valueOf(m.getPrice()));
        itemCategoryField.setText(m.getCategory());
        itemDescField.setText(m.getDescription());
    }

    @FXML
    private void showAddItemDialog(ActionEvent e) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Menu Item");
        dialog.setHeaderText(null);

        ButtonType addBtn = new ButtonType("Add Item",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
              .addAll(addBtn, ButtonType.CANCEL);

        VBox form = new VBox(8);
        form.setStyle("-fx-padding: 16;");

        TextField nameF  = new TextField();
        nameF.setPromptText("Item name  e.g. Chicken Momo");
        TextField priceF = new TextField();
        priceF.setPromptText("Price  e.g. 250");
        TextField catF   = new TextField();
        catF.setPromptText("Category  e.g. Main, Starter, Drink");
        TextArea  descF  = new TextArea();
        descF.setPromptText("Description (optional)");
        descF.setPrefRowCount(3);

        form.getChildren().addAll(
            new Label("Name:"), nameF,
            new Label("Price (NPR):"), priceF,
            new Label("Category:"), catF,
            new Label("Description:"), descF
        );
        dialog.getDialogPane().setContent(form);

        dialog.showAndWait().ifPresent(result -> {
            if (result == addBtn) {
                try {
                    String name  = nameF.getText().trim();
                    String price = priceF.getText().trim();
                    String cat   = catF.getText().trim();
                    String desc  = descF.getText().trim();
                    if (name.isEmpty() || price.isEmpty()) {
                        menuStatus.setText(
                            "Name and price are required.");
                        return;
                    }
                    double p = Double.parseDouble(price);
                    MenuItem m = new MenuItem(0,
                        myRestaurant.getRestaurantID(),
                        name, desc, p, cat);
                    menuItemDAO.addItem(m);
                    menuStatus.setText("Item added: " + name);
                    loadMenu();
                } catch (NumberFormatException ex) {
                    menuStatus.setText(
                        "Invalid price. Enter a number.");
                } catch (SQLException ex) {
                    menuStatus.setText("Error saving item.");
                }
            }
        });
    }

    @FXML
    private void handleUpdateItem(ActionEvent e) {
        int idx = menuListView.getSelectionModel()
                              .getSelectedIndex();
        if (idx < 0) {
            menuStatus.setText("Select an item to update.");
            return;
        }
        try {
            MenuItem m = buildItemFromForm(
                    menuItems.get(idx).getItemID());
            menuItemDAO.updateItem(m);
            menuStatus.setText("Item updated.");
            loadMenu();
        } catch (Exception ex) {
            menuStatus.setText("Error: " + ex.getMessage());
        }
    }

    @FXML
    private void handleDeleteItem(ActionEvent e) {
        int idx = menuListView.getSelectionModel()
                              .getSelectedIndex();
        if (idx < 0) {
            menuStatus.setText("Select an item to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Item");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete \""
            + menuItems.get(idx).getName() + "\"?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    menuItemDAO.deleteItem(
                        menuItems.get(idx).getItemID());
                    menuStatus.setText("Item deleted.");
                    clearMenuForm();
                    loadMenu();
                } catch (SQLException ex) {
                    menuStatus.setText("Error deleting.");
                }
            }
        });
    }

    @FXML
    private void handleToggle(ActionEvent e) {
        int idx = menuListView.getSelectionModel()
                              .getSelectedIndex();
        if (idx < 0) {
            menuStatus.setText("Select an item first.");
            return;
        }
        try {
            MenuItem m = menuItems.get(idx);
            menuItemDAO.toggleAvailability(
                    m.getItemID(), !m.isAvailable());
            menuStatus.setText("Availability updated.");
            loadMenu();
        } catch (SQLException ex) {
            menuStatus.setText("Error updating.");
        }
    }

    private MenuItem buildItemFromForm(int id) {
        String name  = itemNameField.getText().trim();
        String price = itemPriceField.getText().trim();
        String cat   = itemCategoryField.getText().trim();
        String desc  = itemDescField.getText().trim();
        if (name.isEmpty())
            throw new IllegalArgumentException("Name required.");
        double p = Double.parseDouble(price);
        return new MenuItem(id,
            myRestaurant.getRestaurantID(),
            name, desc, p, cat);
    }

    private void clearMenuForm() {
        itemNameField.clear();
        itemPriceField.clear();
        itemCategoryField.clear();
        itemDescField.clear();
    }

    // ── Orders ───────────────────────────────────────────────────
    private void loadOrders() throws SQLException {
        orders = orderDAO.getByRestaurant(
                myRestaurant.getRestaurantID());
        List<String> display = new ArrayList<>();
        for (Order o : orders)
            display.add("Order #" + o.getOrderID()
                    + "   " + o.getStatus()
                    + "   NPR "
                    + String.format("%.2f", o.getTotalAmount()));
        orderListView.setItems(
            FXCollections.observableArrayList(display));
    }

    private void loadOrdersSafe() {
        try { loadOrders(); }
        catch (SQLException e) {
            orderStatus.setText("Error loading orders.");
        }
    }

    @FXML
    private void handleOrderSelected() {
        int idx = orderListView.getSelectionModel()
                               .getSelectedIndex();
        if (idx < 0 || idx >= orders.size()) return;
        orderDetailArea.setText(
            orders.get(idx).getOrderSummary());
    }

    @FXML
    private void handleConfirm(ActionEvent e) {
        updateOrderStatus(OrderStatus.CONFIRMED,
            "Order confirmed.");
    }

    @FXML
    private void handleCancel(ActionEvent e) {
        updateOrderStatus(OrderStatus.CANCELLED,
            "Order cancelled.");
    }

    @FXML
    private void handleRefreshOrders(ActionEvent e) {
        loadOrdersSafe();
        orderStatus.setText("Refreshed.");
    }

    private void updateOrderStatus(OrderStatus s, String msg) {
        int idx = orderListView.getSelectionModel()
                               .getSelectedIndex();
        if (idx < 0) {
            orderStatus.setText("Select an order first.");
            return;
        }
        try {
            orderDAO.updateStatus(
                orders.get(idx).getOrderID(), s);
            orderStatus.setText(msg);
            loadOrders();
        } catch (SQLException e) {
            orderStatus.setText("Error updating status.");
        }
    }

    // ── Reports ──────────────────────────────────────────────────
    @FXML
    private void handleWeekly(ActionEvent e) {
        if (myRestaurant == null) return;
        try {
            SalesReport report = reportService.weeklyReport(
                    myRestaurant.getRestaurantID());

            if (report.getTotalOrders() == 0) {
                reportArea.setText(
                    "WEEKLY REPORT\n"
                    + "─────────────────────────────────\n\n"
                    + "No completed orders in the last 7 days.\n\n"
                    + "Keep accepting orders and the data\n"
                    + "will populate here automatically.");
            } else {
                reportArea.setText(buildReportText(
                    report, "WEEKLY", 7));
            }
        } catch (SQLException ex) {
            reportArea.setText("Error generating report.");
        }
    }

    @FXML
    private void handleMonthly(ActionEvent e) {
        if (myRestaurant == null) return;
        try {
            SalesReport report = reportService.monthlyReport(
                    myRestaurant.getRestaurantID());

            if (report.getTotalOrders() == 0) {
                reportArea.setText(
                    "MONTHLY REPORT\n"
                    + "─────────────────────────────────\n\n"
                    + "No completed orders in the last 30 days.\n\n"
                    + "Keep accepting orders and the data\n"
                    + "will populate here automatically.");
            } else {
                reportArea.setText(buildReportText(
                    report, "MONTHLY", 30));
            }
        } catch (SQLException ex) {
            reportArea.setText("Error generating report.");
        }
    }

    private String buildReportText(SalesReport r,
            String type, int days) {
        LocalDate to   = LocalDate.now();
        LocalDate from = to.minusDays(days);
        return type + " SALES REPORT\n"
            + "─────────────────────────────────\n"
            + "Period:          " + from + "  to  " + to + "\n"
            + "Restaurant:      "
            + myRestaurant.getRestaurantName() + "\n"
            + "─────────────────────────────────\n"
            + "Total Orders:    " + r.getTotalOrders() + "\n"
            + "Total Revenue:   NPR "
            + String.format("%.2f", r.getTotalRevenue()) + "\n"
            + "Popular Item:    " + r.getTopSellingItem() + "\n"
            + "─────────────────────────────────\n"
            + "Avg per Order:   NPR "
            + String.format("%.2f",
                r.getTotalOrders() > 0
                    ? r.getTotalRevenue() / r.getTotalOrders()
                    : 0) + "\n";
    }

    // ── Feedback ─────────────────────────────────────────────────
    @FXML
    private void handleLoadFeedback(ActionEvent e) {
        if (myRestaurant == null) return;
        try {
            Connection conn = DatabaseManager
                    .getInstance().getConnection();
            String sql = "SELECT f.rating, f.comment, "
                + "u.fullName, f.createdAt "
                + "FROM feedbacks f "
                + "JOIN users u ON f.customerID=u.userID "
                + "WHERE f.restaurantID=? "
                + "ORDER BY f.createdAt DESC";
            List<String> display = new ArrayList<>();
            try (PreparedStatement ps =
                    conn.prepareStatement(sql)) {
                ps.setInt(1,
                    myRestaurant.getRestaurantID());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String stars = "★".repeat(
                        rs.getInt("rating"))
                        + "☆".repeat(
                        5 - rs.getInt("rating"));
                    display.add(stars
                        + "   " + rs.getString("fullName")
                        + "   —   " + rs.getString("comment"));
                }
            }
            if (display.isEmpty())
                display.add(
                    "No feedback received yet. "
                    + "Feedback will appear here after "
                    + "customers rate their orders.");
            feedbackListView.setItems(
                FXCollections.observableArrayList(display));
        } catch (SQLException ex) {
            feedbackListView.setItems(
                FXCollections.observableArrayList(
                    "Could not load feedback."));
        }
    }

    // ── Logout ───────────────────────────────────────────────────
    @FXML
    private void handleLogout(ActionEvent e) {
        SessionManager.getInstance().logout();
        // Fixed: was "Login.fxml" — actual file is "LogIn.fxml"
        loadScreen(e, "/com/fooddelivery/views/LogIn.fxml",
                "Login");
    }

    private void loadScreen(ActionEvent e,
                             String path, String title) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource(path));
            Stage stage = (Stage) ((Node) e.getSource())
                              .getScene().getWindow();
            stage.setTitle("Food Delivery — " + title);
            stage.setMaximized(false);
            stage.setScene(new Scene(root, 480, 520));
            stage.setResizable(false);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
