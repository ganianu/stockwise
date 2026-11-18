
package com.stockwise.service;

import com.stockwise.exceptions.ResourceNotFoundException;
import com.stockwise.models.Product;
import com.stockwise.models.StockTransaction;
import com.stockwise.models.Supplier;
import com.stockwise.repository.ProductRepository;
import com.stockwise.repository.StockTransactionRepository;
import com.stockwise.repository.SupplierRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SelfStockTransactionService implements StockTransactionService {

    private static final String PRODUCT_CACHE_KEY_PREFIX = "PRODUCT_V3:";

    private final StockTransactionRepository stockTransactionRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public SelfStockTransactionService(
            StockTransactionRepository stockTransactionRepository,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.stockTransactionRepository = stockTransactionRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public StockTransaction recordStockIn(
            Long productId,
            Integer quantity,
            Long supplierId,
            String notes
    ) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Stock-in quantity must be greater than zero"
            );
        }

        Product product = getActiveProductForUpdate(productId);

        Supplier supplier = null;

        if (supplierId != null) {
            supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Supplier not found with ID: " + supplierId
                    ));

            if (supplier.isDeleted()) {
                throw new ResourceNotFoundException(
                        "Supplier not found with ID: " + supplierId
                );
            }
        }

        product.setQuantity(Math.addExact(product.getQuantity(), quantity));
        productRepository.save(product);

        StockTransaction transaction = saveTransaction(
                product,
                StockTransaction.TransactionType.STOCK_IN,
                quantity,
                supplier,
                notes
        );

        evictProductCacheAfterCommit(productId);

        return transaction;
    }

    @Override
    @Transactional
    public StockTransaction recordStockOut(
            Long productId,
            Integer quantity,
            String notes
    ) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Stock-out quantity must be greater than zero"
            );
        }

        Product product = getActiveProductForUpdate(productId);

        if (product.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock available"
            );
        }

        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);

        StockTransaction transaction = saveTransaction(
                product,
                StockTransaction.TransactionType.STOCK_OUT,
                -quantity,
                null,
                notes
        );

        evictProductCacheAfterCommit(productId);

        return transaction;
    }

    @Override
    @Transactional
    public StockTransaction adjustStock(
            Long productId,
            Integer quantityChange,
            String notes
    ) {
        if (quantityChange == null || quantityChange == 0) {
            throw new IllegalArgumentException(
                    "Stock adjustment cannot be zero"
            );
        }

        Product product = getActiveProductForUpdate(productId);

        int adjustedQuantity = Math.addExact(
                product.getQuantity(),
                quantityChange
        );

        if (adjustedQuantity < 0) {
            throw new IllegalArgumentException(
                    "Stock adjustment cannot result in negative inventory"
            );
        }

        product.setQuantity(adjustedQuantity);
        productRepository.save(product);

        StockTransaction transaction = saveTransaction(
                product,
                StockTransaction.TransactionType.ADJUSTMENT,
                quantityChange,
                null,
                notes
        );

        evictProductCacheAfterCommit(productId);

        return transaction;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockTransaction> getTransactionsForProduct(
            Long productId,
            int pageNumber,
            int pageSize
    ) {
        getActiveProduct(productId);

        if (pageNumber < 0 || pageSize <= 0) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters"
            );
        }

        return stockTransactionRepository
                .findByProductIdOrderByCreatedAtDesc(
                        productId,
                        PageRequest.of(pageNumber, pageSize)
                );
    }

    private Product getActiveProductForUpdate(Long productId) {

        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + productId
                ));

        if (product.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Product not found with ID: " + productId
            );
        }

        return product;
    }

    private Product getActiveProduct(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + productId
                ));

        if (product.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Product not found with ID: " + productId
            );
        }

        return product;
    }

    private StockTransaction saveTransaction(
            Product product,
            StockTransaction.TransactionType transactionType,
            Integer quantityChange,
            Supplier supplier,
            String notes
    ) {
        StockTransaction transaction = new StockTransaction();
        transaction.setProduct(product);
        transaction.setTransactionType(transactionType);
        transaction.setQuantityChange(quantityChange);
        transaction.setSupplier(supplier);
        transaction.setNotes(notes);

        return stockTransactionRepository.save(transaction);
    }

    private void evictProductCacheAfterCommit(Long productId) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                    "Transaction synchronization is not active"
            );
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        String cacheKey = PRODUCT_CACHE_KEY_PREFIX + productId;

                        redisTemplate.delete(cacheKey);

                        System.out.println(
                                "REDIS CACHE EVICTED after stock update for product ID: "
                                        + productId
                        );
                    }
                }
        );
    }
}
