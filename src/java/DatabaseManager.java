package java;

import java.sql.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/food_delivery";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "yourpassword"; // change this

    private DatabaseManager() {
        connect();
        createTables();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() { return connection; }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("Database connected.");
        } catch (SQLException e) {
            System.err.println("DB failed: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS users (" +
                "userID INT PRIMARY KEY AUTO_INCREMENT," +
                "email VARCHAR(255) NOT NULL UNIQUE," +
                "password VARCHAR(255) NOT NULL," +
                "fullName VARCHAR(255) NOT NULL," +
                "phone VARCHAR(50)," +
                "role VARCHAR(50) NOT NULL," +
                "isActive TINYINT(1) DEFAULT 1) ENGINE=InnoDB");

            s.execute("CREATE TABLE IF NOT EXISTS restaurants (" +
                "restaurantID INT PRIMARY KEY AUTO_INCREMENT," +
                "ownerUserID INT NOT NULL," +
                "restaurantName VARCHAR(255) NOT NULL," +
                "address VARCHAR(500)," +
                "cuisineType VARCHAR(100)," +
                "rating DOUBLE DEFAULT 0.0," +
                "operatingHours VARCHAR(100)," +
                "FOREIGN KEY (ownerUserID) REFERENCES users(userID)" +
                ") ENGINE=InnoDB");

            s.execute("CREATE TABLE IF NOT EXISTS menu_items (" +
                "itemID INT PRIMARY KEY AUTO_INCREMENT," +
                "restaurantID INT NOT NULL," +
                "name VARCHAR(255) NOT NULL," +
                "description TEXT," +
                "price DOUBLE NOT NULL," +
                "category VARCHAR(100)," +
                "isAvailable TINYINT(1) DEFAULT 1," +
                "FOREIGN KEY (restaurantID) REFERENCES restaurants(restaurantID)" +
                ") ENGINE=InnoDB");

            s.execute("CREATE TABLE IF NOT EXISTS orders (" +
                "orderID INT PRIMARY KEY AUTO_INCREMENT," +
                "customerID INT NOT NULL," +
                "restaurantID INT NOT NULL," +
                "driverID INT DEFAULT 0," +
                "deliveryFee DOUBLE DEFAULT 50.0," +
                "deliveryAddress VARCHAR(500)," +
                "status VARCHAR(50) DEFAULT 'PENDING'," +
                "orderTime VARCHAR(50)," +
                "FOREIGN KEY (customerID) REFERENCES users(userID)," +
                "FOREIGN KEY (restaurantID) REFERENCES restaurants(restaurantID)" +
                ") ENGINE=InnoDB");

            s.execute("CREATE TABLE IF NOT EXISTS order_items (" +
                "orderItemID INT PRIMARY KEY AUTO_INCREMENT," +
                "orderID INT NOT NULL," +
                "menuItemID INT NOT NULL," +
                "itemName VARCHAR(255) NOT NULL," +
                "itemPrice DOUBLE NOT NULL," +
                "quantity INT DEFAULT 1," +
                "FOREIGN KEY (orderID) REFERENCES orders(orderID)," +
                "FOREIGN KEY (menuItemID) REFERENCES menu_items(itemID)" +
                ") ENGINE=InnoDB");

            System.out.println("All tables ready.");
        } catch (SQLException e) {
            System.err.println("Tables error: " + e.getMessage());
        }
    }
}