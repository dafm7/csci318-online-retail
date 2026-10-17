package com.csci318.product.domain;

import com.csci318.product.domain.event.LowStockAlert;
import com.csci318.product.domain.event.StockChangedEvent;
import org.springframework.stereotype.Component;

// Domain rule for the stock stream: a stock level is low when it is below that product's threshold
@Component
public class LowStockPolicy {

    public int thresholdOf(StockChangedEvent level) {
        return level.lowStockThreshold() != null ? level.lowStockThreshold() : Product.DEFAULT_LOW_STOCK_THRESHOLD;
    }

    public boolean isLow(StockChangedEvent level) { return level.stock() < thresholdOf(level); }

    public LowStockAlert alertFor(StockChangedEvent level) {
        return new LowStockAlert(level.productId(), level.name(), level.stock(), thresholdOf(level));
    }
}
