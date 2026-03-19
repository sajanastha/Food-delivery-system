package java;


import java.util.ArrayList;
import java.util.List;

public class Customer extends User {

    private String deliveryAddress;
    private List<OrderItem> cart = new ArrayList<>();

    public Customer(int userID, String email, String password,
                    String fullName, String phone) {
        super(userID, email, password, fullName, phone, UserRole.CUSTOMER);
    }

    @Override
    public void accessDashboard() {
        System.out.println("Customer Dashboard loaded for: " + getFullName());
    }

    // Add item to cart — if already exists just increase quantity
    public void addToCart(OrderItem item) {
        for (OrderItem existing : cart) {
            if (existing.getMenuItemID() == item.getMenuItemID()) {
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                return;
            }
        }
        cart.add(item);
    }

    public void removeFromCart(int menuItemID) {
        cart.removeIf(i -> i.getMenuItemID() == menuItemID);
    }

    public void clearCart() {
        cart.clear();
    }

    public double getCartSubtotal() {
        return cart.stream().mapToDouble(OrderItem::getSubtotal).sum();
    }

    public List<OrderItem> getCart()            { return cart; }
    public String getDeliveryAddress()           { return deliveryAddress; }
    public void setDeliveryAddress(String addr)  { this.deliveryAddress = addr; }
}