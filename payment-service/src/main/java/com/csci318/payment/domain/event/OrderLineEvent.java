package com.csci318.payment.domain.event;

import java.math.BigDecimal;

public record OrderLineEvent(Long productId, int quantity, BigDecimal unitPrice) {}
