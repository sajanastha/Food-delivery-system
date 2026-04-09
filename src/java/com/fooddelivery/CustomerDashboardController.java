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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class CustomerDashboardController {

    private static final String ACTIVE_TAB_STYLE =
            "-fx-background-color: transparent;"
                    + "-fx-text-fill: #F97316;"
                    + "-fx-font-size: 13px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-padding: 10 18;"
                    + "-fx-cursor: hand;"
                    + "-fx-border-color: transparent transparent #F97316 transparent;"
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
    @FXML private Button homeTabButton;
    @FXML private Button orderTabButton;
    @FXML private Button historyTabButton;
    @FXML private Button profileTabButton;
    @FXML private Button feedbackTabButton;

    @FXML private VBox homePanel;
    @FXML private Label homeDateLabel;
    @FXML private Label homeStatusLabel;
    @FXML private HBox topRestaurantsBox;
    @FXML private HBox topFoodsBox;
    @FXML private HBox recommendationsBox;
    @FXML private Label recommendSubLabel;

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

    @FXML private VBox myFeedbackPanel;
    @FXML private ListView<String> myFeedbackListView;
    @FXML private Label feedbackCountLabel;
    @FXML private Label myFeedbackStatus;

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
        setActiveTab(homeTabButton);
        homePanel.setVisible(true);
        homePanel.setManaged(true);
        orderPanel.setVisible(false);
        orderPanel.setManaged(false);
        historyPanel.setVisible(false);
        historyPanel.setManaged(false);
        profilePanel.setVisible(false);
        profilePanel.setManaged(false);
        myFeedbackPanel.setVisible(false);
        myFeedbackPanel.setManaged(false);
        loadHomeInsights();
    }

    @FXML
    public void showOrderTab(ActionEvent event) {
        setActiveTab(orderTabButton);
        homePanel.setVisible(false);
        homePanel.setManaged(false);
        orderPanel.setVisible(true);
        orderPanel.setManaged(true);
        historyPanel.setVisible(false);
        historyPanel.setManaged(false);
        profilePanel.setVisible(false);
        profilePanel.setManaged(false);
        myFeedbackPanel.setVisible(false);
        myFeedbackPanel.setManaged(false);
    }

    @FXML
    public void showHistoryTab(ActionEvent event) {
        setActiveTab(historyTabButton);
        homePanel.setVisible(false);
        homePanel.setManaged(false);
        orderPanel.setVisible(false);
        orderPanel.setManaged(false);
        historyPanel.setVisible(true);
        historyPanel.setManaged(true);
        profilePanel.setVisible(false);
        profilePanel.setManaged(false);
        myFeedbackPanel.setVisible(false);
        myFeedbackPanel.setManaged(false);
        loadHistory();
    }

    @FXML
    public void showProfile(ActionEvent event) {
        setActiveTab(profileTabButton);
        homePanel.setVisible(false);
        homePanel.setManaged(false);
        orderPanel.setVisible(false);
        orderPanel.setManaged(false);
        historyPanel.setVisible(false);
        historyPanel.setManaged(false);
        profilePanel.setVisible(true);
        profilePanel.setManaged(true);
        myFeedbackPanel.setVisible(false);
        myFeedbackPanel.setManaged(false);
        loadProfile();
    }

    @FXML
    public void showFeedbackTab(ActionEvent event) {
        setActiveTab(feedbackTabButton);
        homePanel.setVisible(false);
        homePanel.setManaged(false);
        orderPanel.setVisible(false);
        orderPanel.setManaged(false);
        historyPanel.setVisible(false);
        historyPanel.setManaged(false);
        profilePanel.setVisible(false);
        profilePanel.setManaged(false);
        myFeedbackPanel.setVisible(true);
        myFeedbackPanel.setManaged(true);
        loadMyFeedback();
    }

    private void loadMyFeedback() {
        try {
            List<FeedbackEntry> entries = feedbackDAO.getByCustomer(me.getUserID());
            if (entries.isEmpty()) {
                myFeedbackListView.setItems(FXCollections.observableArrayList(
                        "You haven't submitted any feedback yet."));
                feedbackCountLabel.setText("0 feedback(s)");
                myFeedbackStatus.setText("");
                return;
            }
            List<String> display = new ArrayList<>();
            for (FeedbackEntry e : entries) {
                String stars = "★".repeat(e.getRating()) + "☆".repeat(5 - e.getRating());
                String restName = getRestaurantName(e.getRestaurantID());
                String comment = (e.getComment() == null || e.getComment().isBlank())
                        ? "(no comment)" : e.getComment();
                String date = e.getCreatedAt() != null && e.getCreatedAt().length() >= 10
                        ? e.getCreatedAt().substring(0, 10) : "";
                display.add(stars + "  " + restName
                        + "  |  Order #" + e.getOrderItemID()
                        + "  |  " + comment
                        + (date.isEmpty() ? "" : "  [" + date + "]"));
            }
            myFeedbackListView.setItems(FXCollections.observableArrayList(display));
            feedbackCountLabel.setText(entries.size() + " feedback(s)");
            myFeedbackStatus.setText("");
        } catch (SQLException ex) {
            myFeedbackListView.setItems(FXCollections.observableArrayList("Could not load feedback."));
            myFeedbackStatus.setText("Error loading feedback.");
        }
    }

    private void loadHomeInsights() {
        homeDateLabel.setText("Today: " + LocalDate.now());
        homeStatusLabel.setText("Top picks based on today's orders.");

        try {
            // ── Restaurant Near You (top 4 by today's orders) ────────────────
            topRestaurantsBox.getChildren().clear();
            List<String> topRestNames = orderDAO.getTopRestaurantsForToday(4);
            List<Restaurant> allRests = restaurantDAO.getAll();

            if (topRestNames.isEmpty()) {
                // fallback: show all restaurants randomly up to 4
                Collections.shuffle(allRests, new Random());
                for (Restaurant r : allRests.subList(0, Math.min(4, allRests.size())))
                    topRestaurantsBox.getChildren().add(makeRestCard(r));
            } else {
                for (String entry : topRestNames) {
                    String name = entry.contains(" - ") ? entry.split(" - ")[0].trim() : entry;
                    for (Restaurant r : allRests) {
                        if (r.getRestaurantName().equalsIgnoreCase(name)) {
                            topRestaurantsBox.getChildren().add(makeRestCard(r));
                            break;
                        }
                    }
                }
            }

            // ── Top Popular Foods (top 4 by today's orders) ──────────────────
            topFoodsBox.getChildren().clear();
            List<String> topFoodNames = orderDAO.getTopFoodsForToday(4);

            if (topFoodNames.isEmpty()) {
                // fallback: random items from all menus
                List<MenuItem> all = menuItemDAO.getAllMenuItems();
                Collections.shuffle(all, new Random());
                for (MenuItem mi : all.subList(0, Math.min(4, all.size())))
                    topFoodsBox.getChildren().add(makeFoodCard(mi.getName(),
                            "NPR " + String.format("%.0f", mi.getPrice()), mi.getCategory()));
            } else {
                for (String entry : topFoodNames) {
                    String name = entry.contains(" - ") ? entry.split(" - ")[0].trim() : entry;
                    topFoodsBox.getChildren().add(makeFoodCard(name, "", ""));
                }
            }

            // ── Recommendations ───────────────────────────────────────────────
            loadRecommendations(allRests);

        } catch (SQLException e) {
            homeStatusLabel.setText("Could not load data.");
        }
    }

    private void loadRecommendations(List<Restaurant> allRests) throws SQLException {
        recommendationsBox.getChildren().clear();
        Customer me = (Customer) SessionManager.getInstance().getCurrentUser();
        List<Order> history = orderDAO.getByCustomer(me.getUserID());

        List<MenuItem> candidates = new ArrayList<>();

        if (history.isEmpty()) {
            // No history → random items across all restaurants
            recommendSubLabel.setText("  Discover something new!");
            List<MenuItem> all = menuItemDAO.getAllMenuItems();
            Collections.shuffle(all, new Random());
            candidates = all.subList(0, Math.min(4, all.size()));
        } else {
            recommendSubLabel.setText("  Based on your order history");

            // Collect categories the customer has ordered
            Set<String> likedCategories = new LinkedHashSet<>();
            Set<Integer> orderedRestaurantIDs = new LinkedHashSet<>();
            for (Order o : history) {
                orderedRestaurantIDs.add(o.getRestaurantID());
                for (OrderItem oi : o.getItems()) {
                    // Try to find this item's category from the menu
                    for (Restaurant r : allRests) {
                        try {
                            for (MenuItem mi : menuItemDAO.getAvailable(r.getRestaurantID())) {
                                if (mi.getName().equalsIgnoreCase(oi.getItemName()))
                                    likedCategories.add(mi.getCategory());
                            }
                        } catch (SQLException ignored) {}
                    }
                }
            }

            // Find items in liked categories from DIFFERENT restaurants, or same rest different items
            Set<String> alreadyOrdered = new LinkedHashSet<>();
            for (Order o : history)
                for (OrderItem oi : o.getItems())
                    alreadyOrdered.add(oi.getItemName().toLowerCase());

            for (Restaurant r : allRests) {
                for (MenuItem mi : menuItemDAO.getAvailable(r.getRestaurantID())) {
                    if (alreadyOrdered.contains(mi.getName().toLowerCase())) continue;
                    boolean sameCategory = likedCategories.isEmpty()
                            || likedCategories.contains(mi.getCategory());
                    boolean diffRest = !orderedRestaurantIDs.contains(r.getRestaurantID());
                    if (sameCategory && (diffRest || likedCategories.contains(mi.getCategory())))
                        candidates.add(mi);
                }
            }

            // shuffle and cap at 4; if still empty fall back to random
            Collections.shuffle(candidates, new Random());
            if (candidates.isEmpty()) {
                List<MenuItem> all = menuItemDAO.getAllMenuItems();
                Collections.shuffle(all, new Random());
                candidates = all.subList(0, Math.min(4, all.size()));
            } else {
                candidates = candidates.subList(0, Math.min(4, candidates.size()));
            }
        }

        for (MenuItem mi : candidates) {
            // Find restaurant name for this item
            String restName = "";
            for (Restaurant r : allRests)
                if (r.getRestaurantID() == mi.getRestaurantID()) {
                    restName = r.getRestaurantName();
                    break;
                }
            recommendationsBox.getChildren().add(
                    makeFoodCard(mi.getName(),
                            "NPR " + String.format("%.0f", mi.getPrice()),
                            restName));
        }
    }

    /** Rectangular restaurant card */
    private VBox makeRestCard(Restaurant r) {
        VBox card = new VBox(8);
        card.setPrefWidth(180);
        card.setMinWidth(160);
        card.setMaxWidth(220);
        card.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #e2e8f0;"
                + "-fx-border-radius: 14;"
                + "-fx-border-width: 1;"
                + "-fx-padding: 0 0 14 0;"
                + "-fx-cursor: hand;");

        // Branded top panel instead of a plain gradient
        StackPane banner = new StackPane();
        banner.setPrefHeight(90);
        banner.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #fde8d8, #ffd7c2);"
                + "-fx-background-radius: 14 14 0 0;");
        Label brandLabel = new Label("So Yummy");
        brandLabel.setStyle(
                "-fx-font-size: 18px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #c85a22;");
        banner.getChildren().add(brandLabel);

        Label name = new Label(r.getRestaurantName());
        name.setWrapText(true);
        name.setStyle(
                "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #F97316;"
                + "-fx-padding: 10 12 2 12;");

        Label cuisine = new Label(r.getCuisineType() != null ? r.getCuisineType() : "");
        cuisine.setStyle(
                "-fx-font-size: 11px;"
                + "-fx-text-fill: #64748B;"
                + "-fx-padding: 0 12 0 12;");

        card.getChildren().addAll(banner, name, cuisine);
        return card;
    }

    /** Rectangular food/item card */
    private VBox makeFoodCard(String itemName, String price, String subtitle) {
        VBox card = new VBox(8);
        card.setPrefWidth(180);
        card.setMinWidth(160);
        card.setMaxWidth(220);
        card.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #e2e8f0;"
                + "-fx-border-radius: 14;"
                + "-fx-border-width: 1;"
                + "-fx-padding: 0 0 14 0;"
                + "-fx-cursor: hand;");

        // Branded top panel instead of a plain gradient
        StackPane banner = new StackPane();
        banner.setPrefHeight(90);
        banner.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #d9f7e1, #b8f0d1);"
                + "-fx-background-radius: 14 14 0 0;");
        Label brandLabel = new Label("So Yummy");
        brandLabel.setStyle(
                "-fx-font-size: 18px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #167f4f;");
        banner.getChildren().add(brandLabel);

        Label name = new Label(itemName);
        name.setWrapText(true);
        name.setStyle(
                "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #F97316;"
                + "-fx-padding: 10 12 2 12;");

        if (!price.isEmpty()) {
            Label priceLabel = new Label(price);
            priceLabel.setStyle(
                    "-fx-font-size: 12px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #16a34a;"
                    + "-fx-padding: 0 12 0 12;");
            card.getChildren().addAll(banner, name, priceLabel);
        } else {
            card.getChildren().addAll(banner, name);
        }

        if (!subtitle.isEmpty()) {
            Label sub = new Label(subtitle);
            sub.setStyle(
                    "-fx-font-size: 11px;"
                    + "-fx-text-fill: #64748B;"
                    + "-fx-padding: 0 12 0 12;");
            card.getChildren().add(sub);
        }

        return card;
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
        if (idx < 0 || idx >= allRestaurants.size()) return;

        selectedRest = allRestaurants.get(idx);
        restaurantInfoLabel.setText(selectedRest.getAddress()
                + "  ·  Hours: " + selectedRest.getOperatingHours());
        menuTitleLabel.setText(selectedRest.getRestaurantName() + " — Menu");

        try {
            List<MenuItem> raw =
                menuItemDAO.getAvailable(selectedRest.getRestaurantID());

            // Sort by category order, then alphabetically within each
            raw.sort((a, b) -> {
                int ai = categoryRank(a.getCategory());
                int bi = categoryRank(b.getCategory());
                if (ai != bi) return ai - bi;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            // Build display list with category headers
            currentMenu = new ArrayList<>();
            List<String> display = new ArrayList<>();
            String lastCat = "";
            for (MenuItem m : raw) {
                String cat = m.getCategory() == null
                        ? "Other" : m.getCategory();
                if (!cat.equalsIgnoreCase(lastCat)) {
                    display.add("\u2500\u2500\u2500 "
                            + cat.toUpperCase() + " \u2500\u2500\u2500");
                    currentMenu.add(null); // header placeholder
                    lastCat = cat;
                }
                currentMenu.add(m);
                display.add("    " + m.getName()
                        + "   NPR " + String.format("%.2f", m.getPrice()));
            }

            menuListView.setItems(
                FXCollections.observableArrayList(display));
            selectedItemLabel.setText("");
        } catch (SQLException ex) {
            browseStatus.setText("Could not load menu.");
        }
    }

    // Same category order as the restaurant portal
    private int categoryRank(String cat) {
        if (cat == null) return 99;
        return switch (cat.toLowerCase().trim()) {
            case "starter"  -> 0;
            case "main"     -> 1;
            case "drink"    -> 2;
            case "desert", "dessert" -> 3;
            default         -> 4;
        };
    }

    @FXML
    private void handleAddToCart(ActionEvent event) {
        int idx = menuListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= currentMenu.size()) {
            browseStatus.setText("Select a menu item first.");
            return;
        }

        MenuItem item = currentMenu.get(idx);
        if (item == null) {  // clicked a category header — ignore
            menuListView.getSelectionModel().clearSelection();
            browseStatus.setText("Select a specific menu item.");
            return;
        }
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
        popup.setMinWidth(500);
        popup.setMinHeight(440);

        VBox root = new VBox(14);
        root.setStyle("-fx-padding: 26;"
                + "-fx-background-color: white;");

        // Title
        Label title = new Label(
            "Feedback for Order #" + order.getOrderID());
        title.setStyle("-fx-font-size: 17px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #1E293B;");

        // Subtitle — restaurant + items
        Label subtitle = new Label(
            getRestaurantName(order.getRestaurantID())
            + "  ·  " + firstItemsSummary(order));
        subtitle.setStyle("-fx-font-size: 12px;"
                + "-fx-text-fill: #64748B;");

        // Divider
        Separator sep = new Separator();

        // Rating label
        Label ratingLbl = new Label("Your Rating");
        ratingLbl.setStyle("-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #334155;");

        // Star buttons — use an int[] to track selection
        // so clicking same star twice keeps it selected
        final int[] selectedRating = {0};

        HBox stars = new HBox(8);
        List<Button> starButtons = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            final int starVal = i;
            Button starBtn = new Button("★");
            starBtn.setStyle(unselectedStarStyle());
            starBtn.setUserData(starVal);
            starBtn.setOnAction(ev -> {
                selectedRating[0] = starVal;
                // Update all button styles
                for (Button b : starButtons) {
                    int bVal = (int) b.getUserData();
                    b.setStyle(bVal <= starVal
                        ? selectedStarStyle()
                        : unselectedStarStyle());
                }
            });
            starButtons.add(starBtn);
            stars.getChildren().add(starBtn);
        }

        // Comment
        Label commentLbl = new Label("Comment  (optional)");
        commentLbl.setStyle("-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #334155;");

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Tell the restaurant what you thought...");
        commentArea.setPrefRowCount(4);
        commentArea.setWrapText(true);
        commentArea.setStyle("-fx-border-color: #CBD5E1;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-font-size: 13px;");

        // Status label (errors)
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;"
                + "-fx-text-fill: #DC2626;");

        // Pre-fill if feedback already exists
        try {
            FeedbackEntry existing = feedbackDAO
                    .getByCustomerAndOrder(
                        me.getUserID(), order.getOrderID());
            if (existing != null) {
                commentArea.setText(existing.getComment());
                selectedRating[0] = existing.getRating();
                for (Button b : starButtons) {
                    int bVal = (int) b.getUserData();
                    b.setStyle(bVal <= existing.getRating()
                        ? selectedStarStyle()
                        : unselectedStarStyle());
                }
            }
        } catch (SQLException ignored) {}

        // Buttons
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #E2E8F0;"
                + "-fx-text-fill: #334155;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 9 20;"
                + "-fx-cursor: hand;");
        closeBtn.setOnAction(ev -> popup.close());

        Button saveBtn = new Button("Save Feedback");
        saveBtn.setStyle("-fx-background-color: #FF7518;"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 9 20;"
                + "-fx-cursor: hand;");
        HBox.setHgrow(saveBtn, Priority.ALWAYS);
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(ev -> {
            if (selectedRating[0] == 0) {
                statusLabel.setText("Please click a star to select a rating.");
                return;
            }
            String comment = commentArea.getText().trim();
            try {
                feedbackDAO.saveOrUpdate(
                    me.getUserID(),
                    order.getRestaurantID(),
                    order.getOrderID(),
                    selectedRating[0],
                    comment);
                // Success — close popup and refresh everything
                popup.close();
                updateFeedbackButton();
                historyDetailArea.setText(buildHistoryDetails(order));
                loadMyFeedback(); // refresh customer Feedback tab immediately
                showAlert("Feedback Saved",
                    "Your " + selectedRating[0] + "-star review"
                    + " for Order #" + order.getOrderID()
                    + " has been saved.\nThank you!");
            } catch (SQLException e) {
                // Print full stack trace to console AND show the actual SQL error
                e.printStackTrace();
                statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626;");
                statusLabel.setText("Save failed: " + e.getMessage());
            }
        });

        HBox actions = new HBox(10, closeBtn, saveBtn);

        root.getChildren().addAll(
            title, subtitle, sep,
            ratingLbl, stars,
            commentLbl, commentArea,
            statusLabel, actions);

        popup.setScene(new Scene(root));
        popup.showAndWait();
    }

    // Star button styles
    private String selectedStarStyle() {
        return "-fx-background-color: #FF7518;"
             + "-fx-text-fill: white;"
             + "-fx-background-radius: 8;"
             + "-fx-font-size: 22px;"
             + "-fx-padding: 6 12;"
             + "-fx-cursor: hand;";
    }

    private String unselectedStarStyle() {
        return "-fx-background-color: #FFF0E0;"
             + "-fx-text-fill: #FF7518;"
             + "-fx-background-radius: 8;"
             + "-fx-font-size: 22px;"
             + "-fx-padding: 6 12;"
             + "-fx-cursor: hand;";
    }

    private void loadProfile() {
        profileNameLabel.setText("Name: " + me.getFullName());
        profileEmailLabel.setText("Email: " + me.getEmail());
        profilePhoneLabel.setText("Phone: "
                + (me.getPhone() == null || me.getPhone().isBlank() ? "Not provided" : me.getPhone()));
        profileRoleLabel.setText("Role: Customer");
    }

    private void setActiveTab(Button activeTab) {
        homeTabButton.setStyle(INACTIVE_TAB_STYLE);
        orderTabButton.setStyle(INACTIVE_TAB_STYLE);
        historyTabButton.setStyle(INACTIVE_TAB_STYLE);
        profileTabButton.setStyle(INACTIVE_TAB_STYLE);
        feedbackTabButton.setStyle(INACTIVE_TAB_STYLE);
        activeTab.setStyle(ACTIVE_TAB_STYLE);
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
            stage.setMaximized(false);
            stage.setScene(new Scene(root, 480, 520));
            stage.setResizable(false);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }   
}