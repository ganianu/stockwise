
package com.stockwise.service;

import com.stockwise.exceptions.ResourceNotFoundException;
import com.stockwise.models.Category;
import com.stockwise.models.Product;
import com.stockwise.repository.CategoryRepository;
import com.stockwise.repository.ProductRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SelfCategoryService implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public SelfCategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Category createCategory(String name) {

        if (categoryRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException(
                    "Category already exists: " + name
            );
        }

        Category category = new Category();
        category.setName(name.trim());

        return categoryRepository.save(category);
    }

    @Override
    public Category getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with ID: " + id
                ));

        if (category.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Category not found with ID: " + id
            );
        }

        return category;
    }

    @Override
    public List<Category> getAllCategories() {

        return categoryRepository.findAll()
                .stream()
                .filter(category -> !category.isDeleted())
                .toList();
    }

    @Override
    public Category updateCategory(Long id, String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Category name is required"
            );
        }

        Category existingCategory = getCategoryById(id);
        String updatedName = name.trim();

        if (!updatedName.equals(existingCategory.getName()) &&
                categoryRepository.existsByName(updatedName)) {
            throw new IllegalArgumentException(
                    "Category already exists: " + updatedName
            );
        }

        existingCategory.setName(updatedName);

        return categoryRepository.save(existingCategory);
    }

    @Override
    public List<Product> getProductsInCategory(Long categoryId) {

        getCategoryById(categoryId);

        return productRepository.findProductsByCategoryId(categoryId);
    }

    @Override
    public void deleteCategory(Long id) {

        Category category = getCategoryById(id);

        category.setDeleted(true);

        categoryRepository.save(category);
    }
}
