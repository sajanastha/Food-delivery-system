package java;

import java.util.ArrayList;
import java.util.List;

public class Order {

    private int orderID;
    private int customerID;
    private int restaurantID;
    private int driverID;
    private List<OrderItem> items = new ArrayList<>();
    private double deliveryFee;
    private String deliveryAddress;
    private OrderStatus status = OrderStatus.PENDING;

    public Order(int customerID, int restaurantID,
                 String deliveryAddress, double deliveryFee) {
        this.customerID      = customerID;
        this.restaurantID    = restaurantID;
        this.deliveryAddress = deliveryAddress;
        this.deliveryFee     = deliveryFee;
    }

    public double getItemsTotal() {
        return items.stream().mapToDouble(OrderItem::getSubtotal).sum();
    }

    public double getTotalAmount() {
        return getItemsTotal() + deliveryFee;
    }

    public String getOrderSummary() {
        return "Order #" + orderID + " | " + status
             + " | NPR " + String.format("%.2f", getTotalAmount());
    }

    public int getOrderID()                     { return orderID; }
    public void setOrderID(int id)              { this.orderID = id; }
    public int getCustomerID()                  { return customerID; }
    public void setCustomerID(int id)           { this.customerID = id; }
    public int getRestaurantID()                { return restaurantID; }
    public void setRestaurantID(int id)         { this.restaurantID = id; }
    public int getDriverID()                    { return driverID; }
    public void setDriverID(int id)             { this.driverID = id; }
    public List<OrderItem> getItems()           { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public double getDeliveryFee()              { return deliveryFee; }
    public String getDeliveryAddress()          { return deliveryAddress; }
    public OrderStatus getStatus()              { return status; }
    public void setStatus(OrderStatus s)        { this.status = s; }
}