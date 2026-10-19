package com.csci318.product.domain;

import java.util.Arrays;

public enum Category {
    CLOTHES, ELECTRONICS, SPORTS, HOME;

    public static Category from(String value) {
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(value == null ? "" : value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "category must be one of " + Arrays.toString(values())));
    }
}
