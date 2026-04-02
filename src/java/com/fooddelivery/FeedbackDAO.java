package com.fooddelivery;

import java.sql.*;
import java.time.LocalDateTime;

public class FeedbackDAO {
    private final Connection conn =
            DatabaseManager.getInstance().getConnection();

    public void saveOrUpdate(int customerID, int restaurantID,
            int orderID, int rating, String comment)
            throws SQLException {

        // Try update first
        String updateSql = "UPDATE feedbacks SET rating=?,"
            + "comment=?,createdAt=? WHERE customerID=?"
            + " AND restaurantID=? AND orderItemID=?";
        try (PreparedStatement ps =
                conn.prepareStatement(updateSql)) {
            ps.setInt(1, rating);
            ps.setString(2, comment);
            ps.setString(3, LocalDateTime.now().toString());
            ps.setInt(4, customerID);
            ps.setInt(5, restaurantID);
            ps.setInt(6, orderID);
            if (ps.executeUpdate() > 0) return;
        }

        // Insert new
        String insertSql = "INSERT INTO feedbacks "
            + "(customerID,restaurantID,orderItemID,"
            + "rating,comment,createdAt) "
            + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps =
                conn.prepareStatement(insertSql)) {
            ps.setInt(1, customerID);
            ps.setInt(2, restaurantID);
            ps.setInt(3, orderID);
            ps.setInt(4, rating);
            ps.setString(5, comment);
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }
}
