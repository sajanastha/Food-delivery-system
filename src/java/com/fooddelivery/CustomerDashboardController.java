package com.fooddelivery;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomerDashboardController {

    @FXML private ScrollPane homePage;
    @FXML private VBox searchPage;
    @FXML private VBox cartPage;
    @FXML private VBox historyPage;
    @FXML private ScrollPane profilePage;
    @FXML private ScrollPane feedbackPage;

    @FXML private FlowPane popularRestaurantsPane;
    @FXML private FlowPane popularFoodPane;
    @FXML private Label noPopularRestLabel;
    @FXML private Label noPopularFoodLabel;

    @FXML private TextField searchField;
    @FXML private ToggleGroup searchTypeGroup;
    @FXML private RadioButton searchRestaurantRadio;
    @FXML private RadioButton searchFoodRadio;
    @FXML private Label searchStatusLabel;
    @FXML private Label notFoundLabel;
    @FXML private FlowPane searchResultsPane;

    @FXML private ListView<String> cartListView;
    @FXML private TextArea pricingArea;
    @FXML private TextField addressField;
    @FXML private Label cartStatus;

    @FXML private ListView<String> historyListView;
    @FXML private VBox emptyHistoryBox;

    @FXML private TextField profileNameField;
    @FXML private TextField profilePhoneField;
    @FXML private Label nameStatus;
    @FXML private TextField currentEmailDisplay;
    @FXML private TextField newEmailField;
    @FXML private PasswordField emailConfirmPassField;
    @FXML private Label emailStatus;
    @FXML private PasswordField currentPassField;
    @FXML private PasswordField newPassField;
    @FXML private Label passStatus;
    @FXML private Label profileWelcomeLabel;

    @FXML private VBox feedbackItemsBox;
    @FXML private VBox emptyFeedbackBox;
    @FXML private Label feedbackPageStatus;

    @FXML private Button navHome;
    @FXML private Button navSearch;
    @FXML private Button navHistory;
    @FXML private Button navProfile;
    @FXML private Button navFeedback;

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PricingService pricingService = new PricingService();
    private final UserDAO userDAO = new UserDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();

    private final List<OrderItem> cart = new ArrayList<>();
    private Restaurant selectedRest;
    private Customer me;

    private static final String ACTIVE_NAV =
            "-fx-background-color: #FFF7ED; -fx-text-fill: #FF7518; " +
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;";
    private static final String INACTIVE_NAV =
            "-fx-background-color: white; -fx-text-fill: #6B7280; " +
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;";
    private static final String ORANGE_BUTTON =
            "-fx-background-color: #FF7A00; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (!(currentUser instanceof Customer customer)) {
            throw new IllegalStateException("Customer dashboard requires a logged-in customer.");
        }
        me = customer;
        loadProfileFields();
        loadHomePopular();
        loadHistory();
        loadFeedbackPage();
        showPage(homePage);
        setActiveNav(navHome);
    }

    private void showPage(Node page) {
        homePage.setVisible(false);
        searchPage.setVisible(false);
        cartPage.setVisible(false);
        historyPage.setVisible(false);
        profilePage.setVisible(false);
        feedbackPage.setVisible(false);
        page.setVisible(true);
    }

    private void setActiveNav(Button active) {
        navHome.setStyle(INACTIVE_NAV);
        navSearch.setStyle(INACTIVE_NAV);
        navHistory.setStyle(INACTIVE_NAV);
        navProfile.setStyle(INACTIVE_NAV);
        navFeedback.setStyle(INACTIVE_NAV);
        active.setStyle(ACTIVE_NAV);
    }

    @FXML private void navToHome() { showPage(homePage); setActiveNav(navHome); loadHomePopular(); }
    @FXML private void navToSearch() { showPage(searchPage); setActiveNav(navSearch); }
    @FXML private void navToHistory() { showPage(historyPage); setActiveNav(navHistory); loadHistory(); }
    @FXML private void navToProfile() { showPage(profilePage); setActiveNav(navProfile); loadProfileFields(); }
    @FXML private void navToFeedback() { showPage(feedbackPage); setActiveNav(navFeedback); loadFeedbackPage(); }
    @FXML private void goToSearch() { navToSearch(); }

    private void loadHomePopular() {
        try {
            List<Restaurant> rests = restaurantDAO.getAll();
            popularRestaurantsPane.getChildren().clear();

            if (rests.isEmpty()) {
                noPopularRestLabel.setText("No restaurants registered yet.");
                noPopularRestLabel.setVisible(true);
            } else {
                noPopularRestLabel.setVisible(false);
                for (Restaurant r : rests) {
                    popularRestaurantsPane.getChildren().add(makeRestCard(r));
                }
            }

            List<MenuItem> items = menuItemDAO.getAllMenuItems();
            popularFoodPane.getChildren().clear();
            if (items.isEmpty()) {
                noPopularFoodLabel.setText("No menu items available yet.");
                noPopularFoodLabel.setVisible(true);
            } else {
                noPopularFoodLabel.setVisible(false);
                int max = Math.min(items.size(), 8);
                for (int i = 0; i < max; i++) {
                    popularFoodPane.getChildren().add(makeFoodCard(items.get(i)));
                }
            }
        } catch (SQLException e) {
            noPopularRestLabel.setText("Could not load data.");
            noPopularRestLabel.setVisible(true);
        }
    }

    private VBox makeRestCard(Restaurant r) {
        VBox card = new VBox(6);
        card.setPrefWidth(180);
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #E5E7EB;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 14 14 12 14;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);"
        );

        Label placeholder = new Label("Restaurant");
        placeholder.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #FF7518;");
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setMaxWidth(Double.MAX_VALUE);

        Label name = new Label(r.getRestaurantName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1C3A5E; -fx-wrap-text: true;");
        name.setMaxWidth(160);

        Label cuisine = new Label(r.getCuisineType() != null ? r.getCuisineType() : "");
        cuisine.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        Label rating = new Label("Rating " + String.format("%.1f", r.getRating()));
        rating.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151;");

        Button order = new Button("Order Now");
        order.setMaxWidth(Double.MAX_VALUE);
        order.setStyle(
                "-fx-background-color: #FF7518; -fx-text-fill: white;" +
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 0;"
        );
        order.setOnAction(e -> {
            selectedRest = r;
            loadMenuForRestaurant(r);
            navToSearch();
        });

        card.getChildren().addAll(placeholder, name, cuisine, rating, order);
        return card;
    }

    private VBox makeFoodCard(MenuItem m) {
        VBox card = new VBox(6);
        card.setPrefWidth(160);
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #E5E7EB;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 12 12 10 12;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);"
        );

        Label category = new Label(m.getCategory() != null ? m.getCategory() : "Food Item");
        category.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #FF7518;");

        Label name = new Label(m.getName());
        name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151; -fx-wrap-text: true;");
        name.setMaxWidth(140);

        Label price = new Label(m.getFormattedPrice());
        price.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1C3A5E;");

        Button add = new Button("Add to Cart");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setStyle(
                "-fx-background-color: #15803D; -fx-text-fill: white;" +
                "-fx-font-size: 10px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 0;"
        );
        add.setOnAction(e -> {
            addItemToCart(m);
            cartStatus.setText(m.getName() + " added.");
        });

        card.getChildren().addAll(category, name, price, add);
        return card;
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String kw = searchField.getText().trim();
        searchResultsPane.getChildren().clear();
        notFoundLabel.setVisible(false);

        if (kw.isEmpty()) {
            searchStatusLabel.setText("Type something to search...");
            return;
        }

        try {
            if (searchRestaurantRadio.isSelected()) {
                List<Restaurant> found = restaurantDAO.searchByName(kw);
                if (found.isEmpty()) {
                    notFoundLabel.setVisible(true);
                    searchStatusLabel.setText("");
                } else {
                    searchStatusLabel.setText("Found " + found.size() + " restaurant(s) for \"" + kw + "\"");
                    for (Restaurant r : found) {
                        searchResultsPane.getChildren().add(makeRestCard(r));
                    }
                }
            } else {
                List<MenuItem> allItems = menuItemDAO.searchByName(kw);
                if (allItems.isEmpty()) {
                    notFoundLabel.setVisible(true);
                    searchStatusLabel.setText("");
                } else {
                    searchStatusLabel.setText("Found " + allItems.size() + " item(s) for \"" + kw + "\"");
                    for (MenuItem m : allItems) {
                        searchResultsPane.getChildren().add(makeFoodCard(m));
                    }
                }
            }
        } catch (SQLException e) {
            searchStatusLabel.setText("Search failed. Try again.");
        }
    }

    private void loadMenuForRestaurant(Restaurant r) {
        try {
            List<MenuItem> currentMenu = menuItemDAO.getAvailable(r.getRestaurantID());
            searchResultsPane.getChildren().clear();
            notFoundLabel.setVisible(false);
            searchStatusLabel.setText("Menu for " + r.getRestaurantName() + " - " + currentMenu.size() + " items");
            for (MenuItem m : currentMenu) {
                searchResultsPane.getChildren().add(makeFoodCard(m));
            }
        } catch (SQLException e) {
            searchStatusLabel.setText("Could not load menu.");
        }
    }

    private void addItemToCart(MenuItem m) {
        if (selectedRest == null) {
            try {
                selectedRest = restaurantDAO.getById(m.getRestaurantID());
            } catch (SQLException ignored) {
            }
        }

        for (OrderItem existing : cart) {
            if (existing.getMenuItemID() == m.getItemID()) {
                existing.setQuantity(existing.getQuantity() + 1);
                refreshCart();
                return;
            }
        }
        cart.add(new OrderItem(m.getItemID(), m.getName(), m.getPrice(), 1));
        refreshCart();
    }

    private void refreshCart() {
        List<String> display = new ArrayList<>();
        for (OrderItem i : cart) {
            display.add(i.getSummary());
        }
        cartListView.setItems(FXCollections.observableArrayList(display));
        pricingArea.setText(pricingService.breakdown(cart));
    }

    @FXML
    private void handleRemoveFromCart(ActionEvent event) {
        int idx = cartListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= cart.size()) return;
        cart.remove(idx);
        refreshCart();
        cartStatus.setText("Item removed.");
    }

    @FXML
    private void handlePlaceOrder(ActionEvent event) {
        if (cart.isEmpty()) {
            cartStatus.setText("Your cart is empty.");
            return;
        }
        String address = addressField.getText().trim();
        if (address.isEmpty()) {
            cartStatus.setText("Please enter a delivery address.");
            return;
        }
        if (selectedRest == null) {
            cartStatus.setText("No restaurant selected.");
            return;
        }

        try {
            Order order = new Order(me.getUserID(), selectedRest.getRestaurantID(),
                    address, pricingService.getDeliveryFee());
            order.setItems(new ArrayList<>(cart));
            int newID = orderDAO.createOrder(order);
            if (newID > 0) {
                showPopup("Order Placed!",
                        "Order #" + newID + " placed.\nTotal: NPR "
                                + String.format("%.2f", pricingService.orderTotal(cart)));
                cart.clear();
                refreshCart();
                addressField.clear();
                cartStatus.setText("Order #" + newID + " placed.");
                loadHistory();
                loadFeedbackPage();
            }
        } catch (SQLException e) {
            cartStatus.setText("Failed to place order.");
        }
    }

    private void loadHistory() {
        try {
            List<Order> orders = orderDAO.getByCustomer(me.getUserID());
            if (orders.isEmpty()) {
                historyListView.setVisible(false);
                emptyHistoryBox.setVisible(true);
            } else {
                emptyHistoryBox.setVisible(false);
                historyListView.setVisible(true);
                List<String> display = new ArrayList<>();
                for (Order o : orders) {
                    String restaurantName = getRestaurantName(o.getRestaurantID());
                    display.add("Order #" + o.getOrderID()
                            + " | " + restaurantName
                            + " | " + o.getStatus()
                            + " | NPR " + String.format("%.2f", o.getTotalAmount())
                            + " | " + o.getDeliveryAddress());
                }
                historyListView.setItems(FXCollections.observableArrayList(display));
            }
        } catch (SQLException e) {
            emptyHistoryBox.setVisible(false);
            historyListView.setVisible(true);
            historyListView.setItems(FXCollections.observableArrayList("Could not load history."));
        }
    }

    @FXML
    private void handleRefreshHistory(ActionEvent event) {
        loadHistory();
    }

    private void loadProfileFields() {
        profileNameField.setText(me.getFullName());
        profilePhoneField.setText(me.getPhone() != null ? me.getPhone() : "");
        currentEmailDisplay.setText(me.getEmail());
        profileWelcomeLabel.setText("Welcome, " + me.getFullName() + "!");
        nameStatus.setText("");
        emailStatus.setText("");
        passStatus.setText("");
    }

    @FXML
    private void handleSaveName(ActionEvent event) {
        String name = profileNameField.getText().trim();
        if (name.isEmpty()) {
            nameStatus.setText("Name cannot be empty.");
            return;
        }
        me.setFullName(name);
        me.setPhone(profilePhoneField.getText().trim());
        userDAO.updateUser(me);
        profileWelcomeLabel.setText("Welcome, " + name + "!");
        nameStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #15803D;");
        nameStatus.setText("Name and phone updated.");
    }

    @FXML
    private void handleChangeEmail(ActionEvent event) {
        String newEmail = newEmailField.getText().trim();
        String pass = emailConfirmPassField.getText();
        if (newEmail.isEmpty() || pass.isEmpty()) {
            setEmailStatus("All fields are required.", false);
            return;
        }
        if (!pass.equals(me.getPassword())) {
            setEmailStatus("Incorrect current password.", false);
            return;
        }
        if (!newEmail.contains("@")) {
            setEmailStatus("Invalid email format.", false);
            return;
        }
        me.setEmail(newEmail);
        userDAO.updateUser(me);
        currentEmailDisplay.setText(newEmail);
        newEmailField.clear();
        emailConfirmPassField.clear();
        setEmailStatus("Email updated successfully.", true);
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        String current = currentPassField.getText();
        String newPass = newPassField.getText();
        if (current.isEmpty() || newPass.isEmpty()) {
            setPassStatus("All fields are required.", false);
            return;
        }
        if (!current.equals(me.getPassword())) {
            setPassStatus("Current password is incorrect.", false);
            return;
        }
        if (newPass.length() < 4) {
            setPassStatus("New password must be at least 4 characters.", false);
            return;
        }
        me.setPassword(newPass);
        userDAO.updateUser(me);
        currentPassField.clear();
        newPassField.clear();
        setPassStatus("Password changed successfully.", true);
    }

    private void loadFeedbackPage() {
        feedbackItemsBox.getChildren().clear();
        feedbackPageStatus.setText("");

        try {
            List<Order> orders = orderDAO.getByCustomer(me.getUserID());
            Map<Integer, FeedbackEntry> feedbackMap = feedbackDAO.getByCustomer(me.getUserID());

            if (orders.isEmpty()) {
                emptyFeedbackBox.setVisible(true);
                return;
            }

            emptyFeedbackBox.setVisible(false);
            int itemCount = 0;
            int feedbackCount = 0;

            for (Order order : orders) {
                String restaurantName = getRestaurantName(order.getRestaurantID());
                for (OrderItem item : order.getItems()) {
                    FeedbackEntry existing = feedbackMap.get(item.getOrderItemID());
                    if (existing != null) {
                        feedbackCount++;
                    }
                    feedbackItemsBox.getChildren().add(
                            makeFeedbackCard(order, restaurantName, item, existing)
                    );
                    itemCount++;
                }
            }

            feedbackPageStatus.setText(
                    "Showing " + itemCount + " ordered item(s). " +
                    feedbackCount + " already have feedback saved."
            );
        } catch (SQLException e) {
            emptyFeedbackBox.setVisible(false);
            feedbackPageStatus.setText("Could not load feedback history.");
        }
    }

    private VBox makeFeedbackCard(Order order, String restaurantName,
            OrderItem item, FeedbackEntry existing) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #E5E7EB;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 18 20;"
        );

        Label restaurantLabel = new Label(restaurantName);
        restaurantLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1C3A5E;");

        Label orderLabel = new Label("Order #" + order.getOrderID() + " | " + order.getStatus());
        orderLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        Label itemLabel = new Label(item.getItemName());
        itemLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        Label itemMeta = new Label(
                "Quantity: " + item.getQuantity() +
                " | Price: NPR " + String.format("%.2f", item.getSubtotal())
        );
        itemMeta.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        Label addressLabel = new Label("Delivery: " + order.getDeliveryAddress());
        addressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        card.getChildren().addAll(restaurantLabel, orderLabel, itemLabel, itemMeta, addressLabel);

        if (existing != null) {
            Region divider = new Region();
            divider.setPrefHeight(1);
            divider.setStyle("-fx-background-color: #E5E7EB;");

            Label feedbackTitle = new Label("Saved Feedback");
            feedbackTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FF7A00;");

            Label ratingLabel = new Label("Rating: " + existing.getRating() + "/5");
            ratingLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1C3A5E;");

            String comment = existing.getComment() == null || existing.getComment().isBlank()
                    ? "No written comment provided."
                    : existing.getComment();
            Label commentLabel = new Label(comment);
            commentLabel.setWrapText(true);
            commentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");

            card.getChildren().addAll(divider, feedbackTitle, ratingLabel, commentLabel);
            return card;
        }

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #E5E7EB;");

        Label noFeedback = new Label("No feedback saved yet for this item.");
        noFeedback.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        Button leaveFeedback = new Button("Leave Feedback");
        leaveFeedback.setStyle(ORANGE_BUTTON + "-fx-padding: 10 18;");

        VBox formBox = new VBox(10);
        formBox.setVisible(false);
        formBox.setManaged(false);
        formBox.setStyle(
                "-fx-background-color: #FFF7ED;" +
                "-fx-border-color: #FED7AA;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 14;"
        );

        ComboBox<Integer> ratingBox = new ComboBox<>();
        ratingBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingBox.setValue(5);
        ratingBox.setPrefWidth(120);

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Share your feedback for this food item...");
        commentArea.setPrefRowCount(3);
        commentArea.setWrapText(true);

        Label formStatus = new Label();
        formStatus.setWrapText(true);
        formStatus.setStyle("-fx-font-size: 11px;");

        Button saveFeedback = new Button("Save Feedback");
        saveFeedback.setStyle(ORANGE_BUTTON + "-fx-padding: 10 18;");
        saveFeedback.setOnAction(e -> saveFeedback(order, item, ratingBox, commentArea, formStatus));

        formBox.getChildren().addAll(
                new Label("Rating"),
                ratingBox,
                new Label("Comment"),
                commentArea,
                saveFeedback,
                formStatus
        );

        leaveFeedback.setOnAction(e -> {
            formBox.setManaged(true);
            formBox.setVisible(true);
            leaveFeedback.setDisable(true);
        });

        card.getChildren().addAll(divider, noFeedback, leaveFeedback, formBox);
        return card;
    }

    private void saveFeedback(Order order, OrderItem item, ComboBox<Integer> ratingBox,
            TextArea commentArea, Label formStatus) {
        Integer rating = ratingBox.getValue();
        if (rating == null) {
            formStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
            formStatus.setText("Please choose a rating.");
            return;
        }

        try {
            feedbackDAO.saveOrUpdate(
                    me.getUserID(),
                    order.getRestaurantID(),
                    item.getOrderItemID(),
                    rating,
                    commentArea.getText().trim()
            );
            loadFeedbackPage();
        } catch (SQLException e) {
            formStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
            formStatus.setText("Could not save feedback.");
        }
    }

    private String getRestaurantName(int restaurantID) {
        try {
            Restaurant restaurant = restaurantDAO.getById(restaurantID);
            if (restaurant != null && restaurant.getRestaurantName() != null) {
                return restaurant.getRestaurantName();
            }
        } catch (SQLException ignored) {
        }
        return "Restaurant #" + restaurantID;
    }

    private void setEmailStatus(String msg, boolean ok) {
        emailStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (ok ? "#15803D" : "#DC2626") + ";");
        emailStatus.setText(msg);
    }

    private void setPassStatus(String msg, boolean ok) {
        passStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (ok ? "#15803D" : "#DC2626") + ";");
        passStatus.setText(msg);
    }

    private void showPopup(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/fooddelivery/views/LogIn.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Food Delivery - Login");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
