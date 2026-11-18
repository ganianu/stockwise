
package com.stockwise;

import com.stockwise.models.Category;
import com.stockwise.models.Product;
import com.stockwise.repository.CategoryRepository;
import com.stockwise.repository.ProductRepository;
import com.stockwise.repository.projections.ProductProjection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StockwiseApplicationTests {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldFindProductsByCategoryIdUsingJpql() {

        Category category = createCategory();

        Product product = createProduct(category, "JPQL Test Product");

        List<Product> products =
                productRepository.findProductsByCategoryId(category.getId());

        assertFalse(products.isEmpty());
        assertTrue(products.stream()
                .anyMatch(p -> p.getId().equals(product.getId())));
    }

    @Test
    void shouldFindProductsByCategoryIdUsingNativeQuery() {

        Category category = createCategory();

        Product product = createProduct(category, "Native SQL Test Product");

        List<Product> products =
                productRepository.findProductsByCategoryIdNative(
                        category.getId()
                );

        assertFalse(products.isEmpty());
        assertTrue(products.stream()
                .anyMatch(p -> p.getId().equals(product.getId())));
    }

    @Test
    void shouldReturnProductProjectionByCategoryId() {

        Category category = createCategory();

        Product product = createProduct(category, "Projection Test Product");

        List<ProductProjection> products =
                productRepository.findProductDetailsByCategoryId(
                        category.getId()
                );

        assertFalse(products.isEmpty());

        ProductProjection result = products.stream()
                .filter(p -> p.getId().equals(product.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(product.getId(), result.getId());
        assertEquals("Projection Test Product", result.getName());
    }

    @Test
    void shouldFetchProductWithCategoryDetails() {

        Category category = createCategory();

        Product product = createProduct(
                category,
                "Fetch Graph Test Product"
        );

        Product result = productRepository.findByIdWithCategory(
                product.getId()
        ).orElseThrow();

        assertEquals(product.getId(), result.getId());
        assertNotNull(result.getCategory());
        assertEquals(category.getId(), result.getCategory().getId());
        assertEquals(category.getName(), result.getCategory().getName());
    }

    @Test
    void shouldFindLowStockProducts() {

        Category category = createCategory();

        Product lowStockProduct = createProduct(
                category,
                "Low Stock Test Product"
        );

        lowStockProduct.setQuantity(5);
        lowStockProduct.setReorderLevel(10);

        productRepository.save(lowStockProduct);

        Product normalStockProduct = createProduct(
                category,
                "Normal Stock Test Product"
        );

        normalStockProduct.setQuantity(50);
        normalStockProduct.setReorderLevel(10);

        productRepository.save(normalStockProduct);

        Page<Product> products =
                productRepository.findLowStockProducts(
                        PageRequest.of(0, 20)
                );

        assertTrue(products.getContent().stream()
                .anyMatch(p -> p.getId().equals(lowStockProduct.getId())));

        assertFalse(products.getContent().stream()
                .anyMatch(p -> p.getId().equals(normalStockProduct.getId())));
    }

    private Category createCategory() {

        Category category = new Category();

        category.setName("Test Category " + UUID.randomUUID());

        return categoryRepository.save(category);
    }

    private Product createProduct(Category category, String name) {

        Product product = new Product();

        product.setName(name);
        product.setSku("TEST-" + UUID.randomUUID());
        product.setDescription("Repository testing product");
        product.setUnitPrice(new BigDecimal("25.00"));
        product.setQuantity(50);
        product.setReorderLevel(10);
        product.setCategory(category);

        return productRepository.save(product);
    }
}
