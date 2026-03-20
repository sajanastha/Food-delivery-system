package java;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDAO {
    private final Connection conn =
            DatabaseManager.getInstance().getConnection();

    public int addItem(MenuItem item) throws SQLException {
        String sql = "INSERT INTO menu_items " +
                "(restaurantID,name,description,price," +
                "category,isAvailable) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getRestaurantID());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setDouble(4, item.getPrice());
            ps.setString(5, item.getCategory());
            ps.setInt(6, item.isAvailable() ? 1 : 0);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    public List<MenuItem> getByRestaurant(int restaurantID)
            throws SQLException {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT * FROM menu_items " +
                "WHERE restaurantID=? ORDER BY category,name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<MenuItem> getAvailable(int restaurantID)
            throws SQLException {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT * FROM menu_items " +
                "WHERE restaurantID=? AND isAvailable=1 " +
                "ORDER BY category,name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void updateItem(MenuItem item) throws SQLException {
        String sql = "UPDATE menu_items SET name=?," +
                "description=?,price=?,category=?," +
                "isAvailable=? WHERE itemID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getPrice());
            ps.setString(4, item.getCategory());
            ps.setInt(5, item.isAvailable() ? 1 : 0);
            ps.setInt(6, item.getItemID());
            ps.executeUpdate();
        }
    }

    public void deleteItem(int itemID) throws SQLException {
        String sql = "DELETE FROM menu_items WHERE itemID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemID);
            ps.executeUpdate();
        }
    }

    public void toggleAvailability(int itemID, boolean available)
            throws SQLException {
        String sql = "UPDATE menu_items SET isAvailable=? " +
                "WHERE itemID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, available ? 1 : 0);
            ps.setInt(2, itemID);
            ps.executeUpdate();
        }
    }

    private MenuItem map(ResultSet rs) throws SQLException {
        MenuItem m = new MenuItem(
            rs.getInt("itemID"),
            rs.getInt("restaurantID"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getDouble("price"),
            rs.getString("category")
        );
        m.setAvailable(rs.getInt("isAvailable") == 1);
        return m;
    }
}