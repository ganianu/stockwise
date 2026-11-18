
package com.stockwise.service;

import com.stockwise.cache.ProductCacheDTO;
import com.stockwise.exceptions.ResourceNotFoundException;
import com.stockwise.models.Category;
import com.stockwise.models.Product;
import com.stockwise.repository.CategoryRepository;
import com.stockwise.repository.ProductRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;

@Service
public class SelfProductService implements ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final String PRODUCT_CACHE_KEY_PREFIX = "PRODUCT_V3:";

    private static final Duration PRODUCT_CACHE_TTL =
            Duration.ofMinutes(30);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "sku",
            "unitPrice",
            "quantity",
            "reorderLevel",
            "createdAt"
    );

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public SelfProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Product createProduct(Product product) {

        if (productRepository.existsBySku(product.getSku())) {
            throw new IllegalArgumentException(
                    "Product already exists with SKU: " + product.getSku()
            );
        }

        if (product.getCategory() == null ||
                product.getCategory().getId() == null) {
            throw new IllegalArgumentException(
                    "A valid category is required"
            );
        }

        Category category = categoryRepository.findById(
                product.getCategory().getId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Category not found with ID: " + product.getCategory().getId()
        ));

        if (category.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Category not found with ID: " + category.getId()
            );
        }

        product.setCategory(category);

        return productRepository.save(product);
    }

    @Override
    public Product getProductById(Long id) {

        String cacheKey = getProductCacheKey(id);

        Object cachedProduct = redisTemplate.opsForValue().get(cacheKey);

        if (cachedProduct instanceof ProductCacheDTO cachedDTO) {
            System.out.println("REDIS CACHE HIT for product ID: " + id);
            return cachedDTO.toProduct();
        }

        System.out.println("REDIS CACHE MISS for product ID: " + id);

        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + id
                ));

        if (product.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Product not found with ID: " + id
            );
        }

        ProductCacheDTO productCacheDTO = ProductCacheDTO.from(product);

        redisTemplate.opsForValue().set(
                cacheKey,
                productCacheDTO,
                PRODUCT_CACHE_TTL
        );

        return product;
    }

    @Override
    public Page<Product> getAllProducts(
            int pageNumber,
            int pageSize,
            String sortBy,
            String sortDirection
    ) {
        validatePagination(pageNumber, pageSize);

        Sort sort = createSort(sortBy, sortDirection);

        return productRepository.findByDeletedFalse(
                PageRequest.of(pageNumber, pageSize, sort)
        );
    }

    @Override
    public Page<Product> getLowStockProducts(
            int pageNumber,
            int pageSize,
            String sortBy,
            String sortDirection
    ) {
        validatePagination(pageNumber, pageSize);

        Sort sort = createSort(sortBy, sortDirection);

        return productRepository.findLowStockProducts(
                PageRequest.of(pageNumber, pageSize, sort)
        );
    }

    private void validatePagination(int pageNumber, int pageSize) {

        if (pageNumber < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (pageSize <= 0 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
    }

    private Sort createSort(String sortBy, String sortDirection) {

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "name";
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + sortBy
            );
        }

        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "asc";
        }

        Sort.Direction direction;

        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Sort direction must be 'asc' or 'desc'"
            );
        }

        return Sort.by(direction, sortBy);
    }

    @Override
    public void deleteProduct(Long id) {

        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + id
                ));

        if (product.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Product not found with ID: " + id
            );
        }

        product.setDeleted(true);
        productRepository.save(product);

        evictProductCache(id);
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {

        if (updatedProduct.getQuantity() != null) {
            throw new IllegalArgumentException(
                    "Product quantity cannot be updated directly. "
                            + "Use stock-in, stock-out or stock-adjustment transactions."
            );
        }

        Product existingProduct = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + id
                ));

        if (existingProduct.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Product not found with ID: " + id
            );
        }

        if (updatedProduct.getSku() != null &&
                !updatedProduct.getSku().equals(existingProduct.getSku())) {

            if (productRepository.existsBySku(updatedProduct.getSku())) {
                throw new IllegalArgumentException(
                        "SKU already exists: " + updatedProduct.getSku()
                );
            }

            existingProduct.setSku(updatedProduct.getSku());
        }

        if (updatedProduct.getName() != null) {
            existingProduct.setName(updatedProduct.getName());
        }

        if (updatedProduct.getDescription() != null) {
            existingProduct.setDescription(updatedProduct.getDescription());
        }

        if (updatedProduct.getUnitPrice() != null) {
            existingProduct.setUnitPrice(updatedProduct.getUnitPrice());
        }

        if (updatedProduct.getReorderLevel() != null) {
            existingProduct.setReorderLevel(updatedProduct.getReorderLevel());
        }

        if (updatedProduct.getCategory() != null &&
                updatedProduct.getCategory().getId() != null) {

            Category category = categoryRepository.findById(
                    updatedProduct.getCategory().getId()
            ).orElseThrow(() -> new ResourceNotFoundException(
                    "Category not found with ID: "
                            + updatedProduct.getCategory().getId()
            ));

            if (category.isDeleted()) {
                throw new ResourceNotFoundException(
                        "Category not found with ID: " + category.getId()
                );
            }

            existingProduct.setCategory(category);
        }

        Product savedProduct = productRepository.save(existingProduct);

        evictProductCache(id);

        return savedProduct;
    }

    private String getProductCacheKey(Long id) {
        return PRODUCT_CACHE_KEY_PREFIX + id;
    }

    private void evictProductCache(Long id) {
        redisTemplate.delete(getProductCacheKey(id));
    }
}
