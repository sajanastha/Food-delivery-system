package java;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final Connection conn =
            DatabaseManager.getInstance().getConnection();

    public int createUser(String email, String password,
            String fullName, String phone, String role)
            throws SQLException {
        String sql = "INSERT INTO users " +
                "(email,password,fullName,phone,role) " +
                "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setString(4, phone);
            ps.setString(5, role.toUpperCase());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    public User validateLogin(String email, String password)
            throws SQLException {
        String sql = "SELECT * FROM users " +
                "WHERE email=? AND password=? AND isActive=1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return UserFactory.createUser(
                    rs.getString("role"),
                    rs.getInt("userID"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("fullName"),
                    rs.getString("phone")
                );
            }
        }
        return null;
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY role,fullName";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                User u = UserFactory.createUser(
                    rs.getString("role"),
                    rs.getInt("userID"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("fullName"),
                    rs.getString("phone")
                );
                u.setActive(rs.getInt("isActive") == 1);
                list.add(u);
            }
        }
        return list;
    }

    public void setUserActive(int userID, boolean active)
            throws SQLException {
        String sql = "UPDATE users SET isActive=? WHERE userID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, userID);
            ps.executeUpdate();
        }
    }

    public void deleteUser(int userID) throws SQLException {
        String sql = "DELETE FROM users WHERE userID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.executeUpdate();
        }
    }
}