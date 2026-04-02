package com.fooddelivery;

import java.time.LocalDate;

public class SalesReport {
    private String    reportType;
    private LocalDate fromDate, toDate;
    private int       totalOrders;
    private double    totalRevenue;
    private String    topSellingItem;

    public SalesReport(String reportType, LocalDate from,
                       LocalDate to, int totalOrders,
                       double totalRevenue, String topItem) {
        this.reportType     = reportType;
        this.fromDate       = from;
        this.toDate         = to;
        this.totalOrders    = totalOrders;
        this.totalRevenue   = totalRevenue;
        this.topSellingItem = topItem;
    }

    public String getSummary() {
        return String.format(
            "%s REPORT\nPeriod: %s to %s\n" +
            "Orders: %d\nRevenue: NPR %.2f\nTop Item: %s",
            reportType, fromDate, toDate,
            totalOrders, totalRevenue, topSellingItem);
    }

    // ── Getters (were missing — caused red lines in controller) ──
    public int    getTotalOrders()    { return totalOrders; }
    public double getTotalRevenue()   { return totalRevenue; }
    public String getTopSellingItem() { return topSellingItem; }
}