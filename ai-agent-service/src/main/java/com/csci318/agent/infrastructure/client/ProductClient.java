package com.csci318.agent.infrastructure.client;

import com.csci318.agent.domain.ProductInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

// REST client for product-service
@Component
public class ProductClient {

    private final RestClient api;

    public ProductClient(@Value("${services.product-url}") String baseUrl) {
        this.api = RestClient.create(baseUrl + "/api/products");
    }

    // Every filter is optional; null filters are left out of the URL
    public List<ProductInfo> find(String keyword, String category, Double minPrice, Double maxPrice,
                                  Boolean inStockOnly, String sort) {
        return List.of(api.get().uri(b -> b
                        .queryParamIfPresent("search", Optional.ofNullable(keyword))
                        .queryParamIfPresent("category", Optional.ofNullable(category))
                        .queryParamIfPresent("minPrice", Optional.ofNullable(minPrice))
                        .queryParamIfPresent("maxPrice", Optional.ofNullable(maxPrice))
                        .queryParamIfPresent("inStock", Optional.ofNullable(inStockOnly))
                        .queryParamIfPresent("sort", Optional.ofNullable(sort))
                        .build())
                .retrieve().body(ProductInfo[].class));
    }

    public List<ProductInfo> inCategory(String category) {
        return List.of(api.get().uri("?category={c}", category).retrieve().body(ProductInfo[].class));
    }

    public ProductInfo get(Long productId) {
        return api.get().uri("/{id}", productId).retrieve().body(ProductInfo.class);
    }

    public List<ProductInfo> all() {
        return List.of(api.get().retrieve().body(ProductInfo[].class));
    }
}
