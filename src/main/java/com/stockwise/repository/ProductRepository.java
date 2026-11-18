
package com.stockwise.repository;

import com.stockwise.models.Product;
import com.stockwise.repository.projections.ProductProjection;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE p.quantity <= p.reorderLevel AND p.deleted = false")
    Page<Product> findLowStockProducts(Pageable pageable);

    // Fetch product and category together.
    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    // Find active products by category ID.
    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.deleted = false")
    List<Product> findProductsByCategoryId(@Param("categoryId") Long categoryId);

    // Native SQL Query: Find products by category ID.
    @Query(value = "SELECT * FROM products WHERE category_id = :categoryId AND deleted = false",
            nativeQuery = true)
    List<Product> findProductsByCategoryIdNative(@Param("categoryId") Long categoryId);

    // Product Projection: Retrieve only product ID and name.
    @Query("SELECT p.id AS id, p.name AS name FROM Product p WHERE p.category.id = :categoryId AND p.deleted = false")
    List<ProductProjection> findProductDetailsByCategoryId(@Param("categoryId") Long categoryId);

    // Pessimistic locking for concurrent stock updates.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
