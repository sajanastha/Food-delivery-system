package com.fooddelivery;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RestaurantDashboardController {

    private static final String ACTIVE_TAB_STYLE =
            "-fx-background-color: transparent;"
                    + "-fx-text-fill: #4F46E5;"
                    + "-fx-font-size: 13px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-padding: 10 18;"
                    + "-fx-cursor: hand;"
                    + "-fx-border-color: transparent transparent #4F46E5 transparent;"
                    + "-fx-border-width: 0 0 2 0;";

    private static final String INACTIVE_TAB_STYLE =
            "-fx-background-color: transparent;"
                    + "-fx-text-fill: #64748B;"
                    + "-fx-font-size: 13px;"
                    + "-fx-padding: 10 18;"
                    + "-fx-cursor: hand;"
                    + "-fx-border-color: transparent transparent transparent transparent;"
                    + "-fx-border-width: 0 0 2 0;";

    @FXML private Label welcomeLabel;
    @FXML private Label menuStatus;
    @FXML private Button menuTabButton;
    @FXML private Button ordersTabButton;
    @FXML private Button reportsTabButton;
    @FXML private Button feedbackTabButton;
    @FXML private Button profileTabButton;

    // Menu panel
    @FXML private ListView<String> menuListView;
    @FXML private TextField itemNameField;
    @FXML private TextField itemPriceField;
    @FXML private TextField itemCategoryField;
    @FXML private TextArea  itemDescField;

    // Orders panel
    @FXML private ListView<String> orderListView;
    @FXML private ListView<String> ongoingOrderListView;
    @FXML private TextArea orderDetailArea;
    @FXML private Label orderStatus;
    @FXML private Label ongoingStatus;

    // Reports panel
    @FXML private VBox reportCardBox;

    // Feedback panel
    @FXML private ListView<FeedbackEntry> feedbackListView;

    // Content panels (for manual switching)
    @FXML private VBox menuPanel;
    @FXML private VBox ordersPanel;
    @FXML private VBox reportsPanel;
    @FXML private VBox feedbackPanel;
    @FXML private VBox profilePanel;
    @FXML private TextField profileNameField;
    @FXML private Label profileEmailLabel;
    @FXML private Label profilePhoneLabel;
    @FXML private Label profileRoleLabel;

    private final MenuItemDAO   menuItemDAO   = new MenuItemDAO();
    private final OrderDAO      orderDAO      = new OrderDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final ReportService reportService = new ReportService();
    private final FeedbackDAO   feedbackDAO   = new FeedbackDAO();
    private final UserDAO       userDAO       = new UserDAO();

    private Restaurant      myRestaurant;
    private List<MenuItem>  menuItems     = new ArrayList<>();
    private List<Order>     orders        = new ArrayList<>(); // incoming (PENDING/CONFIRMED waiting for driver)
    private List<Order>     ongoingOrders = new ArrayList<>(); // IN_DELIVERY

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
        setProfileInfo(owner);
        showPanel("menu");
    }

    private void setProfileInfo(Restaurant owner) {
        profileNameField.setText(owner.getFullName());
        profileEmailLabel.setText("Email: " + owner.getEmail());
        profilePhoneLabel.setText("Phone: " + (owner.getPhone() == null || owner.getPhone().isBlank()
                ? "Not provided" : owner.getPhone()));
        profileRoleLabel.setText("Role: Restaurant Owner");
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        String newName = profileNameField.getText().trim();
        if (newName.isBlank()) {
            showAlert("Error", "Name cannot be empty.");
            return;
        }
        Restaurant owner = (Restaurant) SessionManager.getInstance().getCurrentUser();
        if (newName.equals(owner.getFullName())) {
            showAlert("Info", "No changes to save.");
            return;
        }
        owner.setFullName(newName);
        userDAO.updateUser(owner);
        profileNameField.setText(owner.getFullName());
        showAlert("Saved", "Your name has been updated.");
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        PasswordField currentPassword = new PasswordField();
        currentPassword.setPromptText("Current password");
        Dialog<String> verifyDialog = new Dialog<>();
        verifyDialog.setTitle("Verify Current Password");
        verifyDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        verifyDialog.getDialogPane().setContent(currentPassword);
        verifyDialog.setResultConverter(button -> button == ButtonType.OK ? currentPassword.getText() : null);
        Optional<String> currentResult = verifyDialog.showAndWait();
        if (currentResult.isEmpty() || currentResult.get().isBlank()) {
            return;
        }
        Restaurant owner = (Restaurant) SessionManager.getInstance().getCurrentUser();
        if (!currentResult.get().equals(owner.getPassword())) {
            showAlert("Error", "Current password is incorrect.");
            return;
        }

        PasswordField newPassword = new PasswordField();
        PasswordField confirmPassword = new PasswordField();
        newPassword.setPromptText("New password");
        confirmPassword.setPromptText("Confirm new password");
        GridPane passwordGrid = new GridPane();
        passwordGrid.setHgap(10);
        passwordGrid.setVgap(10);
        passwordGrid.add(new Label("New password:"), 0, 0);
        passwordGrid.add(newPassword, 1, 0);
        passwordGrid.add(new Label("Confirm password:"), 0, 1);
        passwordGrid.add(confirmPassword, 1, 1);

        Dialog<ButtonType> changeDialog = new Dialog<>();
        changeDialog.setTitle("Change Password");
        changeDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        changeDialog.getDialogPane().setContent(passwordGrid);
        Optional<ButtonType> changeResult = changeDialog.showAndWait();
        if (changeResult.isEmpty() || changeResult.get() != ButtonType.OK) {
            return;
        }
        String newPass = newPassword.getText().trim();
        String confirmPass = confirmPassword.getText().trim();
        if (newPass.isEmpty()) {
            showAlert("Error", "New password cannot be empty.");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            showAlert("Error", "Passwords do not match.");
            return;
        }
        owner.setPassword(newPass);
        userDAO.updateUser(owner);
        showAlert("Saved", "Password updated successfully.");
    }

    // ── Panel switching ───────────────────────────────────────────
    private void showPanel(String name) {
        menuPanel.setVisible(false);
        menuPanel.setManaged(false);
        ordersPanel.setVisible(false);
        ordersPanel.setManaged(false);
        reportsPanel.setVisible(false);
        reportsPanel.setManaged(false);
        feedbackPanel.setVisible(false);
        feedbackPanel.setManaged(false);
        profilePanel.setVisible(false);
        profilePanel.setManaged(false);
        switch (name) {
            case "menu" -> {
                menuPanel.setVisible(true);
                menuPanel.setManaged(true);
                setActiveTab(menuTabButton);
            }
            case "orders" -> {
                ordersPanel.setVisible(true);
                ordersPanel.setManaged(true);
                setActiveTab(ordersTabButton);
            }
            case "reports" -> {
                reportsPanel.setVisible(true);
                reportsPanel.setManaged(true);
                setActiveTab(reportsTabButton);
            }
            case "feedback" -> {
                feedbackPanel.setVisible(true);
                feedbackPanel.setManaged(true);
                setActiveTab(feedbackTabButton);
                loadFeedbackList(); // auto-load on tab open
            }
            case "profile" -> {
                profilePanel.setVisible(true);
                profilePanel.setManaged(true);
                setActiveTab(profileTabButton);
            }
        }
    }

    @FXML private void goMenu(ActionEvent e)     { showPanel("menu"); }
    @FXML private void goOrders(ActionEvent e)   { showPanel("orders"); loadOrdersSafe(); }
    @FXML private void goReports(ActionEvent e)  { showPanel("reports"); }
    @FXML private void goFeedback(ActionEvent e) { showPanel("feedback"); }
    @FXML private void goProfile(ActionEvent e)  { showPanel("profile"); }

    // ── Menu ─────────────────────────────────────────────────────
    private static final List<String> CATEGORY_ORDER =
            java.util.Arrays.asList("Starter", "Dessert", "Main", "Drink");

    private int categoryRank(String cat) {
        if (cat == null) return 99;
        for (int i = 0; i < CATEGORY_ORDER.size(); i++)
            if (CATEGORY_ORDER.get(i).equalsIgnoreCase(cat)) return i;
        return 50;
    }

    private void loadMenu() throws SQLException {
        List<MenuItem> raw = menuItemDAO.getByRestaurant(
                myRestaurant.getRestaurantID());

        // Sort by defined category order, then alphabetically within each
        raw.sort((a, b) -> {
            int ai = categoryRank(a.getCategory());
            int bi = categoryRank(b.getCategory());
            if (ai != bi) return ai - bi;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        menuItems = new ArrayList<>();
        List<String> display = new ArrayList<>();
        String lastCat = "";
        for (MenuItem m : raw) {
            String cat = m.getCategory() == null ? "Other" : m.getCategory();
            if (!cat.equalsIgnoreCase(lastCat)) {
                display.add("── " + cat.toUpperCase() + " ──");
                menuItems.add(null); // header placeholder (keeps index in sync)
                lastCat = cat;
            }
            menuItems.add(m);
            display.add("    " + m.getName()
                    + "   NPR " + String.format("%.2f", m.getPrice())
                    + (m.isAvailable() ? "  \u2713" : "  \u2717 unavailable"));
        }
        menuListView.setItems(
            FXCollections.observableArrayList(display));
    }

    @FXML
    private void handleMenuSelected() {
        int idx = menuListView.getSelectionModel()
                              .getSelectedIndex();
        if (idx < 0 || idx >= menuItems.size()) return;
        MenuItem m = menuItems.get(idx);
        if (m == null) { // clicked a category header row
            menuListView.getSelectionModel().clearSelection();
            return;
        }
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
        if (idx < 0 || idx >= menuItems.size() || menuItems.get(idx) == null) {
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
        if (idx < 0 || idx >= menuItems.size() || menuItems.get(idx) == null) {
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
        if (idx < 0 || idx >= menuItems.size() || menuItems.get(idx) == null) {
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

    private void setActiveTab(Button activeTab) {
        menuTabButton.setStyle(INACTIVE_TAB_STYLE);
        ordersTabButton.setStyle(INACTIVE_TAB_STYLE);
        reportsTabButton.setStyle(INACTIVE_TAB_STYLE);
        feedbackTabButton.setStyle(INACTIVE_TAB_STYLE);
        profileTabButton.setStyle(INACTIVE_TAB_STYLE);
        activeTab.setStyle(ACTIVE_TAB_STYLE);
    }

    // ── Orders ───────────────────────────────────────────────────
    private void loadOrders() throws SQLException {
        List<Order> all = orderDAO.getByRestaurant(
                myRestaurant.getRestaurantID());

        // Preparing = PENDING or CONFIRMED
        // Ongoing   = IN_DELIVERY
        orders        = new ArrayList<>();
        ongoingOrders = new ArrayList<>();

        for (Order o : all) {
            if (o.getStatus() == OrderStatus.IN_DELIVERY) {
                ongoingOrders.add(o);
            } else if (o.getStatus() == OrderStatus.PENDING
                    || o.getStatus() == OrderStatus.CONFIRMED) {
                orders.add(o);
            }
            // DELIVERED and CANCELLED are history — not shown here
        }

        // Populate preparing list
        List<String> incomingDisplay = new ArrayList<>();
        for (Order o : orders)
            incomingDisplay.add("Order #" + o.getOrderID()
                    + "  " + o.getStatus()
                    + "  NPR " + String.format("%.2f", o.getTotalAmount()));
        orderListView.setItems(
            FXCollections.observableArrayList(incomingDisplay));

        // Populate ongoing list
        List<String> ongoingDisplay = new ArrayList<>();
        for (Order o : ongoingOrders)
            ongoingDisplay.add("Order #" + o.getOrderID()
                    + "  🚚  NPR " + String.format("%.2f", o.getTotalAmount()));
        ongoingOrderListView.setItems(
            FXCollections.observableArrayList(ongoingDisplay));

        if (orders.isEmpty()) {
            orderStatus.setText("No orders currently waiting to be prepared.");
        } else {
            orderStatus.setText(orders.size() + " order(s) preparing.");
        }

        if (ongoingOrders.isEmpty())
            ongoingStatus.setText("No orders currently out for delivery.");
        else
            ongoingStatus.setText(ongoingOrders.size() + " order(s) in delivery.");
    }

    private void loadOrdersSafe() {
        try { loadOrders(); }
        catch (SQLException e) {
            orderStatus.setText("Error loading orders.");
        }
    }

    @FXML
    private void handleOrderSelected() {
        int idx = orderListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= orders.size()) return;
        ongoingOrderListView.getSelectionModel().clearSelection(); // deselect other list
        showOrderDetail(orders.get(idx));
    }

    @FXML
    private void handleOngoingOrderSelected() {
        int idx = ongoingOrderListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= ongoingOrders.size()) return;
        orderListView.getSelectionModel().clearSelection(); // deselect other list
        showOrderDetail(ongoingOrders.get(idx));
    }

    private void showOrderDetail(Order o) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order #").append(o.getOrderID()).append("\n");
        sb.append("Status : ").append(o.getStatus()).append("\n");
        sb.append("Address: ").append(o.getDeliveryAddress()).append("\n");
        sb.append("Total  : NPR ").append(String.format("%.2f", o.getTotalAmount())).append("\n");
        sb.append("\nItems:\n");
        if (o.getItems() == null || o.getItems().isEmpty()) {
            sb.append("  (no items loaded)");
        } else {
            for (var item : o.getItems())
                sb.append("  • ").append(item.getItemName())
                  .append(" x").append(item.getQuantity())
                  .append("  NPR ").append(String.format("%.2f", item.getSubtotal()))
                  .append("\n");
        }
        orderDetailArea.setText(sb.toString());
    }

    @FXML
    private void handleConfirm(ActionEvent e) {
        updateOrderStatus(OrderStatus.IN_DELIVERY,
            "Order is now out for delivery.");
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
        int idx = orderListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= orders.size()) {
            orderStatus.setText("Select a preparing order first.");
            return;
        }
        try {
            orderDAO.updateStatus(orders.get(idx).getOrderID(), s);
            orderStatus.setText(msg);
            orderDetailArea.clear();
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
            buildReportCard(report, "WEEKLY SALES REPORT",
                    7, report.getTotalOrders() == 0);
        } catch (SQLException ex) {
            buildErrorCard("Error generating weekly report.");
        }
    }

    @FXML
    private void handleMonthly(ActionEvent e) {
        if (myRestaurant == null) return;
        try {
            SalesReport report = reportService.monthlyReport(
                    myRestaurant.getRestaurantID());
            buildReportCard(report, "MONTHLY SALES REPORT",
                    30, report.getTotalOrders() == 0);
        } catch (SQLException ex) {
            buildErrorCard("Error generating monthly report.");
        }
    }

    private void buildReportCard(SalesReport r, String title,
                                  int days, boolean empty) {
        reportCardBox.getChildren().clear();

        // ── Big centred yellow title ───────────────────────────────
        Label titleLabel = new Label(title);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(javafx.geometry.Pos.CENTER);
        titleLabel.setStyle(
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FF7518;" +
                "-fx-padding: 0 0 18 0;");
        reportCardBox.getChildren().add(titleLabel);

        // ── Full-width separator ───────────────────────────────────
        Separator sep1 = new Separator();
        sep1.setMaxWidth(Double.MAX_VALUE);
        sep1.setStyle("-fx-background-color: #d8d8b8;");
        reportCardBox.getChildren().add(sep1);

        if (empty) {
            Label noData = new Label(
                    "No completed orders in the last " + days + " days.\n" +
                    "Keep accepting orders and the data will appear here.");
            noData.setWrapText(true);
            noData.setStyle(
                    "-fx-font-size: 14px;" +
                    "-fx-text-fill: #999;" +
                    "-fx-padding: 30 0;" +
                    "-fx-alignment: center;");
            noData.setMaxWidth(Double.MAX_VALUE);
            noData.setAlignment(javafx.geometry.Pos.CENTER);
            reportCardBox.getChildren().add(noData);
            return;
        }

        // ── Period & restaurant sub-heading ───────────────────────
        LocalDate to   = LocalDate.now();
        LocalDate from = to.minusDays(days);
        Label periodLabel = new Label(
                from + "  →  " + to +
                "   |   " + myRestaurant.getRestaurantName());
        periodLabel.setMaxWidth(Double.MAX_VALUE);
        periodLabel.setAlignment(javafx.geometry.Pos.CENTER);
        periodLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: #888;" +
                "-fx-padding: 10 0 22 0;");
        reportCardBox.getChildren().add(periodLabel);

        // ── Stat cards row ─────────────────────────────────────────
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(javafx.geometry.Pos.CENTER);
        statsRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statsRow, Priority.ALWAYS);

        double avgPerOrder = r.getTotalOrders() > 0
                ? r.getTotalRevenue() / r.getTotalOrders() : 0;

        statsRow.getChildren().addAll(
            makeStatCard("📦", "Total Orders",
                    String.valueOf(r.getTotalOrders()), "#4d9078"),
            makeStatCard("💰", "Total Revenue",
                    "NPR " + String.format("%.2f", r.getTotalRevenue()), "#FF7518"),
            makeStatCard("🍽", "Popular Item",
                    r.getTopSellingItem(), "#5b7ec7"),
            makeStatCard("📊", "Avg per Order",
                    "NPR " + String.format("%.2f", avgPerOrder), "#9b59b6")
        );
        reportCardBox.getChildren().add(statsRow);

        // ── Bottom separator ───────────────────────────────────────
        Separator sep2 = new Separator();
        sep2.setMaxWidth(Double.MAX_VALUE);
        sep2.setStyle("-fx-background-color: #d8d8b8; -fx-padding: 18 0 0 0;");
        VBox.setMargin(sep2, new javafx.geometry.Insets(24, 0, 0, 0));
        reportCardBox.getChildren().add(sep2);
    }

    private VBox makeStatCard(String icon, String label,
                               String value, String accent) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 28px;");
        iconLbl.setAlignment(javafx.geometry.Pos.CENTER);
        iconLbl.setMaxWidth(Double.MAX_VALUE);

        Label valueLbl = new Label(value);
        valueLbl.setWrapText(true);
        valueLbl.setAlignment(javafx.geometry.Pos.CENTER);
        valueLbl.setMaxWidth(Double.MAX_VALUE);
        valueLbl.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + accent + ";");

        Label labelLbl = new Label(label);
        labelLbl.setAlignment(javafx.geometry.Pos.CENTER);
        labelLbl.setMaxWidth(Double.MAX_VALUE);
        labelLbl.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: #888;" +
                "-fx-padding: 2 0 0 0;");

        VBox card = new VBox(6, iconLbl, valueLbl, labelLbl);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPadding(new javafx.geometry.Insets(20));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle(
                "-fx-background-color: #fafafa;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #e8e8c8;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;");
        return card;
    }

    private void buildErrorCard(String msg) {
        reportCardBox.getChildren().clear();
        Label err = new Label(msg);
        err.setStyle("-fx-font-size: 13px; -fx-text-fill: #c0392b;");
        reportCardBox.getChildren().add(err);
    }

    // ── Feedback ─────────────────────────────────────────────────
    @FXML
    private void handleLoadFeedback(ActionEvent e) {
        loadFeedbackList(); // kept for the Load Feedback button in FXML
    }

    private void loadFeedbackList() {
        if (myRestaurant == null) return;
        feedbackListView.setCellFactory(
                lv -> new FeedbackCardCell(FeedbackCardCell.Mode.RECEIVED));
        try {
            List<FeedbackEntry> entries =
                    feedbackDAO.getByRestaurant(myRestaurant.getRestaurantID());
            if (entries.isEmpty()) {
                feedbackListView.setItems(FXCollections.observableArrayList());
                return;
            }
            feedbackListView.setItems(FXCollections.observableArrayList(entries));
        } catch (SQLException ex) {
            ex.printStackTrace();
            feedbackListView.setItems(FXCollections.observableArrayList());
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

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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