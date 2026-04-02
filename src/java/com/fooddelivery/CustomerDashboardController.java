package com.fooddelivery;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardController {

    // Top nav
    @FXML private Label welcomeLabel;

    // Order tab
    @FXML private BorderPane orderPanel;
    @FXML private TextField searchField;
    @FXML private ListView<String> restaurantListView;
    @FXML private Label restaurantInfoLabel;
    @FXML private ListView<String> menuListView;
    @FXML private Label browseStatus;
    @FXML private Label menuTitleLabel;
    @FXML private Label cartCountLabel;
    @FXML private Label selectedItemLabel;

    // History tab
    @FXML private VBox historyPanel;
    @FXML private ListView<String> historyListView;

    // Profile panel
    @FXML private VBox profilePanel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profilePhoneLabel;
    @FXML private Label profileRoleLabel;

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
        showOrderTab(null);
    }

    // ── Tab switching ─────────────────────────────────────────────────────────
    @FXML
    public void showOrderTab(ActionEvent e) {
        orderPanel.setVisible(true);
        historyPanel.setVisible(false);
        profilePanel.setVisible(false);
    }

    @FXML
    public void showHistoryTab(ActionEvent e) {
        orderPanel.setVisible(false);
        historyPanel.setVisible(true);
        profilePanel.setVisible(false);
        loadHistory();
    }

    @FXML
    public void showProfile(ActionEvent e) {
        orderPanel.setVisible(false);
        historyPanel.setVisible(false);
        profilePanel.setVisible(true);
        profileNameLabel.setText("Name:   " + me.getFullName());
        profileEmailLabel.setText("Email:    " + me.getEmail());
        profilePhoneLabel.setText("Phone:  "
            + (me.getPhone() == null || me.getPhone().isEmpty()
                ? "Not provided" : me.getPhone()));
        profileRoleLabel.setText("Role:     Customer");
    }

    // ── Browse / Search ───────────────────────────────────────────────────────
    private void loadRestaurants() {
        try {
            allRestaurants = restaurantDAO.getAll();
            List<String> display = new ArrayList<>();
            for (Restaurant r : allRestaurants)
                display.add(r.getRestaurantName()
                    + "  ·  " + r.getCuisineType()
                    + "  ·  ⭐ "
                    + String.format("%.1f", r.getRating()));
            restaurantListView.setItems(
                FXCollections.observableArrayList(display));
            browseStatus.setText(
                allRestaurants.size() + " restaurants");
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
                    + "  ·  " + r.getCuisineType()
                    + "  ·  ⭐ "
                    + String.format("%.1f", r.getRating()));
            restaurantListView.setItems(
                FXCollections.observableArrayList(display));
            browseStatus.setText("Found "
                + allRestaurants.size() + " result(s)");
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
            selectedRest.getAddress()
            + "  ·  Hours: "
            + selectedRest.getOperatingHours());
        menuTitleLabel.setText(
            selectedRest.getRestaurantName() + " — Menu");
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
        int idx = menuListView.getSelectionModel()
                              .getSelectedIndex();
        if (idx < 0 || idx >= currentMenu.size()) {
            browseStatus.setText(
                "Click a menu item first, then Add to Cart.");
            return;
        }
        MenuItem m = currentMenu.get(idx);
        for (OrderItem existing : cart) {
            if (existing.getMenuItemID() == m.getItemID()) {
                existing.setQuantity(
                    existing.getQuantity() + 1);
                updateCartCount();
                selectedItemLabel.setText(
                    m.getName() + " quantity updated.");
                return;
            }
        }
        cart.add(new OrderItem(
            m.getItemID(), m.getName(), m.getPrice(), 1));
        updateCartCount();
        selectedItemLabel.setText(
            m.getName() + " added to cart.");
    }

    // Update cart count badge in header
    private void updateCartCount() {
        int total = cart.stream()
            .mapToInt(OrderItem::getQuantity).sum();
        cartCountLabel.setText(
            total > 0 ? total + " item(s) in cart" : "");
    }

    // "View Cart" opens a popup dialog
    @FXML
    private void handleViewCart(ActionEvent e) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.DECORATED);
        popup.setTitle("Your Cart");
        popup.setMinWidth(520);
        popup.setMinHeight(480);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F4F5F7;");

        // Header
        HBox header = new HBox();
        header.setStyle("-fx-background-color: white;"
            + "-fx-padding: 16 20;"
            + "-fx-border-color: #EBEBEB;"
            + "-fx-border-width: 0 0 1 0;");
        Label title = new Label("Your Cart");
        title.setStyle("-fx-font-size: 16px;"
            + "-fx-font-weight: bold;"
            + "-fx-text-fill: #222;");
        header.getChildren().add(title);
        root.getChildren().add(header);

        // Empty message
        if (cart.isEmpty()) {
            VBox emptyBox = new VBox();
            emptyBox.setStyle("-fx-alignment: center;"
                + "-fx-padding: 60;");
            Label emptyLbl = new Label(
                "Your cart is empty.\n\n"
                + "Go back and select items from the menu.");
            emptyLbl.setStyle("-fx-text-fill: #999;"
                + "-fx-font-size: 13px;"
                + "-fx-text-alignment: center;");
            emptyLbl.setWrapText(true);
            emptyBox.getChildren().add(emptyLbl);
            root.getChildren().add(emptyBox);
            VBox.setVgrow(emptyBox, Priority.ALWAYS);
        } else {
            // Cart items list
            ListView<String> cartList = new ListView<>();
            cartList.setStyle("-fx-border-color: transparent;"
                + "-fx-font-size: 13px;");
            List<String> lines = new ArrayList<>();
            for (OrderItem i : cart)
                lines.add(i.getItemName()
                    + "  ×" + i.getQuantity()
                    + "   =   NPR "
                    + String.format("%.2f", i.getSubtotal()));
            cartList.setItems(
                FXCollections.observableArrayList(lines));
            VBox.setVgrow(cartList, Priority.ALWAYS);

            // Remove selected button
            Button removeBtn = new Button("Remove Selected");
            removeBtn.setStyle("-fx-background-color: #FFF1F1;"
                + "-fx-text-fill: #DC2626;"
                + "-fx-background-radius: 7;"
                + "-fx-font-size: 12px;"
                + "-fx-padding: 6 14;"
                + "-fx-cursor: hand;");
            removeBtn.setOnAction(ev -> {
                int sel = cartList.getSelectionModel()
                                  .getSelectedIndex();
                if (sel >= 0 && sel < cart.size()) {
                    cart.remove(sel);
                    List<String> updated = new ArrayList<>();
                    for (OrderItem i : cart)
                        updated.add(i.getItemName()
                            + "  ×" + i.getQuantity()
                            + "   =   NPR "
                            + String.format("%.2f",
                                i.getSubtotal()));
                    cartList.setItems(FXCollections
                        .observableArrayList(updated));
                    updateCartCount();
                }
            });

            HBox removeRow = new HBox(removeBtn);
            removeRow.setStyle("-fx-padding: 6 16 0 16;");

            // Pricing summary
            double itemsTotal = cart.stream()
                .mapToDouble(OrderItem::getSubtotal).sum();
            double delivery   = pricingService.getDeliveryFee();
            double grand      = itemsTotal + delivery;

            VBox summary = new VBox(6);
            summary.setStyle("-fx-padding: 14 18;"
                + "-fx-background-color: white;"
                + "-fx-border-color: #EBEBEB;"
                + "-fx-border-width: 1 0 0 0;");
            Label lItems = new Label(
                "Items:       NPR "
                + String.format("%.2f", itemsTotal));
            lItems.setStyle("-fx-font-size: 13px;"
                + "-fx-text-fill: #555;");
            Label lDel = new Label(
                "Delivery:   NPR "
                + String.format("%.2f", delivery));
            lDel.setStyle("-fx-font-size: 13px;"
                + "-fx-text-fill: #555;");
            Label lTotal = new Label(
                "Total:          NPR "
                + String.format("%.2f", grand));
            lTotal.setStyle("-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #222;");
            summary.getChildren().addAll(
                lItems, lDel,
                new Separator(),
                lTotal);

            // Address field
            VBox addrBox = new VBox(6);
            addrBox.setStyle("-fx-padding: 10 18 0 18;");
            Label addrLbl = new Label("Delivery Address:");
            addrLbl.setStyle("-fx-font-size: 12px;"
                + "-fx-text-fill: #777;");
            TextField addrField = new TextField();
            addrField.setPromptText(
                "Enter your full delivery address");
            addrField.setStyle("-fx-border-color: #E0E0E0;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 8 12;"
                + "-fx-font-size: 13px;");
            addrBox.getChildren().addAll(addrLbl, addrField);

            // Action buttons
            HBox actions = new HBox(10);
            actions.setStyle("-fx-padding: 14 18 18 18;");
            Button continueBtn = new Button(
                "Continue Adding");
            continueBtn.setStyle("-fx-background-color: white;"
                + "-fx-text-fill: #555;"
                + "-fx-border-color: #DDD;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 10 18;"
                + "-fx-cursor: hand;");
            continueBtn.setOnAction(ev -> popup.close());

            Button orderBtn = new Button("Place Order");
            orderBtn.setStyle("-fx-background-color: #FF6B35;"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 8;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 10 28;"
                + "-fx-cursor: hand;");
            HBox.setHgrow(orderBtn, Priority.ALWAYS);
            orderBtn.setMaxWidth(Double.MAX_VALUE);

            Label statusLbl = new Label("");
            statusLbl.setStyle("-fx-font-size: 12px;"
                + "-fx-text-fill: #DC2626;");

            orderBtn.setOnAction(ev -> {
                String address = addrField.getText().trim();
                if (cart.isEmpty()) {
                    statusLbl.setText("Cart is empty.");
                    return;
                }
                if (address.isEmpty()) {
                    statusLbl.setText(
                        "Please enter delivery address.");
                    return;
                }
                if (selectedRest == null) {
                    statusLbl.setText(
                        "No restaurant selected.");
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
                        popup.close();
                        cart.clear();
                        updateCartCount();
                        selectedItemLabel.setText("");
                        showAlert("Order Placed!",
                            "Order #" + newID
                            + " placed successfully!\n"
                            + "The restaurant will confirm shortly.\n"
                            + "Total: NPR "
                            + String.format("%.2f", grand));
                        loadHistory();
                    }
                } catch (SQLException ex) {
                    statusLbl.setText(
                        "Failed to place order. Try again.");
                    ex.printStackTrace();
                }
            });

            actions.getChildren().addAll(
                continueBtn, orderBtn);

            root.getChildren().addAll(
                cartList, removeRow,
                summary, addrBox,
                actions, statusLbl);
        }

        // Always show a "Continue Adding" button if empty
        if (cart.isEmpty()) {
            Button backBtn = new Button("Continue Adding");
            backBtn.setStyle("-fx-background-color: #FF6B35;"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 8;"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 10 24;"
                + "-fx-cursor: hand;");
            backBtn.setOnAction(ev -> popup.close());
            HBox btnRow = new HBox(backBtn);
            btnRow.setStyle("-fx-padding: 16; -fx-alignment: center;");
            root.getChildren().add(btnRow);
        }

        popup.setScene(new Scene(root));
        popup.showAndWait();
    }

    // ── Order History & Feedback ───────────────────────────────────────────────
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
                    + "   │   " + o.getStatus()
                    + "   │   NPR "
                    + String.format("%.2f", o.getTotalAmount())
                    + "   │   → " + o.getDeliveryAddress());
            historyListView.setItems(
                FXCollections.observableArrayList(display));
        } catch (SQLException ex) {
            historyListView.setItems(
                FXCollections.observableArrayList(
                    "Could not load order history."));
        }
    }

    // Feedback popup — customer selects a delivered order and rates it
    @FXML
    private void handleLeaveFeedback(ActionEvent e) {
        int idx = historyListView.getSelectionModel()
                                  .getSelectedIndex();

        // Load delivered orders for feedback
        List<Order> delivered = new ArrayList<>();
        try {
            List<Order> all = orderDAO.getByCustomer(
                    me.getUserID());
            for (Order o : all)
                if (o.getStatus() == OrderStatus.DELIVERED)
                    delivered.add(o);
        } catch (SQLException ex) {
            showAlert("Error",
                "Could not load orders.");
            return;
        }

        if (delivered.isEmpty()) {
            showAlert("No Delivered Orders",
                "You have no delivered orders to give feedback on.\n"
                + "Feedback is available after an order\n"
                + "is marked as Delivered.");
            return;
        }

        // Pick which order to review
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Leave Feedback");
        popup.setMinWidth(460);
        popup.setMinHeight(400);

        VBox root = new VBox(14);
        root.setStyle("-fx-padding: 24;"
            + "-fx-background-color: white;");

        Label ttl = new Label("Leave a Review");
        ttl.setStyle("-fx-font-size: 16px;"
            + "-fx-font-weight: bold;"
            + "-fx-text-fill: #222;");

        Label subttl = new Label(
            "Select the order you want to review:");
        subttl.setStyle("-fx-font-size: 12px;"
            + "-fx-text-fill: #888;");

        ComboBox<String> orderPicker = new ComboBox<>();
        orderPicker.setMaxWidth(Double.MAX_VALUE);
        for (Order o : delivered)
            orderPicker.getItems().add(
                "Order #" + o.getOrderID()
                + "  —  NPR "
                + String.format("%.2f", o.getTotalAmount()));
        orderPicker.getSelectionModel().selectFirst();

        Label ratingLbl = new Label("Rating:");
        ratingLbl.setStyle("-fx-font-size: 12px;"
            + "-fx-text-fill: #777;");

        HBox stars = new HBox(6);
        ToggleGroup tg = new ToggleGroup();
        for (int i = 1; i <= 5; i++) {
            ToggleButton tb = new ToggleButton(i + " ★");
            tb.setToggleGroup(tg);
            tb.setUserData(i);
            tb.setStyle("-fx-background-color: #F0F0F0;"
                + "-fx-text-fill: #555;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 6 14;"
                + "-fx-font-size: 12px;"
                + "-fx-cursor: hand;");
            if (i == 5) tb.setSelected(true);
            stars.getChildren().add(tb);
        }

        Label commentLbl = new Label("Comment:");
        commentLbl.setStyle("-fx-font-size: 12px;"
            + "-fx-text-fill: #777;");

        TextArea commentArea = new TextArea();
        commentArea.setPromptText(
            "Tell the restaurant what you thought...");
        commentArea.setPrefRowCount(4);
        commentArea.setStyle("-fx-border-color: #E0E0E0;"
            + "-fx-border-radius: 8;"
            + "-fx-background-radius: 8;"
            + "-fx-font-size: 13px;");

        Label statusLbl = new Label("");
        statusLbl.setStyle("-fx-font-size: 12px;"
            + "-fx-text-fill: #DC2626;");

        HBox actions = new HBox(10);
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #F0F0F0;"
            + "-fx-text-fill: #555;"
            + "-fx-background-radius: 8;"
            + "-fx-padding: 9 18;"
            + "-fx-font-size: 13px;"
            + "-fx-cursor: hand;");
        cancelBtn.setOnAction(ev -> popup.close());

        Button submitBtn = new Button("Submit Feedback");
        submitBtn.setStyle("-fx-background-color: #FF6B35;"
            + "-fx-text-fill: white;"
            + "-fx-background-radius: 8;"
            + "-fx-font-size: 13px;"
            + "-fx-font-weight: bold;"
            + "-fx-padding: 9 20;"
            + "-fx-cursor: hand;");
        HBox.setHgrow(submitBtn, Priority.ALWAYS);
        submitBtn.setMaxWidth(Double.MAX_VALUE);

        submitBtn.setOnAction(ev -> {
            int selIdx = orderPicker.getSelectionModel()
                                    .getSelectedIndex();
            if (selIdx < 0) {
                statusLbl.setText("Select an order.");
                return;
            }
            if (tg.getSelectedToggle() == null) {
                statusLbl.setText("Select a rating.");
                return;
            }
            int rating = (int) tg.getSelectedToggle()
                                  .getUserData();
            String comment = commentArea.getText().trim();
            Order chosen = delivered.get(selIdx);

            try {
                FeedbackDAO fbDAO = new FeedbackDAO();
                fbDAO.saveOrUpdate(
                    me.getUserID(),
                    chosen.getRestaurantID(),
                    chosen.getOrderID(),
                    rating,
                    comment);
                popup.close();
                showAlert("Thank you!",
                    "Your feedback for Order #"
                    + chosen.getOrderID()
                    + " has been submitted.\n"
                    + "Rating: " + rating + "/5");
            } catch (SQLException ex) {
                statusLbl.setText("Error saving feedback.");
            }
        });

        actions.getChildren().addAll(cancelBtn, submitBtn);
        root.getChildren().addAll(
            ttl, subttl,
            orderPicker,
            ratingLbl, stars,
            commentLbl, commentArea,
            statusLbl, actions);

        popup.setScene(new Scene(root));
        popup.showAndWait();
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