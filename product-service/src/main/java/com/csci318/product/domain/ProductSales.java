package com.csci318.product.domain;

import com.csci318.product.domain.event.OrderLineEvent;

// Running sales totals for one product (built by SalesStream from paid orders)
public record ProductSales(Long productId, long unitsSold, double revenue, long orders) {

    public static ProductSales none() { return new ProductSales(null, 0, 0, 0); }

    // Domain rule: a paid order adds its quantity, amount and one order to the totals
    public ProductSales add(OrderLineEvent line) {
        double lineRevenue = line.unitPrice() * line.quantity();
        double newRevenue = Math.round((revenue + lineRevenue) * 100) / 100.0;
        return new ProductSales(line.productId(), unitsSold + line.quantity(), newRevenue, orders + 1);
    }
}
