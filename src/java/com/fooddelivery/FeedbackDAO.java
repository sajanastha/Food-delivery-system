package com.fooddelivery;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDAO {
    private final Connection conn =
            DatabaseManager.getInstance().getConnection();

    /** Get feedback for a specific customer + order. */
    public FeedbackEntry getByCustomerAndOrder(int customerID, int orderID) throws SQLException {
        String sql = "SELECT * FROM feedbacks WHERE customerID=? AND orderItemID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            ps.setInt(2, orderID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    /** All feedback sent by a customer (customer Feedback tab). */
    public List<FeedbackEntry> getByCustomer(int customerID) throws SQLException {
        List<FeedbackEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM feedbacks WHERE customerID=? ORDER BY createdAt DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** All feedback received by a restaurant. */
    public List<FeedbackEntry> getByRestaurant(int restaurantID) throws SQLException {
        List<FeedbackEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM feedbacks WHERE restaurantID=? ORDER BY createdAt DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** All feedback for orders delivered by a driver (joins on orderID). */
    public List<FeedbackEntry> getByDriver(int driverID) throws SQLException {
        List<FeedbackEntry> list = new ArrayList<>();
        String sql = "SELECT f.* FROM feedbacks f "
                + "JOIN orders o ON f.orderItemID = o.orderID "
                + "WHERE o.driverID = ? ORDER BY f.createdAt DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, driverID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void saveOrUpdate(int customerID, int restaurantID,
            int orderID, int rating, String comment) throws SQLException {
        String updateSql = "UPDATE feedbacks SET rating=?, comment=?, createdAt=? "
                + "WHERE customerID=? AND restaurantID=? AND orderItemID=?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, rating);
            ps.setString(2, comment);
            ps.setString(3, LocalDateTime.now().toString());
            ps.setInt(4, customerID);
            ps.setInt(5, restaurantID);
            ps.setInt(6, orderID);
            if (ps.executeUpdate() > 0) return;
        }
        String insertSql = "INSERT INTO feedbacks "
                + "(customerID, restaurantID, orderItemID, rating, comment, createdAt) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, customerID);
            ps.setInt(2, restaurantID);
            ps.setInt(3, orderID);
            ps.setInt(4, rating);
            ps.setString(5, comment);
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    private FeedbackEntry map(ResultSet rs) throws SQLException {
        FeedbackEntry entry = new FeedbackEntry();
        entry.setFeedbackID(rs.getInt("feedbackID"));
        entry.setCustomerID(rs.getInt("customerID"));
        entry.setRestaurantID(rs.getInt("restaurantID"));
        entry.setOrderItemID(rs.getInt("orderItemID"));
        entry.setRating(rs.getInt("rating"));
        entry.setComment(rs.getString("comment"));
        entry.setCreatedAt(rs.getString("createdAt"));
        return entry;
    }
}