package java;

public class MenuItem {
    private int    itemID;
    private int    restaurantID;
    private String name;
    private String description;
    private double price;
    private String category;
    private boolean isAvailable = true;

    public MenuItem() {}

    public MenuItem(int itemID, int restaurantID, String name,
                    String description, double price, String category) {
        this.itemID       = itemID;
        this.restaurantID = restaurantID;
        this.name         = name;
        this.description  = description;
        this.price        = price;
        this.category     = category;
    }

    public String getFormattedPrice() {
        return String.format("NPR %.2f", price);
    }

    public int     getItemID()                  { return itemID; }
    public void    setItemID(int id)            { this.itemID = id; }
    public int     getRestaurantID()            { return restaurantID; }
    public void    setRestaurantID(int id)      { this.restaurantID = id; }
    public String  getName()                    { return name; }
    public void    setName(String n)            { this.name = n; }
    public String  getDescription()             { return description; }
    public void    setDescription(String d)     { this.description = d; }
    public double  getPrice()                   { return price; }
    public void    setPrice(double p)           { this.price = p; }
    public String  getCategory()                { return category; }
    public void    setCategory(String c)        { this.category = c; }
    public boolean isAvailable()                { return isAvailable; }
    public void    setAvailable(boolean a)      { this.isAvailable = a; }
}