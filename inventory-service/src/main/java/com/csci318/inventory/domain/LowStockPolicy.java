package com.csci318.inventory.domain;

import com.csci318.inventory.domain.event.LowStockAlert;
import com.csci318.inventory.domain.event.StockChangedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Domain rule: a product is low on stock when it has fewer units than the threshold
@Component
public class LowStockPolicy {

    private final int threshold;

    public LowStockPolicy(@Value("${inventory.low-stock-threshold}") int threshold) { this.threshold = threshold; }

    public boolean isLow(StockChangedEvent level) { return level.stock() < threshold; }

    public LowStockAlert alertFor(StockChangedEvent level) {
        return new LowStockAlert(level.productId(), level.name(), level.stock(), threshold);
    }
}
