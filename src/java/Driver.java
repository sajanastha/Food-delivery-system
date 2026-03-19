package java;

public class Driver extends User {

    private String licenseNumber;
    private String vehicleType;
    private boolean isAvailable  = true;
    private int currentOrderID   = 0;

    public Driver(int userID, String email, String password,
                  String fullName, String phone) {
        super(userID, email, password, fullName, phone, UserRole.DRIVER);
    }

    @Override
    public void accessDashboard() {
        System.out.println("Driver Dashboard loaded for: " + getFullName());
    }

    public void acceptOrder(int orderID) {
        this.currentOrderID = orderID;
        this.isAvailable    = false;
    }

    public void completeDelivery() {
        this.currentOrderID = 0;
        this.isAvailable    = true;
    }

    // Getters & Setters
    public String getLicenseNumber()          { return licenseNumber; }
    public void setLicenseNumber(String l)    { this.licenseNumber = l; }
    public String getVehicleType()            { return vehicleType; }
    public void setVehicleType(String v)      { this.vehicleType = v; }
    public boolean isAvailable()              { return isAvailable; }
    public void setAvailable(boolean a)       { this.isAvailable = a; }
    public int getCurrentOrderID()            { return currentOrderID; }
    public void setCurrentOrderID(int id)     { this.currentOrderID = id; }
}