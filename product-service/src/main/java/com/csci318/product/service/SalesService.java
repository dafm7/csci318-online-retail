package com.csci318.product.service;

import com.csci318.product.domain.ProductSales;
import com.csci318.product.domain.event.StockChangedEvent;
import com.csci318.product.infrastructure.stream.SalesStore;
import com.csci318.product.infrastructure.stream.StockLevelStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

// Use cases for real-time sales analysis
@Service
public class SalesService {

    public record ProductSalesView(Long productId, String name, long unitsSold, double revenue, long orders) {}
    public record MinuteSalesView(Long productId, String name, Instant windowStart, Instant windowEnd, long unitsSold) {}

    private final SalesStore sales;
    private final StockLevelStore stock;

    public SalesService(SalesStore sales, StockLevelStore stock) {
        this.sales = sales;
        this.stock = stock;
    }

    // Best sellers first
    public List<ProductSalesView> totals() {
        return sales.allTotals().stream()
                .sorted(Comparator.comparingLong(ProductSales::unitsSold).reversed())
                .map(s -> new ProductSalesView(s.productId(), nameOf(s.productId()), s.unitsSold(), s.revenue(), s.orders()))
                .toList();
    }

    // Units sold per product in each 1-minute window over the last few minutes, newest first
    public List<MinuteSalesView> perMinute(int minutes) {
        Instant now = Instant.now();
        return sales.unitsPerMinute(now.minus(Duration.ofMinutes(minutes)), now).stream()
                .sorted(Comparator.comparing(SalesStore.WindowCount::windowStart).reversed()
                        .thenComparing(Comparator.comparingLong(SalesStore.WindowCount::units).reversed()))
                .map(w -> new MinuteSalesView(w.productId(), nameOf(w.productId()), w.windowStart(), w.windowEnd(), w.units()))
                .toList();
    }

    // Product names come from the live stock table
    private String nameOf(Long productId) {
        return stock.findByProductId(productId).map(StockChangedEvent::name).orElse(null);
    }
}
