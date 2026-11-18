
package com.stockwise.service;

import com.stockwise.models.StockTransaction;
import org.springframework.data.domain.Page;

public interface StockTransactionService {

    StockTransaction recordStockIn(
            Long productId,
            Integer quantity,
            Long supplierId,
            String notes
    );

    StockTransaction recordStockOut(
            Long productId,
            Integer quantity,
            String notes
    );

    StockTransaction adjustStock(
            Long productId,
            Integer quantityChange,
            String notes
    );

    Page<StockTransaction> getTransactionsForProduct(
            Long productId,
            int pageNumber,
            int pageSize
    );
}
