package java;

public class OrderItem {

    private int orderItemID;
    private int orderID;
    private int menuItemID;
    private String itemName;
    private double itemPrice;
    private int quantity;

    public OrderItem(int menuItemID, String itemName,
                     double itemPrice, int quantity) {
        this.menuItemID = menuItemID;
        this.itemName   = itemName;
        this.itemPrice  = itemPrice;
        this.quantity   = quantity;
    }

    public double getSubtotal() {
        return itemPrice * quantity;
    }

    public String getSummary() {
        return String.format("%s  x%d  =  NPR %.2f",
                itemName, quantity, getSubtotal());
    }

    // Getters & Setters
    public int getOrderItemID()                { return orderItemID; }
    public void setOrderItemID(int id)         { this.orderItemID = id; }
    public int getOrderID()                    { return orderID; }
    public void setOrderID(int id)             { this.orderID = id; }
    public int getMenuItemID()                 { return menuItemID; }
    public void setMenuItemID(int id)          { this.menuItemID = id; }
    public String getItemName()                { return itemName; }
    public void setItemName(String n)          { this.itemName = n; }
    public double getItemPrice()               { return itemPrice; }
    public void setItemPrice(double p)         { this.itemPrice = p; }
    public int getQuantity()                   { return quantity; }
    public void setQuantity(int q)             { this.quantity = q; }

    @Override
    public String toString() { return getSummary(); }
}
