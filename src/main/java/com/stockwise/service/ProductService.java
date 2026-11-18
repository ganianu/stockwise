
package com.stockwise.service;

import com.stockwise.models.Product;
import org.springframework.data.domain.Page;

public interface ProductService {

    Product createProduct(Product product);

    Product getProductById(Long id);

    Page<Product> getAllProducts(
            int pageNumber,
            int pageSize,
            String sortBy,
            String sortDirection
    );

    Product updateProduct(Long id, Product product);

    void deleteProduct(Long id);

    Page<Product> getLowStockProducts(
            int pageNumber,
            int pageSize,
            String sortBy,
            String sortDirection
    );
}
