package com.fooddelivery;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    private final Connection conn =
            DatabaseManager.getInstance().getConnection();

    public int createOrder(Order order) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sql = "INSERT INTO orders (customerID," +
                    "restaurantID,deliveryFee,deliveryAddress," +
                    "status,orderTime,driverID) VALUES (?,?,?,?,?,?,?)";
            int newID;
            try (PreparedStatement ps = conn.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, order.getCustomerID());
                ps.setInt(2, order.getRestaurantID());
                ps.setDouble(3, order.getDeliveryFee());
                ps.setString(4, order.getDeliveryAddress());
                ps.setString(5, "CONFIRMED");
                ps.setString(6, LocalDateTime.now().toString());
                ps.setInt(7, 0);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        newID = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to get generated order ID");
                    }
                }
            }
            String iSql = "INSERT INTO order_items " +
                    "(orderID,menuItemID,itemName," +
                    "itemPrice,quantity) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps =
                    conn.prepareStatement(iSql)) {
                for (OrderItem item : order.getItems()) {
                    ps.setInt(1, newID);
                    ps.setInt(2, item.getMenuItemID());
                    ps.setString(3, item.getItemName());
                    ps.setDouble(4, item.getItemPrice());
                    ps.setInt(5, item.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
            return newID;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public List<Order> getByCustomer(int customerID)
            throws SQLException {
        return query(
            "SELECT * FROM orders WHERE customerID=? " +
            "ORDER BY orderTime DESC", customerID);
    }

    public List<Order> getByRestaurant(int restaurantID)
            throws SQLException {
        return query(
            "SELECT * FROM orders WHERE restaurantID=? " +
            "ORDER BY orderTime DESC", restaurantID);
    }

    public List<Order> getAllOrders() throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY orderTime DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Order o = map(rs);
                o.setItems(itemsFor(o.getOrderID()));
                list.add(o);
            }
        }
        return list;
    }

    public List<Order> getByStatus(OrderStatus status)
            throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status=? ORDER BY orderTime DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Order o = map(rs);
                o.setItems(itemsFor(o.getOrderID()));
                list.add(o);
            }
        }
        return list;
    }

    public List<Order> getAvailableForDrivers()
            throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders " +
                "WHERE status='CONFIRMED' AND (driverID IS NULL OR driverID=0) " +
                "ORDER BY orderTime DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Order o = map(rs);
                o.setItems(itemsFor(o.getOrderID()));
                list.add(o);
            }
        }
        return list;
    }

    public List<Order> getByDriver(int driverID)
            throws SQLException {
        return query(
            "SELECT * FROM orders WHERE driverID=? " +
            "ORDER BY orderTime DESC", driverID);
    }

    public List<Order> getByDriverAndStatus(int driverID,
            OrderStatus status) throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE driverID=? " +
                "AND status=? ORDER BY orderTime DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, driverID);
            ps.setString(2, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Order o = map(rs);
                o.setItems(itemsFor(o.getOrderID()));
                list.add(o);
            }
        }
        return list;
    }

    public List<Order> getCompletedInRange(int restaurantID,
            String from, String to) throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE " +
                "restaurantID=? AND status='DELIVERED' " +
                "AND orderTime BETWEEN ? AND ?";
        try (PreparedStatement ps =
                conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantID);
            ps.setString(2, from);
            ps.setString(3, to + "T23:59:59");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Order o = map(rs);
                o.setItems(itemsFor(o.getOrderID()));
                list.add(o);
            }
        }
        return list;
    }

    public List<String> getTopRestaurantsForToday(int limit)
            throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT r.restaurantName, COUNT(*) AS totalOrders " +
                "FROM orders o " +
                "JOIN restaurants r ON o.restaurantID=r.restaurantID " +
                "WHERE o.orderTime LIKE ? " +
                "GROUP BY r.restaurantID, r.restaurantName " +
                "ORDER BY totalOrders DESC, r.restaurantName ASC " +
                "LIMIT ?";
        String today = LocalDateTime.now().toLocalDate().toString() + "%";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, today);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("restaurantName")
                        + " - " + rs.getInt("totalOrders") + " order(s)");
            }
        }
        return list;
    }

    public List<String> getTopFoodsForToday(int limit)
            throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT oi.itemName, SUM(oi.quantity) AS totalQty " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.orderID=o.orderID " +
                "WHERE o.orderTime LIKE ? " +
                "GROUP BY oi.itemName " +
                "ORDER BY totalQty DESC, oi.itemName ASC " +
                "LIMIT ?";
        String today = LocalDateTime.now().toLocalDate().toString() + "%";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, today);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("itemName")
                        + " - " + rs.getInt("totalQty") + " sold");
            }
        }
        return list;
    }

    public void updateStatus(int orderID, OrderStatus status)
            throws SQLException {
        String sql = "UPDATE orders SET status=? " +
                "WHERE orderID=?";
        try (PreparedStatement ps =
                conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, orderID);
            ps.executeUpdate();
        }
    }

    public void assignDriver(int orderID, int driverID)
            throws SQLException {
        String sql = "UPDATE orders SET driverID=?," +
                "status='IN_DELIVERY' WHERE orderID=?";
        try (PreparedStatement ps =
                conn.prepareStatement(sql)) {
            ps.setInt(1, driverID);
            ps.setInt(2, orderID);
            ps.executeUpdate();
        }
    }

    private List<Order> query(String sql, int param)
            throws SQLException {
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps =
                conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Order o = map(rs);
                o.setItems(itemsFor(o.getOrderID()));
                list.add(o);
            }
        }
        return list;
    }

    private List<OrderItem> itemsFor(int orderID)
            throws SQLException {
        List<OrderItem> list = new ArrayList<>();
        String sql = "SELECT * FROM order_items " +
                "WHERE orderID=?";
        try (PreparedStatement ps =
                conn.prepareStatement(sql)) {
            ps.setInt(1, orderID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                OrderItem i = new OrderItem(
                    rs.getInt("menuItemID"),
                    rs.getString("itemName"),
                    rs.getDouble("itemPrice"),
                    rs.getInt("quantity")
                );
                i.setOrderItemID(rs.getInt("orderItemID"));
                i.setOrderID(orderID);
                list.add(i);
            }
        }
        return list;
    }

    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order(
            rs.getInt("customerID"),
            rs.getInt("restaurantID"),
            rs.getString("deliveryAddress"),
            rs.getDouble("deliveryFee")
        );
        o.setOrderID(rs.getInt("orderID"));
        o.setDriverID(rs.getInt("driverID"));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        return o;
    }
}
