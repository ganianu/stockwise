
package com.stockwise.service;

import com.stockwise.cache.ProductCacheDTO;
import com.stockwise.exceptions.ResourceNotFoundException;
import com.stockwise.models.Category;
import com.stockwise.models.Product;
import com.stockwise.repository.CategoryRepository;
import com.stockwise.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SelfProductService productService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        product = new Product();
        product.setId(1L);
        product.setName("Wireless Mouse");
        product.setSku("SW-MOUSE-001");
        product.setDescription("Wireless mouse");
        product.setUnitPrice(new BigDecimal("25.00"));
        product.setQuantity(50);
        product.setReorderLevel(10);
        product.setCategory(category);

        lenient().when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
    }

    @Test
    void shouldCreateProductSuccessfully() {

        when(productRepository.existsBySku("SW-MOUSE-001"))
                .thenReturn(false);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.createProduct(product);

        assertEquals("Wireless Mouse", result.getName());
        assertEquals("SW-MOUSE-001", result.getSku());
        assertEquals("Electronics", result.getCategory().getName());

        verify(productRepository).save(product);
    }

    @Test
    void shouldRejectDuplicateProductSku() {

        when(productRepository.existsBySku("SW-MOUSE-001"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(product)
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectProductWithoutCategory() {

        product.setCategory(null);

        when(productRepository.existsBySku("SW-MOUSE-001"))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(product)
        );
    }

    @Test
    void shouldRejectProductWithDeletedCategory() {

        category.setDeleted(true);

        when(productRepository.existsBySku("SW-MOUSE-001"))
                .thenReturn(false);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.createProduct(product)
        );
    }

    @Test
    void shouldGetProductFromRedisCache() {

        ProductCacheDTO cachedDTO = ProductCacheDTO.from(product);

        when(valueOperations.get("PRODUCT_V3:1"))
                .thenReturn(cachedDTO);

        Product result = productService.getProductById(1L);

        assertEquals("Wireless Mouse", result.getName());
        assertEquals("SW-MOUSE-001", result.getSku());

        verify(productRepository, never()).findByIdWithCategory(1L);
    }

    @Test
    void shouldGetProductFromDatabaseWhenCacheMisses() {

        when(valueOperations.get("PRODUCT_V3:1"))
                .thenReturn(null);

        when(productRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(product));

        Product result = productService.getProductById(1L);

        assertEquals("Wireless Mouse", result.getName());

        verify(valueOperations).set(
                eq("PRODUCT_V3:1"),
                any(ProductCacheDTO.class),
                any(Duration.class)
        );
    }

    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {

        when(valueOperations.get("PRODUCT_V3:99"))
                .thenReturn(null);

        when(productRepository.findByIdWithCategory(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(99L)
        );
    }

    @Test
    void shouldRejectDeletedProduct() {

        product.setDeleted(true);

        when(valueOperations.get("PRODUCT_V3:1"))
                .thenReturn(null);

        when(productRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(1L)
        );
    }

    @Test
    void shouldGetAllProductsWithPaginationAndSorting() {

        Page<Product> expectedPage =
                new PageImpl<>(List.of(product));

        PageRequest expectedRequest =
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));

        when(productRepository.findByDeletedFalse(expectedRequest))
                .thenReturn(expectedPage);

        Page<Product> result =
                productService.getAllProducts(0, 10, "name", "asc");

        assertEquals(1, result.getTotalElements());
        assertEquals("Wireless Mouse", result.getContent().get(0).getName());
    }

    @Test
    void shouldRejectNegativePageNumber() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(-1, 10, "name", "asc")
        );
    }

    @Test
    void shouldRejectPageSizeGreaterThanMaximum() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(0, 101, "name", "asc")
        );
    }

    @Test
    void shouldRejectInvalidSortField() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(0, 10, "invalidField", "asc")
        );
    }

    @Test
    void shouldRejectInvalidSortDirection() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(0, 10, "name", "sideways")
        );
    }

    @Test
    void shouldGetLowStockProducts() {

        Page<Product> expectedPage =
                new PageImpl<>(List.of(product));

        PageRequest expectedRequest =
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));

        when(productRepository.findLowStockProducts(expectedRequest))
                .thenReturn(expectedPage);

        Page<Product> result =
                productService.getLowStockProducts(0, 10, "name", "asc");

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldUpdateProductSuccessfully() {

        Product updatedProduct = new Product();
        updatedProduct.setName("Updated Wireless Mouse");

        when(productRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product result =
                productService.updateProduct(1L, updatedProduct);

        assertEquals("Updated Wireless Mouse", result.getName());

        verify(redisTemplate).delete("PRODUCT_V3:1");
        verify(productRepository).save(product);
    }

    @Test
    void shouldRejectDirectQuantityUpdate() {

        Product updatedProduct = new Product();
        updatedProduct.setQuantity(100);

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.updateProduct(1L, updatedProduct)
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldSoftDeleteProductAndEvictCache() {

        when(productRepository.findByIdWithCategory(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productService.deleteProduct(1L);

        assertTrue(product.isDeleted());

        verify(productRepository).save(product);
        verify(redisTemplate).delete("PRODUCT_V3:1");
    }
}
