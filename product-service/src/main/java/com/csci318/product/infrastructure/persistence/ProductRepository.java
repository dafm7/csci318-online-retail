package com.csci318.product.infrastructure.persistence;

import com.csci318.product.domain.Category;
import com.csci318.product.domain.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Every filter is optional: a null parameter means "don't filter on this"
    @Query("""
            SELECT p FROM Product p
            WHERE (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:category IS NULL OR p.category = :category)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:inStockOnly = FALSE OR p.stock > 0)
            """)
    List<Product> search(String search, Category category, Double minPrice, Double maxPrice,
                         boolean inStockOnly, Sort sort);
}
