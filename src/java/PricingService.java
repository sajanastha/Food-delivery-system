package java;


import java.util.List;

public class PricingService {

    private static final double DELIVERY_FEE = 50.0;

    public double getDeliveryFee()  { return DELIVERY_FEE; }

    public double itemsTotal(List<OrderItem> cart) {
        return cart.stream()
                   .mapToDouble(OrderItem::getSubtotal)
                   .sum();
    }

    public double orderTotal(List<OrderItem> cart) {
        return itemsTotal(cart) + DELIVERY_FEE;
    }

    public String breakdown(List<OrderItem> cart) {
        if (cart.isEmpty()) return "Your cart is empty.";
        StringBuilder sb = new StringBuilder();
        sb.append(" Order Summary \n");
        for (OrderItem i : cart)
            sb.append(String.format("  %-20s x%d  NPR %.2f%n",
                    i.getItemName(), i.getQuantity(),
                    i.getSubtotal()));
        sb.append("─────────────────────────────\n");
        sb.append(String.format("Items :  NPR %.2f%n",
                itemsTotal(cart)));
        sb.append(String.format("Delivery: NPR %.2f%n",
                DELIVERY_FEE));
        sb.append("─────────────────────────────\n");
        sb.append(String.format("TOTAL :  NPR %.2f%n",
                orderTotal(cart)));
        return sb.toString();
    }
}