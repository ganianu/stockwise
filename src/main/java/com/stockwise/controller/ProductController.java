
package com.stockwise.controller;

import com.stockwise.dto.ProductRequestDTO;
import com.stockwise.dto.ProductUpdateRequestDTO;
import com.stockwise.dto.ProductResponseDTO;
import com.stockwise.models.Category;
import com.stockwise.models.Product;
import com.stockwise.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody ProductRequestDTO request
    ) {
        Product product = toProduct(request);

        Product savedProduct = productService.createProduct(product);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponseDTO(savedProduct));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(
            @PathVariable Long id
    ) {
        Product product = productService.getProductById(id);

        return ResponseEntity.ok(toResponseDTO(product));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        Page<ProductResponseDTO> products =
                productService.getAllProducts(
                        pageNumber,
                        pageSize,
                        sortBy,
                        sortDirection
                ).map(this::toResponseDTO);

        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequestDTO request
    ) {
        Product product = toUpdateProduct(request);

        Product updatedProduct = productService.updateProduct(id, product);

        return ResponseEntity.ok(toResponseDTO(updatedProduct));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id
    ) {
        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Page<ProductResponseDTO>> getLowStockProducts(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        Page<ProductResponseDTO> products =
                productService.getLowStockProducts(
                        pageNumber,
                        pageSize,
                        sortBy,
                        sortDirection
                ).map(this::toResponseDTO);

        return ResponseEntity.ok(products);
    }

    private Product toProduct(ProductRequestDTO request) {

        Product product = new Product();

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setUnitPrice(request.getUnitPrice());
        product.setQuantity(request.getQuantity());
        product.setReorderLevel(request.getReorderLevel());

        Category category = new Category();
        category.setId(request.getCategoryId());

        product.setCategory(category);

        return product;
    }

    private Product toUpdateProduct(ProductUpdateRequestDTO request) {

        Product product = new Product();

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setUnitPrice(request.getUnitPrice());
        product.setReorderLevel(request.getReorderLevel());

        if (request.getCategoryId() != null) {
            Category category = new Category();
            category.setId(request.getCategoryId());
            product.setCategory(category);
        }

        return product;
    }

    private ProductResponseDTO toResponseDTO(Product product) {

        Long categoryId = null;
        String categoryName = null;

        if (product.getCategory() != null) {
            categoryId = product.getCategory().getId();
            categoryName = product.getCategory().getName();
        }

        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getUnitPrice(),
                product.getQuantity(),
                product.getReorderLevel(),
                categoryId,
                categoryName,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
