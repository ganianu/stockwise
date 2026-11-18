
package com.stockwise.controller;

import com.stockwise.dto.CategoryRequestDTO;
import com.stockwise.dto.CategoryResponseDTO;
import com.stockwise.dto.ProductResponseDTO;
import com.stockwise.models.Category;
import com.stockwise.models.Product;
import com.stockwise.service.CategoryService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CategoryRequestDTO request
    ) {
        Category category = categoryService.createCategory(
                request.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponseDTO(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(
            @PathVariable Long id
    ) {
        Category category = categoryService.getCategoryById(id);

        return ResponseEntity.ok(toResponseDTO(category));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {

        List<CategoryResponseDTO> categories =
                categoryService.getAllCategories()
                        .stream()
                        .map(this::toResponseDTO)
                        .toList();

        return ResponseEntity.ok(categories);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO request
    ) {
        Category category = categoryService.updateCategory(
                id,
                request.getName()
        );

        return ResponseEntity.ok(toResponseDTO(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id
    ) {
        categoryService.deleteCategory(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{categoryId}/products")
    public ResponseEntity<List<ProductResponseDTO>> getProductsInCategory(
            @PathVariable Long categoryId
    ) {
        List<ProductResponseDTO> products =
                categoryService.getProductsInCategory(categoryId)
                        .stream()
                        .map(this::toProductResponseDTO)
                        .toList();

        return ResponseEntity.ok(products);
    }

    private CategoryResponseDTO toResponseDTO(Category category) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getName()
        );
    }

    private ProductResponseDTO toProductResponseDTO(Product product) {

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
