package com.fooddelivery;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDAO {

    /** Always gets a fresh independent connection to avoid shared-connection
     *  autoCommit conflicts with OrderDAO transactions. */
    private Connection getConn() throws SQLException {
        return DatabaseManager.getInstance().newConnection();
    }

    /** Get feedback for a specific customer + order. */
    public FeedbackEntry getByCustomerAndOrder(int customerID, int orderID) throws SQLException {
        String sql = "SELECT * FROM feedbacks WHERE customerID=? AND orderItemID=?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            ps.setInt(2, orderID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    /** All feedback submitted by a customer. */
    public List<FeedbackEntry> getByCustomer(int customerID) throws SQLException {
        List<FeedbackEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM feedbacks WHERE customerID=? ORDER BY createdAt DESC";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** All feedback received by a restaurant (includes customer name). */
    public List<FeedbackEntry> getByRestaurant(int restaurantID) throws SQLException {
        List<FeedbackEntry> list = new ArrayList<>();
        String sql = "SELECT f.*, u.fullName AS customerName "
                + "FROM feedbacks f "
                + "JOIN users u ON f.customerID = u.userID "
                + "WHERE f.restaurantID = ? ORDER BY f.createdAt DESC";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FeedbackEntry e = map(rs);
                    e.setCustomerName(rs.getString("customerName"));
                    list.add(e);
                }
            }
        }
        return list;
    }

    /** All feedback for orders delivered by a driver. */
    public List<FeedbackEntry> getByDriver(int driverID) throws SQLException {
        List<FeedbackEntry> list = new ArrayList<>();
        String sql = "SELECT f.*, u.fullName AS customerName "
                + "FROM feedbacks f "
                + "JOIN orders o ON f.orderItemID = o.orderID "
                + "JOIN users u ON f.customerID = u.userID "
                + "WHERE o.driverID = ? ORDER BY f.createdAt DESC";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, driverID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FeedbackEntry e = map(rs);
                    e.setCustomerName(rs.getString("customerName"));
                    list.add(e);
                }
            }
        }
        return list;
    }

    /** Insert or update feedback for a given order. */
    public void saveOrUpdate(int customerID, int restaurantID,
            int orderID, int rating, String comment) throws SQLException {
        // Get the first orderItemID from this order
        int orderItemID = getFirstOrderItemID(orderID);
        if (orderItemID == -1) {
            throw new SQLException("No order items found for order ID: " + orderID);
        }
        
        try (Connection conn = getConn()) {
            // Try UPDATE first
            String updateSql = "UPDATE feedbacks SET rating=?, comment=?, createdAt=? "
                    + "WHERE customerID=? AND restaurantID=? AND orderItemID=?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, rating);
                ps.setString(2, comment);
                ps.setString(3, LocalDateTime.now().toString());
                ps.setInt(4, customerID);
                ps.setInt(5, restaurantID);
                ps.setInt(6, orderItemID);
                if (ps.executeUpdate() > 0) return; // updated existing
            }
            // No existing row — INSERT
            String insertSql = "INSERT INTO feedbacks "
                    + "(customerID, restaurantID, orderItemID, rating, comment, createdAt) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, customerID);
                ps.setInt(2, restaurantID);
                ps.setInt(3, orderItemID);
                ps.setInt(4, rating);
                ps.setString(5, comment);
                ps.setString(6, LocalDateTime.now().toString());
                ps.executeUpdate();
            }
        }
    }

    /** Helper: Get the first orderItemID for a given orderID. */
    private int getFirstOrderItemID(int orderID) throws SQLException {
        String sql = "SELECT orderItemID FROM order_items WHERE orderID = ? LIMIT 1";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("orderItemID");
                }
            }
        }
        return -1; // No items found
    }

    private FeedbackEntry map(ResultSet rs) throws SQLException {
        FeedbackEntry e = new FeedbackEntry();
        e.setFeedbackID(rs.getInt("feedbackID"));
        e.setCustomerID(rs.getInt("customerID"));
        e.setRestaurantID(rs.getInt("restaurantID"));
        e.setOrderItemID(rs.getInt("orderItemID"));
        e.setRating(rs.getInt("rating"));
        e.setComment(rs.getString("comment"));
        e.setCreatedAt(rs.getString("createdAt"));
        return e;
    }
}