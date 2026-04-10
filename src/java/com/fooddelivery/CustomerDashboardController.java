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
    @FXML private Label historyStatusLabel;

    // Order detail card fields
    @FXML private Label  orderDetailPlaceholder;
    @FXML private VBox   orderMetaBox;
    @FXML private Label  detailOrderIdLabel;
    @FXML private Label  detailStatusLabel;
    @FXML private Label  detailAddressLabel;
    @FXML private Label  detailTotalLabel;
    @FXML private VBox   detailItemsBox;
    @FXML private Label  detailRestaurantLabel;
    @FXML private Button restaurantFeedbackBtn;
    @FXML private HBox   driverFeedbackRow;
    @FXML private Label  detailDriverLabel;
    @FXML private Button driverFeedbackBtn;

    @FXML private VBox profilePanel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profilePhoneLabel;
    @FXML private Label profileRoleLabel;

    @FXML private VBox myFeedbackPanel;
    @FXML private ListView<FeedbackEntry> myFeedbackListView;
    @FXML private Label feedbackCountLabel;
    @FXML private Label myFeedbackStatus;
    @FXML private Button showAllFeedbackBtn;

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PricingService pricingService = new PricingService();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final UserDAO userDAO = new UserDAO();

    private List<Restaurant> allRestaurants = new ArrayList<>();
    private List<MenuItem> currentMenu = new ArrayList<>();
    private List<OrderItem> cart = new ArrayList<>();
    private List<Order> historyOrders = new ArrayList<>();
    private Restaurant selectedRest;
    private Order selectedHistoryOrder;
    private Customer me;
    
    // Feedback filtering
    private Integer filteredFeedbackId = null; // When set, show only this feedback

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
        
        // Register button handlers programmatically (fallback for FXML handlers)
        if (restaurantFeedbackBtn != null) {
            restaurantFeedbackBtn.setOnAction(this::handleRestaurantFeedback);
        }
        if (driverFeedbackBtn != null) {
            driverFeedbackBtn.setOnAction(this::handleDriverFeedback);
        }
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
        showFeedbackTab(true);
    }

    private void showFeedbackTab(boolean clearFilter) {
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
        if (clearFilter) {
            filteredFeedbackId = null; // Clear filter when user navigates directly to Feedback tab
        }
        loadMyFeedback();
    }

    private void loadMyFeedback() {
        // Install card-style cell factory once
        myFeedbackListView.setCellFactory(
                lv -> new FeedbackCardCell(FeedbackCardCell.Mode.CUSTOMER));

        try {
            List<FeedbackEntry> entries = feedbackDAO.getByCustomer(me.getUserID());
            
            // Apply filter if set
            boolean isFiltered = false;
            if (filteredFeedbackId != null) {
                List<FeedbackEntry> filtered = entries.stream()
                    .filter(e -> e.getFeedbackID() == filteredFeedbackId)
                    .toList();
                if (!filtered.isEmpty()) {
                    entries = filtered;
                    isFiltered = true;
                } else {
                    filteredFeedbackId = null; // clear stale filter
                }
            }

            if (isFiltered) {
                feedbackCountLabel.setText("Showing 1 review");
                myFeedbackStatus.setText("Filtered view");
                // Show the "Show All" button
                if (showAllFeedbackBtn != null) {
                    showAllFeedbackBtn.setVisible(true);
                    showAllFeedbackBtn.setManaged(true);
                }
            } else {
                feedbackCountLabel.setText(entries.size() + " feedback(s)");
                myFeedbackStatus.setText("");
                // Hide the "Show All" button when not filtering
                if (showAllFeedbackBtn != null) {
                    showAllFeedbackBtn.setVisible(false);
                    showAllFeedbackBtn.setManaged(false);
                }
            }
            
            if (entries.isEmpty()) {
                myFeedbackListView.setItems(FXCollections.observableArrayList());
                if (filteredFeedbackId == null) {
                    myFeedbackStatus.setText("You haven't submitted any feedback yet.");
                }
                return;
            }
            // Enrich each entry: reuse customerName field to carry restaurant name
            for (FeedbackEntry e : entries) {
                String restName = getRestaurantName(e.getRestaurantID());
                e.setCustomerName(restName.isBlank() ? "Restaurant #" + e.getRestaurantID() : restName);
            }
            myFeedbackListView.setItems(FXCollections.observableArrayList(entries));
        } catch (SQLException ex) {
            myFeedbackListView.setItems(FXCollections.observableArrayList());
            myFeedbackStatus.setText("Error loading feedback: " + ex.getMessage());
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
            selectedHistoryOrder = null;
        } catch (SQLException ex) {
            historyListView.setItems(FXCollections.observableArrayList("Could not load order history."));
            historyStatusLabel.setText("History unavailable.");
        }
    }

    @FXML
    private void handleHistorySelected() {
        int idx = historyListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= historyOrders.size()) return;
        selectedHistoryOrder = historyOrders.get(idx);
        populateOrderDetailCard(selectedHistoryOrder);
    }

    private void populateOrderDetailCard(Order order) {
        // Show the meta block, hide placeholder
        orderDetailPlaceholder.setVisible(false);
        orderDetailPlaceholder.setManaged(false);
        orderMetaBox.setVisible(true);
        orderMetaBox.setManaged(true);

        // Basic info
        detailOrderIdLabel.setText("Order #" + order.getOrderID());
        String status = order.getStatus() != null ? order.getStatus().toString() : "—";
        detailStatusLabel.setText(status);
        // Status colour
        String statusColor = switch (status) {
            case "DELIVERED"  -> "#4d9078";
            case "CANCELLED"  -> "#c0392b";
            case "IN_DELIVERY"-> "#FF7518";
            default           -> "#888";
        };
        detailStatusLabel.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-background-color: " + statusColor + ";" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 3 10;");

        detailAddressLabel.setText(order.getDeliveryAddress());
        detailTotalLabel.setText("NPR " + String.format("%.2f", order.getTotalAmount()));

        // Items
        detailItemsBox.getChildren().clear();
        for (OrderItem item : order.getItems()) {
            Label lbl = new Label("• " + item.getItemName()
                    + " x" + item.getQuantity()
                    + "  —  NPR " + String.format("%.2f", item.getSubtotal()));
            lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
            detailItemsBox.getChildren().add(lbl);
        }
        if (order.getItems().isEmpty()) {
            Label none = new Label("No items found");
            none.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa;");
            detailItemsBox.getChildren().add(none);
        }

        // Restaurant row
        detailRestaurantLabel.setText(getRestaurantName(order.getRestaurantID()));
        boolean cancelled = order.getStatus() == OrderStatus.CANCELLED;
        restaurantFeedbackBtn.setDisable(cancelled);

        // Check if restaurant feedback already exists
        try {
            FeedbackEntry existing = feedbackDAO.getRestaurantFeedbackByCustomerAndOrder(
                    me.getUserID(), order.getOrderID());
            if (existing != null) {
                restaurantFeedbackBtn.setText("Show Feedback");
                restaurantFeedbackBtn.setStyle(restaurantFeedbackBtn.getStyle()
                    .replace("-fx-background-color: #4d9078;",
                             "-fx-background-color: #2e6b55;"));
            } else {
                restaurantFeedbackBtn.setText("Add Feedback");
                restaurantFeedbackBtn.setStyle(
                    "-fx-background-color: #4d9078;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-font-size: 11px;" +
                    "-fx-padding: 6 12;" +
                    "-fx-cursor: hand;");
            }
        } catch (SQLException ignored) {
            restaurantFeedbackBtn.setText("Add Feedback");
        }

        // Driver row — only show if a driver is assigned
        int driverID = order.getDriverID();
        if (driverID > 0) {
            driverFeedbackRow.setVisible(true);
            driverFeedbackRow.setManaged(true);
            // Fetch driver name + licence
            try {
                List<User> users = userDAO.getAllUsers();
                String driverInfo = "Driver #" + driverID;
                for (User u : users) {
                    if (u.getUserID() == driverID) {
                        String licence = "";
                        if (u instanceof Driver d) licence = "  ·  Licence: " + d.getLicenseNumber();
                        driverInfo = u.getFullName() + licence;
                        break;
                    }
                }
                detailDriverLabel.setText(driverInfo);
            } catch (SQLException ex) {
                detailDriverLabel.setText("Driver #" + driverID);
            }
            // Driver feedback button state
            boolean driverFeedbackAllowed = !cancelled;
            driverFeedbackBtn.setDisable(!driverFeedbackAllowed);
            try {
                FeedbackEntry df = feedbackDAO.getDriverFeedbackByCustomerAndOrder(
                        me.getUserID(), order.getOrderID());
                if (df != null) {
                    driverFeedbackBtn.setText("Show Feedback");
                    driverFeedbackBtn.setStyle(
                        "-fx-background-color: #c05a00;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 11px;" +
                        "-fx-padding: 6 12;" +
                        "-fx-cursor: hand;");
                } else {
                    driverFeedbackBtn.setText("Add Feedback");
                    driverFeedbackBtn.setStyle(
                        "-fx-background-color: #FF7518;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 11px;" +
                        "-fx-padding: 6 12;" +
                        "-fx-cursor: hand;");
                }
            } catch (SQLException ignored) {
                driverFeedbackBtn.setText("Add Feedback");
            }
        } else {
            driverFeedbackRow.setVisible(false);
            driverFeedbackRow.setManaged(false);
        }
    }

    @FXML
    private void handleRestaurantFeedback(ActionEvent event) {
        if (selectedHistoryOrder == null) return;
        try {
            FeedbackEntry existing =
                feedbackDAO.getRestaurantFeedbackByCustomerAndOrder(
                    me.getUserID(),
                    selectedHistoryOrder.getOrderID());
            if (existing != null) {
                // Already submitted — jump straight to Feedback tab
                // filtered to show only this review
                filteredFeedbackId = existing.getFeedbackID();
                showFeedbackTab(false);
                return;
            }
        } catch (SQLException ignored) {}
        openFeedbackPopup(selectedHistoryOrder, false);
    }

    @FXML
    private void handleDriverFeedback(ActionEvent event) {
        if (selectedHistoryOrder == null) {
            showAlert("Error", "Please select an order first.");
            return;
        }
        try {
            FeedbackEntry existing =
                feedbackDAO.getDriverFeedbackByCustomerAndOrder(
                    me.getUserID(),
                    selectedHistoryOrder.getOrderID());
            if (existing != null) {
                // Already submitted — jump straight to Feedback tab
                // filtered to show only this review
                filteredFeedbackId = existing.getFeedbackID();
                showFeedbackTab(false);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Database error: " + e.getMessage());
            return;
        }
        openFeedbackPopup(selectedHistoryOrder, true);
    }

    /** Shows a small prompt telling them feedback is submitted, with a button to jump to Feedback tab. */
    private void openViewFeedbackPrompt(FeedbackEntry entry, boolean isDriver) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Your Feedback");
        popup.setMinWidth(400);

        VBox root = new VBox(14);
        root.setStyle("-fx-padding: 26; -fx-background-color: white;");

        Label title = new Label(isDriver ? "Your Driver Feedback" : "Your Restaurant Feedback");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        String stars = "★".repeat(entry.getRating()) + "☆".repeat(5 - entry.getRating());
        Label starsLbl = new Label(stars);
        starsLbl.setStyle("-fx-font-size: 22px; -fx-text-fill: #f4a22d;");

        Label commentLbl = new Label(entry.getComment() == null || entry.getComment().isBlank()
                ? "(no comment)" : entry.getComment());
        commentLbl.setWrapText(true);
        commentLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");

        Separator sep = new Separator();

        Label hint = new Label("Want to see it in your Feedback tab?");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

        Button viewInTabBtn = new Button("📋  View in Feedback Tab");
        viewInTabBtn.setMaxWidth(Double.MAX_VALUE);
        viewInTabBtn.setStyle(
            "-fx-background-color: #FF7518; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-background-radius: 8;" +
            "-fx-padding: 9 18; -fx-cursor: hand;");
        viewInTabBtn.setOnAction(ev -> {
            popup.close();
            // Navigate to Feedback tab and filter to just this feedback
            filteredFeedbackId = entry.getFeedbackID();
            showFeedbackTab(false);
        });

        Button closeBtn = new Button("Close");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setStyle(
            "-fx-background-color: #e8e8c8; -fx-text-fill: #555;" +
            "-fx-background-radius: 8; -fx-padding: 9 18; -fx-cursor: hand;");
        closeBtn.setOnAction(ev -> popup.close());

        HBox btns = new HBox(10, closeBtn, viewInTabBtn);
        HBox.setHgrow(viewInTabBtn, Priority.ALWAYS);
        HBox.setHgrow(closeBtn, Priority.ALWAYS);

        root.getChildren().addAll(title, starsLbl, commentLbl, sep, hint, btns);
        popup.setScene(new Scene(root));
        popup.showAndWait();
    }

    private void filterMyFeedbackToOrder(int orderID, boolean driverFeedback) {
        myFeedbackListView.setCellFactory(
                lv -> new FeedbackCardCell(FeedbackCardCell.Mode.CUSTOMER));
        try {
            int orderItemID = feedbackDAO.getFirstOrderItemIDFromOrder(orderID);
            if (orderItemID <= 0) {
                myFeedbackListView.setItems(FXCollections.observableArrayList());
                myFeedbackStatus.setText("Could not load feedback for this order.");
                feedbackCountLabel.setText("0 shown");
                return;
            }

            List<FeedbackEntry> all = feedbackDAO.getByCustomer(me.getUserID());
            List<FeedbackEntry> filtered = new ArrayList<>();
            for (FeedbackEntry e : all) {
                boolean isDriverEntry = (e.getDriverID() > 0);
                if (e.getOrderItemID() == orderItemID && isDriverEntry == driverFeedback) {
                    String name = driverFeedback
                            ? "Driver feedback"
                            : getRestaurantName(e.getRestaurantID());
                    e.setCustomerName(name);
                    filtered.add(e);
                }
            }
            // Also check driver feedback table if needed
            if (driverFeedback && filtered.isEmpty()) {
                FeedbackEntry df = feedbackDAO.getDriverFeedbackByCustomerAndOrder(me.getUserID(), orderID);
                if (df != null) {
                    df.setCustomerName("Driver feedback");
                    filtered.add(df);
                }
            }
            myFeedbackListView.setItems(FXCollections.observableArrayList(filtered));
            myFeedbackStatus.setText(
                "Showing feedback for Order #" + orderID
                + "  —  click 💬 Feedback tab again to see all.");
            feedbackCountLabel.setText(filtered.size() + " shown");
        } catch (SQLException ex) {
            myFeedbackStatus.setText("Could not load feedback.");
        }
    }

    private void openFeedbackPopup(Order order, boolean isDriver) {
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

        // Subtitle — restaurant or driver + items
        String targetName = isDriver
                ? "Driver feedback for Order #" + order.getOrderID()
                : getRestaurantName(order.getRestaurantID()) + "  ·  " + firstItemsSummary(order);
        Label subtitle = new Label(targetName);
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
        commentArea.setPromptText(isDriver ? "Tell us about your driver..." : "Tell the restaurant what you thought...");
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
            FeedbackEntry existing = isDriver
                    ? feedbackDAO.getDriverFeedbackByCustomerAndOrder(me.getUserID(), order.getOrderID())
                    : feedbackDAO.getRestaurantFeedbackByCustomerAndOrder(me.getUserID(), order.getOrderID());
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
                if (isDriver) {
                    feedbackDAO.saveOrUpdateDriverFeedback(
                        me.getUserID(),
                        order.getRestaurantID(),
                        order.getDriverID(),
                        order.getOrderID(),
                        selectedRating[0],
                        comment);
                } else {
                    feedbackDAO.saveOrUpdate(
                        me.getUserID(),
                        order.getRestaurantID(),
                        order.getOrderID(),
                        selectedRating[0],
                        comment);
                }
                popup.close();
                populateOrderDetailCard(order); // refresh button states
                filteredFeedbackId = null; // clear any previous per-review filter
                loadMyFeedback();
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