package com.csci318.order.domain.event;

import java.math.BigDecimal;

public record OrderLineEvent(Long productId, int quantity, BigDecimal unitPrice) {}
