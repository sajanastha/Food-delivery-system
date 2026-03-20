package com.fooddelivery;

public class Restaurant extends User {
    private int    restaurantID;
    private String restaurantName;
    private String address;
    private String cuisineType;
    private double rating;
    private String operatingHours;

    public Restaurant(int userID, String email, String password,
                      String fullName, String phone) {
        super(userID, email, password, fullName, phone, UserRole.RESTAURANT);
    }

    @Override
    public void accessDashboard() {
        System.out.println("Restaurant Dashboard: " + restaurantName);
    }

    public int    getRestaurantID()              { return restaurantID; }
    public void   setRestaurantID(int id)        { this.restaurantID = id; }
    public String getRestaurantName()            { return restaurantName; }
    public void   setRestaurantName(String n)    { this.restaurantName = n; }
    public String getAddress()                   { return address; }
    public void   setAddress(String a)           { this.address = a; }
    public String getCuisineType()               { return cuisineType; }
    public void   setCuisineType(String c)       { this.cuisineType = c; }
    public double getRating()                    { return rating; }
    public void   setRating(double r)            { this.rating = r; }
    public String getOperatingHours()            { return operatingHours; }
    public void   setOperatingHours(String h)    { this.operatingHours = h; }
}