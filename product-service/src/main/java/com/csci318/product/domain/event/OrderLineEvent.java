package com.csci318.product.domain.event;

public record OrderLineEvent(Long productId, int quantity, double unitPrice) {}
