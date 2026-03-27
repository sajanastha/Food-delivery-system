package com.fooddelivery;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class FeedbackDAO {
    private final Connection conn =
            DatabaseManager.getInstance().getConnection();

    public Map<Integer, FeedbackEntry> getByCustomer(int customerID)
            throws SQLException {
        Map<Integer, FeedbackEntry> map = new HashMap<>();
        String sql = "SELECT * FROM feedbacks WHERE customerID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FeedbackEntry entry = new FeedbackEntry();
                entry.setFeedbackID(rs.getInt("feedbackID"));
                entry.setCustomerID(rs.getInt("customerID"));
                entry.setRestaurantID(rs.getInt("restaurantID"));
                entry.setOrderItemID(rs.getInt("orderItemID"));
                entry.setRating(rs.getInt("rating"));
                entry.setComment(rs.getString("comment"));
                entry.setCreatedAt(rs.getString("createdAt"));
                map.put(entry.getOrderItemID(), entry);
            }
        }
        return map;
    }

    public void saveOrUpdate(int customerID, int restaurantID,
            int orderItemID, int rating, String comment)
            throws SQLException {
        String updateSql = "UPDATE feedbacks SET rating=?, comment=?, " +
                "createdAt=? WHERE orderItemID=?";
        try (PreparedStatement update = conn.prepareStatement(updateSql)) {
            update.setInt(1, rating);
            update.setString(2, comment);
            update.setString(3, LocalDateTime.now().toString());
            update.setInt(4, orderItemID);
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        String insertSql = "INSERT INTO feedbacks " +
                "(customerID,restaurantID,orderItemID,rating,comment,createdAt) " +
                "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
            insert.setInt(1, customerID);
            insert.setInt(2, restaurantID);
            insert.setInt(3, orderItemID);
            insert.setInt(4, rating);
            insert.setString(5, comment);
            insert.setString(6, LocalDateTime.now().toString());
            insert.executeUpdate();
        }
    }
}
