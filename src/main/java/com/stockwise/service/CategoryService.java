
package com.stockwise.service;

import com.stockwise.models.Category;
import com.stockwise.models.Product;

import java.util.List;

public interface CategoryService {

    Category createCategory(String name);

    Category getCategoryById(Long id);

    List<Category> getAllCategories();

    Category updateCategory(Long id, String name);

    List<Product> getProductsInCategory(Long categoryId);

    void deleteCategory(Long id);
}
