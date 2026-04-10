package com.fooddelivery;

import java.sql.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/food_delivery";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "sajana123456789";

    private DatabaseManager() {
        connect();
        createTables();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    /** Always returns a valid, open connection. Reconnects automatically if closed. */
    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("DB: reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("DB: connection check failed, reconnecting: " + e.getMessage());
            connect();
        }
        return connection;
    }

    /** Opens a brand-new independent connection for use in a single DAO operation.
     *  Caller is responsible for closing it. Used by FeedbackDAO to avoid
     *  shared-connection autoCommit conflicts with OrderDAO transactions. */
    public Connection newConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void connect() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("Database connected.");
        } catch (SQLException e) {
            System.err.println("DB connection failed: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS users ("
                + "userID INT PRIMARY KEY AUTO_INCREMENT,"
                + "email VARCHAR(255) NOT NULL UNIQUE,"
                + "password VARCHAR(255) NOT NULL,"
                + "fullName VARCHAR(255) NOT NULL,"
                + "phone VARCHAR(50),"
                + "role VARCHAR(50) NOT NULL,"
                + "isActive TINYINT(1) DEFAULT 1"
                + ") ENGINE=InnoDB");

            s.execute("CREATE TABLE IF NOT EXISTS restaurants ("
                + "restaurantID INT PRIMARY KEY AUTO_INCREMENT,"
                + "ownerUserID INT NOT NULL,"
                + "restaurantName VARCHAR(255) NOT NULL,"
                + "address VARCHAR(500),"
                + "cuisineType VARCHAR(100),"
                + "rating DOUBLE DEFAULT 0.0,"
                + "operatingHours VARCHAR(100),"
                + "FOREIGN KEY (ownerUserID) REFERENCES users(userID)"
                + ") ENGINE=InnoDB");

            s.execute("CREATE TABLE IF NOT EXISTS menu_items ("
                + "itemID INT PRIMARY KEY AUTO_INCREMENT,"
                + "restaurantID INT NOT NULL,"
                + "name VARCHAR(255) NOT NULL,"
                + "description TEXT,"
                + "price DOUBLE NOT NULL,"
                + "category VARCHAR(100),"
                + "isAvailable TINYINT(1) DEFAULT 1,"
                + "FOREIGN KEY (restaurantID) REFERENCES restaurants(restaurantID)"
                + ") ENGINE=InnoDB");

            s.execute("CREATE TABLE IF NOT EXISTS orders ("
                + "orderID INT PRIMARY KEY AUTO_INCREMENT,"
                + "customerID INT NOT NULL,"
                + "restaurantID INT NOT NULL,"
                + "driverID INT DEFAULT 0,"
                + "deliveryFee DOUBLE DEFAULT 50.0,"
                + "deliveryAddress VARCHAR(500),"
                + "status VARCHAR(50) DEFAULT 'PENDING',"
                + "orderTime VARCHAR(50),"
                + "FOREIGN KEY (customerID) REFERENCES users(userID),"
                + "FOREIGN KEY (restaurantID) REFERENCES restaurants(restaurantID)"
                + ") ENGINE=InnoDB");

            s.execute("CREATE TABLE IF NOT EXISTS order_items ("
                + "orderItemID INT PRIMARY KEY AUTO_INCREMENT,"
                + "orderID INT NOT NULL,"
                + "menuItemID INT NOT NULL,"
                + "itemName VARCHAR(255) NOT NULL,"
                + "itemPrice DOUBLE NOT NULL,"
                + "quantity INT DEFAULT 1,"
                + "FOREIGN KEY (orderID) REFERENCES orders(orderID),"
                + "FOREIGN KEY (menuItemID) REFERENCES menu_items(itemID)"
                + ") ENGINE=InnoDB");

            // orderItemID column stores the orderID (naming kept for backwards compat)
            s.execute("CREATE TABLE IF NOT EXISTS feedbacks ("
                + "feedbackID INT PRIMARY KEY AUTO_INCREMENT,"
                + "customerID INT NOT NULL,"
                + "restaurantID INT NOT NULL,"
                + "orderItemID INT NOT NULL,"
                + "rating INT DEFAULT 5,"
                + "comment TEXT,"
                + "createdAt VARCHAR(50),"
                + "FOREIGN KEY (customerID) REFERENCES users(userID),"
                + "FOREIGN KEY (restaurantID) REFERENCES restaurants(restaurantID)"
                + ") ENGINE=InnoDB");

            // Add driverID column to feedbacks if it doesn't exist yet
            try {
                s.execute("ALTER TABLE feedbacks ADD COLUMN driverID INT DEFAULT 0");
            } catch (SQLException ignored) { /* column already exists */ }

            // ── Remove the restaurantID FK constraint from feedbacks ──────────
            // This constraint breaks driver feedback (no valid restaurantID needed)
            // and breaks restaurant feedback when restaurantID = 0 or mismatched.
            // We do this safely: find the constraint name from INFORMATION_SCHEMA first.
            try {
                ResultSet fkRs = s.executeQuery(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND TABLE_NAME = 'feedbacks' "
                    + "AND COLUMN_NAME = 'restaurantID' "
                    + "AND REFERENCED_TABLE_NAME = 'restaurants'");
                while (fkRs.next()) {
                    String constraintName = fkRs.getString("CONSTRAINT_NAME");
                    try {
                        s.execute("ALTER TABLE feedbacks DROP FOREIGN KEY `" + constraintName + "`");
                        System.out.println("Dropped feedbacks FK: " + constraintName);
                    } catch (SQLException ignored2) { /* already dropped */ }
                }
            } catch (SQLException ignored) { /* INFORMATION_SCHEMA not accessible */ }

            // Make restaurantID nullable so driver-only feedback rows work (restaurantID = 0)
            try {
                s.execute("ALTER TABLE feedbacks MODIFY COLUMN restaurantID INT DEFAULT 0");
            } catch (SQLException ignored) { /* already nullable or column changed */ }

            System.out.println("All tables ready.");
        } catch (SQLException e) {
            System.err.println("Tables error: " + e.getMessage());
        }
    }
}