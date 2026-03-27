package com.fooddelivery;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardController {

    // ── Top bar ───────────────────────────────────────────────────
    @FXML private Label welcomeLabel;

    // ── Pages ─────────────────────────────────────────────────────
    @FXML private ScrollPane homePage;
    @FXML private VBox        searchPage;
    @FXML private VBox        cartPage;
    @FXML private VBox        historyPage;
    @FXML private ScrollPane  profilePage;
    @FXML private VBox        feedbackPage;

    // ── Home ──────────────────────────────────────────────────────
    @FXML private FlowPane popularRestaurantsPane;
    @FXML private FlowPane popularFoodPane;
    @FXML private Label    noPopularRestLabel;
    @FXML private Label    noPopularFoodLabel;

    // ── Search ────────────────────────────────────────────────────
    @FXML private TextField     searchField;
    @FXML private ToggleGroup   searchTypeGroup;
    @FXML private RadioButton   searchRestaurantRadio;
    @FXML private RadioButton   searchFoodRadio;
    @FXML private Label         searchStatusLabel;
    @FXML private Label         notFoundLabel;
    @FXML private FlowPane      searchResultsPane;

    // ── Cart ──────────────────────────────────────────────────────
    @FXML private ListView<String> cartListView;
    @FXML private TextArea         pricingArea;
    @FXML private TextField        addressField;
    @FXML private Label            cartStatus;

    // ── History ───────────────────────────────────────────────────
    @FXML private ListView<String> historyListView;
    @FXML private VBox             emptyHistoryBox;

    // ── Profile ───────────────────────────────────────────────────
    @FXML private TextField     profileNameField;
    @FXML private TextField     profilePhoneField;
    @FXML private Label         nameStatus;
    @FXML private TextField     currentEmailDisplay;
    @FXML private TextField     newEmailField;
    @FXML private PasswordField emailConfirmPassField;
    @FXML private Label         emailStatus;
    @FXML private PasswordField currentPassField;
    @FXML private PasswordField newPassField;
    @FXML private Label         passStatus;
    @FXML private ListView<String> feedbackListView;
    @FXML private Label         feedbackStatus;

    // ── Feedback submit ───────────────────────────────────────────
    @FXML private ToggleGroup   starGroup;
    @FXML private RadioButton   star1, star2, star3, star4, star5;
    @FXML private TextArea      feedbackCommentField;
    @FXML private Label         feedbackSubmitStatus;

    // ── Bottom nav buttons ────────────────────────────────────────
    @FXML private Button navHome, navSearch, navHistory, navProfile, navFeedback;

    // ── DAOs ──────────────────────────────────────────────────────
    private final RestaurantDAO  restaurantDAO  = new RestaurantDAO();
    private final MenuItemDAO    menuItemDAO    = new MenuItemDAO();
    private final OrderDAO       orderDAO       = new OrderDAO();
    private final PricingService pricingService = new PricingService();
    private final UserDAO        userDAO        = new UserDAO();

    // ── State ─────────────────────────────────────────────────────
    private List<Restaurant> allRestaurants = new ArrayList<>();
    private List<MenuItem>   currentMenu    = new ArrayList<>();
    private List<OrderItem>  cart           = new ArrayList<>();
    private Restaurant       selectedRest;
    private Customer         me;

    // ── Colours ───────────────────────────────────────────────────
    private static final String ACTIVE_NAV =
        "-fx-background-color: #FFF7ED; -fx-text-fill: #FF7518; " +
        "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;";
    private static final String INACTIVE_NAV =
        "-fx-background-color: white; -fx-text-fill: #6B7280; " +
        "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;";

    // ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        me = (Customer) SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Welcome, " + me.getFullName() + "!");
        loadHomePopular();
        loadHistory();
        showPage(homePage);
        setActiveNav(navHome);
    }

    // ═══ PAGE SWITCHER ════════════════════════════════════════════
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

    @FXML private void navToHome()     { showPage(homePage);     setActiveNav(navHome);     loadHomePopular(); }
    @FXML private void navToSearch()   { showPage(searchPage);   setActiveNav(navSearch);   }
    @FXML private void navToHistory()  { showPage(historyPage);  setActiveNav(navHistory);  loadHistory(); }
    @FXML private void navToProfile()  { showPage(profilePage);  setActiveNav(navProfile);  loadProfileFields(); }
    @FXML private void navToFeedback() { showPage(feedbackPage); setActiveNav(navFeedback); }

    @FXML private void goToSearch() { navToSearch(); }

    // ═══ HOME — POPULAR ══════════════════════════════════════════
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

            // Popular food — show all available items
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

    // Card builder — Restaurant
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

        // Emoji placeholder (like the food pic in example)
        Label emoji = new Label("🍴");
        emoji.setStyle("-fx-font-size: 36px;");
        emoji.setAlignment(Pos.CENTER);
        emoji.setMaxWidth(Double.MAX_VALUE);

        Label name = new Label(r.getRestaurantName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1C3A5E; -fx-wrap-text: true;");
        name.setMaxWidth(160);

        Label cuisine = new Label(r.getCuisineType() != null ? r.getCuisineType() : "");
        cuisine.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        Label rating = new Label("⭐ " + String.format("%.1f", r.getRating()));
        rating.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151;");

        Button order = new Button("Order Now →");
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

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #FFF7ED;" +
            "-fx-border-color: #FF7518;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 14 14 12 14;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian,rgba(255,117,24,0.15),10,0,0,3);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #E5E7EB;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 14 14 12 14;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);"
        ));

        card.getChildren().addAll(emoji, name, cuisine, rating, order);
        return card;
    }

    // Card builder — Food Item
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

        String foodEmoji = switch (m.getCategory() != null ? m.getCategory().toLowerCase() : "") {
            case "appetizer"   -> "🥗";
            case "main course" -> "🍛";
            case "dessert"     -> "🍮";
            case "beverage"    -> "🥤";
            case "snack"       -> "🍟";
            default            -> "🍽️";
        };

        Label emoji = new Label(foodEmoji);
        emoji.setStyle("-fx-font-size: 32px;");
        emoji.setAlignment(Pos.CENTER);
        emoji.setMaxWidth(Double.MAX_VALUE);

        Label name = new Label(m.getName());
        name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151; -fx-wrap-text: true;");
        name.setMaxWidth(140);

        Label price = new Label(m.getFormattedPrice());
        price.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FF7518;");

        Label cat = new Label(m.getCategory() != null ? m.getCategory() : "");
        cat.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");

        Button add = new Button("+ Add to Cart");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setStyle(
            "-fx-background-color: #15803D; -fx-text-fill: white;" +
            "-fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 0;"
        );
        add.setOnAction(e -> {
            addItemToCart(m);
            cartStatus.setText("✓ " + m.getName() + " added!");
        });

        card.getChildren().addAll(emoji, name, price, cat, add);
        return card;
    }

    // ═══ SEARCH ═══════════════════════════════════════════════════
    @FXML
    private void handleSearch(ActionEvent event) {
        String kw = searchField.getText().trim();
        searchResultsPane.getChildren().clear();
        notFoundLabel.setVisible(false);

        if (kw.isEmpty()) {
            searchStatusLabel.setText("Type something to search...");
            return;
        }

        boolean isRestaurant = searchRestaurantRadio.isSelected();

        try {
            if (isRestaurant) {
                List<Restaurant> found = restaurantDAO.searchByName(kw);
                if (found.isEmpty()) {
                    notFoundLabel.setVisible(true);
                    searchStatusLabel.setText("");
                } else {
                    searchStatusLabel.setText("Found " + found.size() + " restaurant(s) for \"" + kw + "\"");
                    for (Restaurant r : found)
                        searchResultsPane.getChildren().add(makeRestCard(r));
                }
            } else {
                // Search food items across all restaurants
                List<MenuItem> allItems = menuItemDAO.searchByName(kw);
                if (allItems.isEmpty()) {
                    notFoundLabel.setVisible(true);
                    searchStatusLabel.setText("");
                } else {
                    searchStatusLabel.setText("Found " + allItems.size() + " item(s) for \"" + kw + "\"");
                    for (MenuItem m : allItems)
                        searchResultsPane.getChildren().add(makeFoodCard(m));
                }
            }
        } catch (SQLException e) {
            searchStatusLabel.setText("Search failed. Try again.");
        }
    }

    private void loadMenuForRestaurant(Restaurant r) {
        try {
            currentMenu = menuItemDAO.getAvailable(r.getRestaurantID());
            searchResultsPane.getChildren().clear();
            notFoundLabel.setVisible(false);
            searchStatusLabel.setText("Menu for " + r.getRestaurantName() + " — " + currentMenu.size() + " items");
            for (MenuItem m : currentMenu)
                searchResultsPane.getChildren().add(makeFoodCard(m));
        } catch (SQLException e) {
            searchStatusLabel.setText("Could not load menu.");
        }
    }

    // ═══ CART ═════════════════════════════════════════════════════
    private void addItemToCart(MenuItem m) {
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
        for (OrderItem i : cart) display.add(i.getSummary());
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
        if (cart.isEmpty()) { cartStatus.setText("Your cart is empty."); return; }
        String address = addressField.getText().trim();
        if (address.isEmpty()) { cartStatus.setText("Please enter a delivery address."); return; }
        if (selectedRest == null) { cartStatus.setText("No restaurant selected."); return; }

        try {
            Order order = new Order(me.getUserID(), selectedRest.getRestaurantID(),
                    address, pricingService.getDeliveryFee());
            order.setItems(new ArrayList<>(cart));
            int newID = orderDAO.createOrder(order);
            if (newID > 0) {
                showPopup("Order Placed! 🎉",
                        "Order #" + newID + " placed!\nTotal: NPR "
                        + String.format("%.2f", pricingService.orderTotal(cart)));
                cart.clear();
                refreshCart();
                addressField.clear();
                cartStatus.setText("✓ Order #" + newID + " placed!");
                loadHistory();
            }
        } catch (SQLException e) {
            cartStatus.setText("Failed to place order.");
        }
    }

    // ═══ ORDER HISTORY ════════════════════════════════════════════
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
                for (Order o : orders)
                    display.add("Order #" + o.getOrderID()
                            + "  |  " + o.getStatus()
                            + "  |  NPR " + String.format("%.2f", o.getTotalAmount())
                            + "  |  " + o.getDeliveryAddress());
                historyListView.setItems(FXCollections.observableArrayList(display));
            }
        } catch (SQLException e) {
            emptyHistoryBox.setVisible(false);
            historyListView.setVisible(true);
            historyListView.setItems(FXCollections.observableArrayList("Could not load history."));
        }
    }

    @FXML private void handleRefreshHistory(ActionEvent event) { loadHistory(); }

    // ═══ PROFILE ══════════════════════════════════════════════════
    private void loadProfileFields() {
        profileNameField.setText(me.getFullName());
        profilePhoneField.setText(me.getPhone() != null ? me.getPhone() : "");
        currentEmailDisplay.setText(me.getEmail());
        nameStatus.setText("");
        emailStatus.setText("");
        passStatus.setText("");
    }

    @FXML
    private void handleSaveName(ActionEvent event) {
        String name = profileNameField.getText().trim();
        if (name.isEmpty()) { nameStatus.setText("Name cannot be empty."); return; }
        me.setFullName(name);
        me.setPhone(profilePhoneField.getText().trim());
        userDAO.updateUser(me);
        welcomeLabel.setText("Welcome, " + name + "!");
        nameStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #15803D;");
        nameStatus.setText("✓ Name and phone updated!");
    }

    @FXML
    private void handleChangeEmail(ActionEvent event) {
        String newEmail = newEmailField.getText().trim();
        String pass     = emailConfirmPassField.getText();
        if (newEmail.isEmpty() || pass.isEmpty()) {
            setEmailStatus("All fields are required.", false); return;
        }
        if (!pass.equals(me.getPassword())) {
            setEmailStatus("⚠ Incorrect current password.", false); return;
        }
        if (!newEmail.contains("@")) {
            setEmailStatus("⚠ Invalid email format.", false); return;
        }
        me.setEmail(newEmail);
        userDAO.updateUser(me);
        currentEmailDisplay.setText(newEmail);
        newEmailField.clear();
        emailConfirmPassField.clear();
        setEmailStatus("✓ Email updated successfully!", true);
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        String current = currentPassField.getText();
        String newPass  = newPassField.getText();
        if (current.isEmpty() || newPass.isEmpty()) {
            setPassStatus("All fields are required.", false); return;
        }
        if (!current.equals(me.getPassword())) {
            setPassStatus("⚠ Current password is incorrect.", false); return;
        }
        if (newPass.length() < 4) {
            setPassStatus("⚠ New password must be at least 4 characters.", false); return;
        }
        me.setPassword(newPass);
        userDAO.updateUser(me);
        currentPassField.clear();
        newPassField.clear();
        setPassStatus("✓ Password changed successfully!", true);
    }

    @FXML
    private void handleViewFeedback(ActionEvent event) {
        // Show mock feedback history (in a real system would load from DB)
        List<String> fb = new ArrayList<>();
        fb.add("⭐⭐⭐⭐⭐  Great food, fast delivery! — Spice Garden");
        fb.add("⭐⭐⭐      Average experience — Pizza Palace");
        if (fb.isEmpty()) {
            feedbackStatus.setText("You have not left any feedback yet.");
        } else {
            feedbackListView.setItems(FXCollections.observableArrayList(fb));
            feedbackStatus.setText("Showing your " + fb.size() + " feedback(s).");
        }
    }

    // ═══ FEEDBACK SUBMIT ══════════════════════════════════════════
    @FXML
    private void handleSubmitFeedback(ActionEvent event) {
        RadioButton selected = (RadioButton) starGroup.getSelectedToggle();
        String comment = feedbackCommentField.getText().trim();
        if (selected == null) { feedbackSubmitStatus.setText("⚠ Please select a star rating."); return; }
        String stars = selected.getText().substring(0, selected.getText().indexOf(" "));
        feedbackSubmitStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #15803D;");
        feedbackSubmitStatus.setText("✓ Thank you! Your feedback (" + stars + ") has been submitted.");
        feedbackCommentField.clear();
    }

    // ═══ HELPERS ══════════════════════════════════════════════════
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
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/fooddelivery/views/LogIn.fxml"));
            Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Food Delivery — Login");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}