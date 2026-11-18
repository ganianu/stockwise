
package com.stockwise.cache;

import com.stockwise.models.Category;
import com.stockwise.models.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductCacheDTO(
        Long id,
        String name,
        String sku,
        String description,
        BigDecimal unitPrice,
        Integer quantity,
        Integer reorderLevel,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String lastModifiedBy,
        boolean deleted
) {

    public static ProductCacheDTO from(Product product) {

        Category category = product.getCategory();

        return new ProductCacheDTO(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getUnitPrice(),
                product.getQuantity(),
                product.getReorderLevel(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getCreatedBy(),
                product.getLastModifiedBy(),
                product.isDeleted()
        );
    }

    public Product toProduct() {

        Product product = new Product();

        product.setId(id);
        product.setName(name);
        product.setSku(sku);
        product.setDescription(description);
        product.setUnitPrice(unitPrice);
        product.setQuantity(quantity);
        product.setReorderLevel(reorderLevel);
        product.setCreatedAt(createdAt);
        product.setUpdatedAt(updatedAt);
        product.setCreatedBy(createdBy);
        product.setLastModifiedBy(lastModifiedBy);
        product.setDeleted(deleted);

        if (categoryId != null) {
            Category category = new Category();
            category.setId(categoryId);
            category.setName(categoryName);
            product.setCategory(category);
        }

        return product;
    }
}
