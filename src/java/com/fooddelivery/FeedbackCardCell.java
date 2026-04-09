package com.fooddelivery;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class FeedbackCardCell extends ListCell<FeedbackEntry> {

    public enum Mode {
        /** Customer sees own reviews – shows restaurant/context as "name". */
        CUSTOMER,
        /** Driver or Restaurant sees reviews from customers – shows customer name. */
        RECEIVED
    }

    private final Mode mode;

    // Card widgets (reused on every updateItem call)
    private final VBox   card        = new VBox(6);
    private final Label  avatarLabel = new Label();
    private final Label  nameLabel   = new Label();
    private final Label  starsLabel  = new Label();
    private final Label  dateLabel   = new Label();
    private final Label  commentLabel = new Label();
    private final Label  subLabel    = new Label();   // "For customer:" or "Order #N"

    public FeedbackCardCell(Mode mode) {
        this.mode = mode;
        buildCard();
    }

    /* ------------------------------------------------------------------ */
    /*  Layout                                                              */
    /* ------------------------------------------------------------------ */

    private void buildCard() {
        // ── Avatar circle ──────────────────────────────────────────────
        avatarLabel.setPrefSize(40, 40);
        avatarLabel.setMinSize(40, 40);
        avatarLabel.setMaxSize(40, 40);
        avatarLabel.setAlignment(Pos.CENTER);
        avatarLabel.setStyle(
                "-fx-background-color: #4d9078;" +
                "-fx-background-radius: 20;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;");

        // ── Name + more-dots row ───────────────────────────────────────
        nameLabel.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #1a1a1a;");

        Label dotsLabel = new Label("⋮");
        dotsLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #888;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nameRow = new HBox(spacer, dotsLabel);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameRow, Priority.ALWAYS);

        // Put avatar + nameBlock together
        VBox nameBlock = new VBox(2, nameLabel, nameRow);
        HBox.setHgrow(nameBlock, Priority.ALWAYS);

        // ── Stars + date row ───────────────────────────────────────────
        starsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #f4a22d;");
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        HBox starsRow = new HBox(8, starsLabel, dateLabel);
        starsRow.setAlignment(Pos.CENTER_LEFT);

        // ── Comment 
        commentLabel.setWrapText(true);
        commentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        // ── Sub-label ("For customer: …" / "Order #N") 
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #777; -fx-font-style: italic;");

        // ── Top header row (avatar + name block)
        HBox headerRow = new HBox(10, avatarLabel, nameBlock);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // ── Card container 
        card.getChildren().addAll(subLabel, headerRow, starsRow, commentLabel);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #e0e0e0;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 2);");

        setStyle("-fx-background-color: transparent; -fx-padding: 6 10;");
        setPadding(new Insets(4, 8, 4, 8));
    }

   
    /*  Data binding */
   
    @Override
    protected void updateItem(FeedbackEntry entry, boolean empty) {
        super.updateItem(entry, empty);
        if (empty || entry == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        // Stars
        int r = Math.max(1, Math.min(5, entry.getRating()));
        starsLabel.setText("★".repeat(r) + "☆".repeat(5 - r));

        // Date
        String rawDate = entry.getCreatedAt();
        String date = (rawDate != null && rawDate.length() >= 10)
                ? rawDate.substring(0, 10) : "";
        dateLabel.setText(date);

        // Comment
        String comment = (entry.getComment() == null || entry.getComment().isBlank())
                ? "(no comment)" : entry.getComment();
        commentLabel.setText(comment);

        if (mode == Mode.CUSTOMER) {
            // Customer sees their own review → do not show order metadata in the card.
            subLabel.setVisible(false);
            subLabel.setManaged(false);

            String who = entry.getCustomerName().isBlank()
                    ? "Restaurant #" + entry.getRestaurantID()
                    : entry.getCustomerName(); // customerName reused to carry rest name
            nameLabel.setText(who);
            avatarLabel.setText(initials(who));
            avatarLabel.setStyle(avatarStyle("#4d9078"));

        } else {
            // Driver / Restaurant sees reviews from customers
            String who = entry.getCustomerName().isBlank()
                    ? "Customer #" + entry.getCustomerID()
                    : entry.getCustomerName();
            subLabel.setVisible(false);
            subLabel.setManaged(false);

            nameLabel.setText(who);
            avatarLabel.setText(initials(who));
            avatarLabel.setStyle(avatarStyle("#FF7518"));
        }

        setGraphic(card);
        setText(null);
    }

    /*  Helpers  */                       

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static String avatarStyle(String color) {
        return "-fx-background-color: " + color + ";" +
               "-fx-background-radius: 20;" +
               "-fx-text-fill: white;" +
               "-fx-font-size: 14px;" +
               "-fx-font-weight: bold;";
    }
}