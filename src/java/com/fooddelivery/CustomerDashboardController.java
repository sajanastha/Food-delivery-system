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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CustomerDashboardController {

    @FXML private Label welcomeLabel;

    @FXML private VBox homePanel;
    @FXML private Label homeDateLabel;
    @FXML private Label homeStatusLabel;
    @FXML private ListView<String> topFoodsListView;
    @FXML private ListView<String> topRestaurantsListView;

    @FXML private VBox orderPanel;
    @FXML private TextField searchField;
    @FXML private ListView<String> restaurantListView;
    @FXML private Label restaurantInfoLabel;
    @FXML private ListView<String> menuListView;
    @FXML private Label browseStatus;
    @FXML private Label menuTitleLabel;
    @FXML private Label cartCountLabel;
    @FXML private Label selectedItemLabel;

    @FXML private VBox historyPanel;
    @FXML private ListView<String> historyListView;
    @FXML private TextArea historyDetailArea;
    @FXML private Label historyStatusLabel;
    @FXML private Button feedbackButton;

    @FXML private VBox profilePanel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profilePhoneLabel;
    @FXML private Label profileRoleLabel;

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PricingService pricingService = new PricingService();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();

    private List<Restaurant> allRestaurants = new ArrayList<>();
    private List<MenuItem> currentMenu = new ArrayList<>();
    private List<OrderItem> cart = new ArrayList<>();
    private List<Order> historyOrders = new ArrayList<>();
    private Restaurant selectedRest;
    private Order selectedHistoryOrder;
    private Customer me;

    @FXML
    public void initialize() {
        me = (Customer) SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Hello, " + me.getFullName());
        loadRestaurants();
        loadHomeInsights();
        loadHistory();
        loadProfile();
        updateCartCount();
        showHomeTab(null);
    }

    @FXML
    public void showHomeTab(ActionEvent event) {
        homePanel.setVisible(true);
        homePanel.setManaged(true);
        orderPanel.setVisible(false);
        orderPanel.setManaged(false);
        historyPanel.setVisible(false);
        historyPanel.setManaged(false);
        profilePanel.setVisible(false);
        profilePanel.setManaged(false);
        loadHomeInsights();
    }

    @FXML
    public void showOrderTab(ActionEvent event) {
        homePanel.setVisible(false);
        homePanel.setManaged(false);
        orderPanel.setVisible(true);
        orderPanel.setManaged(true);
        historyPanel.setVisible(false);
        historyPanel.setManaged(false);
        profilePanel.setVisible(false);
        profilePanel.setManaged(false);
    }

    @FXML
    public void showHistoryTab(ActionEvent event) {
        homePanel.setVisible(false);
        homePanel.setManaged(false);
        orderPanel.setVisible(false);
        orderPanel.setManaged(false);
        historyPanel.setVisible(true);
        historyPanel.setManaged(true);
        profilePanel.setVisible(false);
        profilePanel.setManaged(false);
        loadHistory();
    }

    @FXML
    public void showProfile(ActionEvent event) {
        homePanel.setVisible(false);
        homePanel.setManaged(false);
        orderPanel.setVisible(false);
        orderPanel.setManaged(false);
        historyPanel.setVisible(false);
        historyPanel.setManaged(false);
        profilePanel.setVisible(true);
        profilePanel.setManaged(true);
        loadProfile();
    }

    private void loadHomeInsights() {
        homeDateLabel.setText("Today: " + LocalDate.now());
        try {
            List<String> topFoods = orderDAO.getTopFoodsForToday(3);
            List<String> topRestaurants = orderDAO.getTopRestaurantsForToday(3);

            if (topFoods.isEmpty()) {
                topFoods.add("No food orders yet today.");
            }
            if (topRestaurants.isEmpty()) {
                topRestaurants.add("No restaurant orders yet today.");
            }

            topFoodsListView.setItems(FXCollections.observableArrayList(topFoods));
            topRestaurantsListView.setItems(FXCollections.observableArrayList(topRestaurants));
            homeStatusLabel.setText("Top 3 popular foods and restaurants based on today's orders.");
        } catch (SQLException e) {
            homeStatusLabel.setText("Could not load today's popularity data.");
            topFoodsListView.setItems(FXCollections.observableArrayList("Unavailable"));
            topRestaurantsListView.setItems(FXCollections.observableArrayList("Unavailable"));
        }
    }

    private void loadRestaurants() {
        try {
            allRestaurants = restaurantDAO.getAll();
            restaurantListView.setItems(
                    FXCollections.observableArrayList(formatRestaurantList(allRestaurants)));
            browseStatus.setText(allRestaurants.size() + " restaurants");
        } catch (SQLException ex) {
            browseStatus.setText("Could not load restaurants.");
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadRestaurants();
            return;
        }

        try {
            List<Restaurant> byName = restaurantDAO.searchByName(keyword);
            List<MenuItem> byFood = menuItemDAO.searchByName(keyword);
            Set<Integer> seenRestaurantIds = new LinkedHashSet<>();
            List<Restaurant> results = new ArrayList<>();

            for (Restaurant restaurant : byName) {
                if (seenRestaurantIds.add(restaurant.getRestaurantID())) {
                    results.add(restaurant);
                }
            }

            for (MenuItem item : byFood) {
                if (seenRestaurantIds.add(item.getRestaurantID())) {
                    Restaurant restaurant = restaurantDAO.getById(item.getRestaurantID());
                    if (restaurant != null) {
                        results.add(restaurant);
                    }
                }
            }

            allRestaurants = results;
            restaurantListView.setItems(
                    FXCollections.observableArrayList(formatRestaurantList(results)));
            browseStatus.setText("Found " + results.size() + " matching restaurant(s)");
        } catch (SQLException ex) {
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
        int idx = restaurantListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= allRestaurants.size()) {
            return;
        }

        selectedRest = allRestaurants.get(idx);
        restaurantInfoLabel.setText(selectedRest.getAddress()
                + " | Hours: " + selectedRest.getOperatingHours());
        menuTitleLabel.setText(selectedRest.getRestaurantName() + " - Menu");

        try {
            currentMenu = menuItemDAO.getAvailable(selectedRest.getRestaurantID());
            List<String> display = new ArrayList<>();
            for (MenuItem item : currentMenu) {
                display.add(item.getName()
                        + " | " + item.getCategory()
                        + " | NPR " + String.format("%.0f", item.getPrice()));
            }
            menuListView.setItems(FXCollections.observableArrayList(display));
            selectedItemLabel.setText("");
        } catch (SQLException ex) {
            browseStatus.setText("Could not load menu.");
        }
    }

    @FXML
    private void handleAddToCart(ActionEvent event) {
        int idx = menuListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= currentMenu.size()) {
            browseStatus.setText("Select a menu item first.");
            return;
        }

        MenuItem item = currentMenu.get(idx);
        for (OrderItem existing : cart) {
            if (existing.getMenuItemID() == item.getItemID()) {
                existing.setQuantity(existing.getQuantity() + 1);
                updateCartCount();
                selectedItemLabel.setText(item.getName() + " quantity updated.");
                return;
            }
        }

        cart.add(new OrderItem(item.getItemID(), item.getName(), item.getPrice(), 1));
        updateCartCount();
        selectedItemLabel.setText(item.getName() + " added to cart.");
    }

    private void updateCartCount() {
        int total = cart.stream().mapToInt(OrderItem::getQuantity).sum();
        cartCountLabel.setText(total > 0 ? total + " item(s) in cart" : "");
    }

    @FXML
    private void handleViewCart(ActionEvent event) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.DECORATED);
        popup.setTitle("Your Cart");
        popup.setMinWidth(520);
        popup.setMinHeight(480);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F4F5F7;");

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

        if (cart.isEmpty()) {
            VBox emptyBox = new VBox();
            emptyBox.setStyle("-fx-alignment: center;"
                    + "-fx-padding: 60;");
            Label emptyLbl = new Label("Your cart is empty.\n\nGo back and select items from the menu.");
            emptyLbl.setStyle("-fx-text-fill: #999;"
                    + "-fx-font-size: 13px;"
                    + "-fx-text-alignment: center;");
            emptyLbl.setWrapText(true);
            emptyBox.getChildren().add(emptyLbl);
            root.getChildren().add(emptyBox);
            VBox.setVgrow(emptyBox, Priority.ALWAYS);
        } else {
            ListView<String> cartList = new ListView<>();
            cartList.setStyle("-fx-border-color: transparent;"
                    + "-fx-font-size: 13px;");
            cartList.setItems(FXCollections.observableArrayList(buildCartLines()));
            VBox.setVgrow(cartList, Priority.ALWAYS);

            Button removeBtn = new Button("Remove Selected");
            removeBtn.setStyle("-fx-background-color: #FFF1F1;"
                    + "-fx-text-fill: #DC2626;"
                    + "-fx-background-radius: 7;"
                    + "-fx-font-size: 12px;"
                    + "-fx-padding: 6 14;"
                    + "-fx-cursor: hand;");
            removeBtn.setOnAction(ev -> {
                int sel = cartList.getSelectionModel().getSelectedIndex();
                if (sel >= 0 && sel < cart.size()) {
                    cart.remove(sel);
                    cartList.setItems(FXCollections.observableArrayList(buildCartLines()));
                    updateCartCount();
                }
            });

            HBox removeRow = new HBox(removeBtn);
            removeRow.setStyle("-fx-padding: 6 16 0 16;");

            double itemsTotal = cart.stream().mapToDouble(OrderItem::getSubtotal).sum();
            double delivery = pricingService.getDeliveryFee();
            double grand = itemsTotal + delivery;

            VBox summary = new VBox(6);
            summary.setStyle("-fx-padding: 14 18;"
                    + "-fx-background-color: white;"
                    + "-fx-border-color: #EBEBEB;"
                    + "-fx-border-width: 1 0 0 0;");
            Label lItems = new Label("Items:       NPR " + String.format("%.2f", itemsTotal));
            Label lDel = new Label("Delivery:   NPR " + String.format("%.2f", delivery));
            Label lTotal = new Label("Total:          NPR " + String.format("%.2f", grand));
            lItems.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
            lDel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
            lTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #222;");
            summary.getChildren().addAll(lItems, lDel, new Separator(), lTotal);

            VBox addrBox = new VBox(6);
            addrBox.setStyle("-fx-padding: 10 18 0 18;");
            Label addrLbl = new Label("Delivery Address:");
            addrLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #777;");
            TextField addrField = new TextField();
            addrField.setPromptText("Enter your full delivery address");
            addrField.setStyle("-fx-border-color: #E0E0E0;"
                    + "-fx-border-radius: 8;"
                    + "-fx-background-radius: 8;"
                    + "-fx-padding: 8 12;"
                    + "-fx-font-size: 13px;");
            addrBox.getChildren().addAll(addrLbl, addrField);

            HBox actions = new HBox(10);
            actions.setStyle("-fx-padding: 14 18 18 18;");
            Button continueBtn = new Button("Continue Adding");
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
            statusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626;");

            orderBtn.setOnAction(ev -> {
                String address = addrField.getText().trim();
                if (cart.isEmpty()) {
                    statusLbl.setText("Cart is empty.");
                    return;
                }
                if (address.isEmpty()) {
                    statusLbl.setText("Please enter delivery address.");
                    return;
                }
                if (selectedRest == null) {
                    statusLbl.setText("No restaurant selected.");
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
                        loadHistory();
                        loadHomeInsights();
                        showAlert("Order Placed!",
                                "Order #" + newID + " placed successfully!\n"
                                        + "The restaurant will confirm shortly.\n"
                                        + "Total: NPR " + String.format("%.2f", grand));
                    }
                } catch (SQLException ex) {
                    statusLbl.setText("Failed to place order. Try again.");
                }
            });

            actions.getChildren().addAll(continueBtn, orderBtn);
            root.getChildren().addAll(cartList, removeRow, summary, addrBox, actions, statusLbl);
        }

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

    @FXML
    private void handleRefreshHistory(ActionEvent event) {
        loadHistory();
    }

    private void loadHistory() {
        try {
            historyOrders = orderDAO.getByCustomer(me.getUserID());
            List<String> display = new ArrayList<>();
            for (Order order : historyOrders) {
                display.add("Order #" + order.getOrderID()
                        + " | " + getRestaurantName(order.getRestaurantID())
                        + " | " + firstItemsSummary(order)
                        + " | " + order.getStatus());
            }

            if (display.isEmpty()) {
                display.add("No orders yet.");
            }

            historyListView.setItems(FXCollections.observableArrayList(display));
            historyStatusLabel.setText(historyOrders.size() + " order(s) found");
            historyDetailArea.setText(historyOrders.isEmpty()
                    ? "Your order history will appear here."
                    : "Select an order to see restaurant, items, address, and feedback.");
            selectedHistoryOrder = null;
            feedbackButton.setDisable(true);
            feedbackButton.setText("Add Feedback");
        } catch (SQLException ex) {
            historyListView.setItems(FXCollections.observableArrayList("Could not load order history."));
            historyStatusLabel.setText("History unavailable.");
        }
    }

    @FXML
    private void handleHistorySelected() {
        int idx = historyListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= historyOrders.size()) {
            return;
        }

        selectedHistoryOrder = historyOrders.get(idx);
        historyDetailArea.setText(buildHistoryDetails(selectedHistoryOrder));
        updateFeedbackButton();
    }

    @FXML
    private void handleFeedbackAction(ActionEvent event) {
        if (selectedHistoryOrder == null) {
            showAlert("Select an Order", "Choose an order from history first.");
            return;
        }
        if (selectedHistoryOrder.getStatus() != OrderStatus.DELIVERED) {
            showAlert("Feedback Unavailable",
                    "Feedback can only be added after the order is delivered.");
            return;
        }

        openFeedbackPopup(selectedHistoryOrder);
    }

    private void updateFeedbackButton() {
        if (selectedHistoryOrder == null
                || selectedHistoryOrder.getStatus() != OrderStatus.DELIVERED) {
            feedbackButton.setDisable(true);
            feedbackButton.setText("Add Feedback");
            return;
        }

        try {
            FeedbackEntry entry = feedbackDAO.getByCustomerAndOrder(
                    me.getUserID(), selectedHistoryOrder.getOrderID());
            feedbackButton.setDisable(false);
            feedbackButton.setText(entry == null ? "Add Feedback" : "View Feedback");
        } catch (SQLException e) {
            feedbackButton.setDisable(false);
            feedbackButton.setText("Add Feedback");
        }
    }

    private void openFeedbackPopup(Order order) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Order Feedback");
        popup.setMinWidth(480);
        popup.setMinHeight(420);

        VBox root = new VBox(14);
        root.setStyle("-fx-padding: 24; -fx-background-color: white;");

        Label title = new Label("Feedback for Order #" + order.getOrderID());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        Label subtitle = new Label(getRestaurantName(order.getRestaurantID())
                + " | " + firstItemsSummary(order));
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        Label ratingLbl = new Label("Rating");
        ratingLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        HBox stars = new HBox(6);
        ToggleGroup group = new ToggleGroup();
        List<ToggleButton> toggles = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ToggleButton star = new ToggleButton(i + " Star");
            star.setToggleGroup(group);
            star.setUserData(i);
            star.setStyle("-fx-background-color: #DBEAFE;"
                    + "-fx-text-fill: #1D4ED8;"
                    + "-fx-background-radius: 8;"
                    + "-fx-font-size: 12px;"
                    + "-fx-padding: 7 12;"
                    + "-fx-cursor: hand;");
            toggles.add(star);
            stars.getChildren().add(star);
        }

        Label commentLbl = new Label("Comment");
        commentLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Write your feedback here...");
        commentArea.setPrefRowCount(5);
        commentArea.setWrapText(true);
        commentArea.setStyle("-fx-border-color: #CBD5E1;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-font-size: 13px;");

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626;");

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #E2E8F0;"
                + "-fx-text-fill: #334155;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 9 18;"
                + "-fx-cursor: hand;");
        closeBtn.setOnAction(ev -> popup.close());

        Button saveBtn = new Button("Save Feedback");
        saveBtn.setStyle("-fx-background-color: #2563EB;"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 9 20;"
                + "-fx-cursor: hand;");
        HBox.setHgrow(saveBtn, Priority.ALWAYS);
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        try {
            FeedbackEntry existing = feedbackDAO.getByCustomerAndOrder(
                    me.getUserID(), order.getOrderID());
            if (existing != null) {
                commentArea.setText(existing.getComment());
                for (ToggleButton toggle : toggles) {
                    if ((int) toggle.getUserData() == existing.getRating()) {
                        toggle.setSelected(true);
                        break;
                    }
                }
                saveBtn.setText("Update Feedback");
            } else {
                toggles.get(4).setSelected(true);
            }
        } catch (SQLException e) {
            toggles.get(4).setSelected(true);
        }

        saveBtn.setOnAction(ev -> {
            if (group.getSelectedToggle() == null) {
                statusLabel.setText("Select a rating.");
                return;
            }
            int rating = (int) group.getSelectedToggle().getUserData();
            String comment = commentArea.getText().trim();
            try {
                feedbackDAO.saveOrUpdate(
                        me.getUserID(),
                        order.getRestaurantID(),
                        order.getOrderID(),
                        rating,
                        comment);
                popup.close();
                updateFeedbackButton();
                historyDetailArea.setText(buildHistoryDetails(order));
                showAlert("Feedback Saved",
                        "Your feedback for Order #" + order.getOrderID() + " has been saved.");
            } catch (SQLException e) {
                statusLabel.setText("Could not save feedback.");
            }
        });

        HBox actions = new HBox(10, closeBtn, saveBtn);
        root.getChildren().addAll(title, subtitle, ratingLbl, stars, commentLbl, commentArea,
                statusLabel, actions);

        popup.setScene(new Scene(root));
        popup.showAndWait();
    }

    private void loadProfile() {
        profileNameLabel.setText("Name: " + me.getFullName());
        profileEmailLabel.setText("Email: " + me.getEmail());
        profilePhoneLabel.setText("Phone: "
                + (me.getPhone() == null || me.getPhone().isBlank() ? "Not provided" : me.getPhone()));
        profileRoleLabel.setText("Role: Customer");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        loadScreen(event, "/com/fooddelivery/views/LogIn.fxml", "Login");
    }

    private List<String> formatRestaurantList(List<Restaurant> restaurants) {
        List<String> display = new ArrayList<>();
        for (Restaurant restaurant : restaurants) {
            display.add(restaurant.getRestaurantName()
                    + " | " + restaurant.getCuisineType()
                    + " | Rating " + String.format("%.1f", restaurant.getRating()));
        }
        return display;
    }

    private List<String> buildCartLines() {
        List<String> lines = new ArrayList<>();
        for (OrderItem item : cart) {
            lines.add(item.getItemName()
                    + " x" + item.getQuantity()
                    + " = NPR " + String.format("%.2f", item.getSubtotal()));
        }
        return lines;
    }

    private String buildHistoryDetails(Order order) {
        StringBuilder items = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            if (items.length() > 0) {
                items.append("\n");
            }
            items.append("- ")
                    .append(item.getItemName())
                    .append(" x")
                    .append(item.getQuantity())
                    .append(" (NPR ")
                    .append(String.format("%.2f", item.getSubtotal()))
                    .append(")");
        }

        String feedbackInfo = "No feedback yet.";
        try {
            FeedbackEntry entry = feedbackDAO.getByCustomerAndOrder(me.getUserID(), order.getOrderID());
            if (entry != null) {
                feedbackInfo = "Rating: " + entry.getRating() + "/5\nComment: "
                        + (entry.getComment() == null || entry.getComment().isBlank()
                        ? "-" : entry.getComment());
            }
        } catch (SQLException ignored) {
        }

        return "Order ID: " + order.getOrderID() + "\n"
                + "Restaurant: " + getRestaurantName(order.getRestaurantID()) + "\n"
                + "Status: " + order.getStatus() + "\n"
                + "Address: " + order.getDeliveryAddress() + "\n"
                + "Total: NPR " + String.format("%.2f", order.getTotalAmount()) + "\n"
                + "Items:\n" + (items.length() == 0 ? "- No items found" : items) + "\n\n"
                + "Feedback:\n" + feedbackInfo;
    }

    private String firstItemsSummary(Order order) {
        if (order.getItems().isEmpty()) {
            return "No items";
        }
        String first = order.getItems().get(0).getItemName();
        int extra = order.getItems().size() - 1;
        return extra > 0 ? first + " +" + extra + " more" : first;
    }

    private String getRestaurantName(int restaurantID) {
        try {
            Restaurant restaurant = restaurantDAO.getById(restaurantID);
            return restaurant == null ? "Restaurant #" + restaurantID : restaurant.getRestaurantName();
        } catch (SQLException e) {
            return "Restaurant #" + restaurantID;
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void loadScreen(ActionEvent event, String path, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Food Delivery - " + title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
