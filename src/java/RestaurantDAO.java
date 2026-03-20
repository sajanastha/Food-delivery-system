package java;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDAO {
    private final Connection conn =
            DatabaseManager.getInstance().getConnection();

    public int addRestaurant(int ownerUserID, String name,
            String address, String cuisine, String hours)
            throws SQLException {
        String sql = "INSERT INTO restaurants " +
                "(ownerUserID,restaurantName,address," +
                "cuisineType,operatingHours) " +
                "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ownerUserID);
            ps.setString(2, name);
            ps.setString(3, address);
            ps.setString(4, cuisine);
            ps.setString(5, hours);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    public Restaurant getByOwner(int ownerUserID)
            throws SQLException {
        String sql = "SELECT r.*, u.email, u.password, " +
                "u.fullName, u.phone " +
                "FROM restaurants r " +
                "JOIN users u ON r.ownerUserID=u.userID " +
                "WHERE r.ownerUserID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerUserID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    public List<Restaurant> getAll() throws SQLException {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT r.*, u.email, u.password, " +
                "u.fullName, u.phone " +
                "FROM restaurants r " +
                "JOIN users u ON r.ownerUserID=u.userID";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Restaurant> searchByName(String keyword)
            throws SQLException {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT r.*, u.email, u.password, " +
                "u.fullName, u.phone " +
                "FROM restaurants r " +
                "JOIN users u ON r.ownerUserID=u.userID " +
                "WHERE LOWER(r.restaurantName) LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword.toLowerCase() + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Restaurant map(ResultSet rs) throws SQLException {
        Restaurant r = new Restaurant(
            rs.getInt("ownerUserID"),
            rs.getString("email"),
            rs.getString("password"),
            rs.getString("fullName"),
            rs.getString("phone")
        );
        r.setRestaurantID(rs.getInt("restaurantID"));
        r.setRestaurantName(rs.getString("restaurantName"));
        r.setAddress(rs.getString("address"));
        r.setCuisineType(rs.getString("cuisineType"));
        r.setRating(rs.getDouble("rating"));
        r.setOperatingHours(rs.getString("operatingHours"));
        return r;
    }
}
