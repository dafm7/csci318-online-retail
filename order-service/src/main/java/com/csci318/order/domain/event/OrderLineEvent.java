package com.csci318.order.domain.event;


public record OrderLineEvent(Long productId, int quantity, double unitPrice) {}
