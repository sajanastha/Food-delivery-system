package java;

public class UserFactory {
    public static User createUser(String role, int userID,
            String email, String password,
            String fullName, String phone) {
        return switch (role.toUpperCase().trim()) {
            case "CUSTOMER"   ->
                new Customer(userID, email, password, fullName, phone);
            case "RESTAURANT" ->
                new Restaurant(userID, email, password, fullName, phone);
            case "DRIVER"     ->
                new Driver(userID, email, password, fullName, phone);
            case "ADMIN"      ->
                new Admin(userID, email, password, fullName, phone);
            default ->
                throw new IllegalArgumentException("Unknown role: " + role);
        };
    }
}