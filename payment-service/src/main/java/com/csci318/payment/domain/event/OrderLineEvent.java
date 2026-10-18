package com.csci318.payment.domain.event;


public record OrderLineEvent(Long productId, int quantity, double unitPrice) {}
