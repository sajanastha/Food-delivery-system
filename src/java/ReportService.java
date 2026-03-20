package java;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private final OrderDAO orderDAO = new OrderDAO();

    public SalesReport weeklyReport(int restaurantId)
            throws SQLException {
        LocalDate today = LocalDate.now();
        return build(restaurantId,
                today.minusDays(7), today, "WEEKLY");
    }

    public SalesReport monthlyReport(int restaurantId)
            throws SQLException {
        LocalDate today = LocalDate.now();
        return build(restaurantId,
                today.minusDays(30), today, "MONTHLY");
    }

    private SalesReport build(int restaurantId,
            LocalDate from, LocalDate to, String type)
            throws SQLException {
        List<Order> orders = orderDAO.getCompletedInRange(
                restaurantId, from.toString(), to.toString());

        double revenue = orders.stream()
                .mapToDouble(Order::getTotalAmount).sum();

        Map<String, Integer> freq = new HashMap<>();
        for (Order o : orders)
            for (OrderItem i : o.getItems())
                freq.merge(i.getItemName(),
                        i.getQuantity(), Integer::sum);

        String top = freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No completed orders yet");

        return new SalesReport(type, from, to,
                orders.size(), revenue, top);
    }
}
