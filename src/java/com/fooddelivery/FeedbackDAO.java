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

    /** Get feedback for a specific customer + order. This returns any feedback record, including driver-specific comments. */
    public FeedbackEntry getByCustomerAndOrder(int customerID, int orderID) throws SQLException {
        int orderItemID = getFirstOrderItemIDFromOrder(orderID);
        if (orderItemID <= 0) return null;

        String sql = "SELECT * FROM feedbacks WHERE customerID=? AND orderItemID=?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            ps.setInt(2, orderItemID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    /** Get restaurant-specific feedback for a customer + order. */
    public FeedbackEntry getRestaurantFeedbackByCustomerAndOrder(int customerID, int orderID) throws SQLException {
        int orderItemID = getFirstOrderItemIDFromOrder(orderID);
        if (orderItemID <= 0) return null;

        String sql = "SELECT * FROM feedbacks WHERE customerID=? AND orderItemID=? AND driverID=0";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            ps.setInt(2, orderItemID);
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

    /** Insert or update restaurant feedback for a given order.
     *  Converts orderID to the actual orderItemID before saving. */
    public void saveOrUpdate(int customerID, int restaurantID,
            int orderID, int rating, String comment) throws SQLException {
        
        // Get the ACTUAL orderItemID from order_items table (NOT the orderID itself!)
        int orderItemID = getFirstOrderItemIDFromOrder(orderID);
        if (orderItemID <= 0) {
            throw new SQLException("Order #" + orderID + " has no items. Cannot save feedback.");
        }

        try (Connection conn = getConn()) {
            // Try UPDATE — key: customerID + orderItemID + driverID=0 (restaurant feedback)
            String updateSql = "UPDATE feedbacks SET rating=?, comment=?, restaurantID=?, createdAt=? "
                    + "WHERE customerID=? AND orderItemID=? AND (driverID IS NULL OR driverID=0)";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, rating);
                ps.setString(2, comment);
                ps.setInt(3, restaurantID);
                ps.setString(4, LocalDateTime.now().toString());
                ps.setInt(5, customerID);
                ps.setInt(6, orderItemID);
                if (ps.executeUpdate() > 0) return; // updated existing row
            }
            // No existing row — INSERT
            String insertSql = "INSERT INTO feedbacks "
                    + "(customerID, restaurantID, orderItemID, driverID, rating, comment, createdAt) "
                    + "VALUES (?, ?, ?, 0, ?, ?, ?)";
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
        try { e.setDriverID(rs.getInt("driverID")); } catch (SQLException ignored) {}
        return e;
    }

    /** Get driver-specific feedback for a customer+order (driverID > 0). */
    public FeedbackEntry getDriverFeedbackByCustomerAndOrder(
            int customerID, int orderID) throws SQLException {
        int orderItemID = getFirstOrderItemIDFromOrder(orderID);
        if (orderItemID <= 0) return null;

        String sql = "SELECT * FROM feedbacks WHERE customerID=? AND orderItemID=? AND driverID>0";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            ps.setInt(2, orderItemID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    /** Save or update driver-specific feedback.
     *  Validates that order, driver, and restaurant all exist before saving. */
    public void saveOrUpdateDriverFeedback(int customerID, int restaurantID, int driverID,
            int orderID, int rating, String comment) throws SQLException {
        
        // Get the ACTUAL orderItemID from order_items table (NOT the orderID itself!)
        int orderItemID = getFirstOrderItemIDFromOrder(orderID);
        if (orderItemID <= 0) {
            throw new SQLException("Order #" + orderID + " has no items. Cannot save feedback.");
        }
        
        // Fetch the actual restaurantID and driverID from the order to ensure FK constraints are satisfied
        int actualRestaurantID = getRestaurantIDFromOrder(orderID);
        int actualDriverID = getDriverIDFromOrder(orderID);
        
        // Validate: Order must have valid restaurant and driver assigned
        if (actualRestaurantID <= 0) {
            throw new SQLException("Order #" + orderID + " has no valid restaurant assigned.");
        }
        if (actualDriverID <= 0) {
            throw new SQLException("Order #" + orderID + " has no driver assigned. Driver feedback requires a driver.");
        }

        try (Connection conn = getConn()) {
            // First, delete any existing records with invalid FK values (corrupted data)
            String cleanup = "DELETE FROM feedbacks WHERE customerID=? AND driverID>0 AND orderItemID=? AND (restaurantID IS NULL OR restaurantID=0)";
            try (PreparedStatement ps = conn.prepareStatement(cleanup)) {
                ps.setInt(1, customerID);
                ps.setInt(2, orderItemID);
                ps.executeUpdate();
            }
            
            // Try UPDATE — key: customerID + driverID + orderItemID
            String upd = "UPDATE feedbacks "
                    + "SET rating=?, comment=?, restaurantID=?, driverID=?, createdAt=? "
                    + "WHERE customerID=? AND orderItemID=? AND driverID>0";
            try (PreparedStatement ps = conn.prepareStatement(upd)) {
                ps.setInt(1, rating);
                ps.setString(2, comment);
                ps.setInt(3, actualRestaurantID);
                ps.setInt(4, actualDriverID);
                ps.setString(5, LocalDateTime.now().toString());
                ps.setInt(6, customerID);
                ps.setInt(7, orderItemID);
                if (ps.executeUpdate() > 0) return; // updated existing row
            }
            // No existing row — INSERT with validated FK values
            String ins = "INSERT INTO feedbacks "
                    + "(customerID, restaurantID, orderItemID, driverID, rating, comment, createdAt) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                ps.setInt(1, customerID);
                ps.setInt(2, actualRestaurantID);
                ps.setInt(3, orderItemID);
                ps.setInt(4, actualDriverID);
                ps.setInt(5, rating);
                ps.setString(6, comment);
                ps.setString(7, LocalDateTime.now().toString());
                ps.executeUpdate();
            }
        }
    }

    /** Helper: Get the FIRST orderItemID for a given orderID. */
    public int getFirstOrderItemIDFromOrder(int orderID) throws SQLException {
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
        return -1; // Not found
    }

    /** Helper: Get restaurantID from an order. */
    private int getRestaurantIDFromOrder(int orderID) throws SQLException {
        String sql = "SELECT restaurantID FROM orders WHERE orderID = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("restaurantID");
                }
            }
        }
        return -1; // Not found
    }

    /** Helper: Get driverID from an order. */
    private int getDriverIDFromOrder(int orderID) throws SQLException {
        String sql = "SELECT driverID FROM orders WHERE orderID = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("driverID");
                }
            }
        }
        return -1; // Not found
    }

    /** All driver-specific feedback received by a driver (driverID > 0). */
    public List<FeedbackEntry> getDriverFeedbackByDriver(int driverID) throws SQLException {
        List<FeedbackEntry> list = new ArrayList<>();
        String sql = "SELECT f.*, u.fullName AS customerName "
                + "FROM feedbacks f "
                + "JOIN users u ON f.customerID = u.userID "
                + "WHERE f.driverID = ? ORDER BY f.createdAt DESC";
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
}