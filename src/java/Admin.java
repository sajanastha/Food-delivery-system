package java;


public class Admin extends User {

    public Admin(int userID, String email, String password,
                 String fullName, String phone) {
        super(userID, email, password, fullName, phone, UserRole.ADMIN);
    }

    @Override
    public void accessDashboard() {
        System.out.println("Admin Dashboard loaded for: " + getFullName());
    }
}
