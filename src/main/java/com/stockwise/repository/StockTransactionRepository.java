
package com.stockwise.repository;

import com.stockwise.models.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTransactionRepository
        extends JpaRepository<StockTransaction, Long> {

    @EntityGraph(attributePaths = {"product", "supplier"})
    Page<StockTransaction> findByProductIdOrderByCreatedAtDesc(
            Long productId, Pageable pageable
    );

    List<StockTransaction> findByTransactionType(
            StockTransaction.TransactionType transactionType
    );
}
