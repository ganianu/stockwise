
package com.stockwise.service;

import com.stockwise.exceptions.ResourceNotFoundException;
import com.stockwise.models.Product;
import com.stockwise.models.StockTransaction;
import com.stockwise.models.Supplier;
import com.stockwise.repository.ProductRepository;
import com.stockwise.repository.StockTransactionRepository;
import com.stockwise.repository.SupplierRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfStockTransactionServiceTest {

    @Mock
    private StockTransactionRepository stockTransactionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private SelfStockTransactionService stockTransactionService;

    private Product product;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Wireless Mouse");
        product.setSku("SW-MOUSE-001");
        product.setQuantity(20);
        product.setReorderLevel(5);

        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Tech Supplies Ltd");
    }

    @AfterEach
    void cleanUpTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    private void completeAfterCommit() {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();

        synchronizations.forEach(TransactionSynchronization::afterCommit);

        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void shouldRecordStockInSuccessfully() {

        beginTransactionSynchronization();

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(stockTransactionRepository.save(any(StockTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockTransaction result =
                stockTransactionService.recordStockIn(
                        1L, 10, 1L, "New stock received"
                );

        assertEquals(30, product.getQuantity());
        assertEquals(StockTransaction.TransactionType.STOCK_IN,
                result.getTransactionType());
        assertEquals(10, result.getQuantityChange());
        assertEquals(supplier, result.getSupplier());

        verify(productRepository).save(product);
        verify(stockTransactionRepository).save(any(StockTransaction.class));
        verify(redisTemplate, never()).delete(anyString());

        completeAfterCommit();

        verify(redisTemplate).delete("PRODUCT_V3:1");
    }

    @Test
    void shouldRecordStockInWithoutSupplier() {

        beginTransactionSynchronization();

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(stockTransactionRepository.save(any(StockTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockTransaction result =
                stockTransactionService.recordStockIn(
                        1L, 5, null, "Stock received"
                );

        assertEquals(25, product.getQuantity());
        assertNull(result.getSupplier());

        verify(supplierRepository, never()).findById(anyLong());

        completeAfterCommit();

        verify(redisTemplate).delete("PRODUCT_V3:1");
    }

    @Test
    void shouldRejectInvalidStockInQuantity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> stockTransactionService.recordStockIn(
                        1L, 0, null, "Invalid stock"
                )
        );

        verify(productRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void shouldRejectStockInWhenProductDoesNotExist() {

        when(productRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> stockTransactionService.recordStockIn(
                        99L, 5, null, "Stock received"
                )
        );
    }

    @Test
    void shouldRejectStockInWithDeletedSupplier() {

        supplier.setDeleted(true);

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        assertThrows(
                ResourceNotFoundException.class,
                () -> stockTransactionService.recordStockIn(
                        1L, 5, 1L, "Stock received"
                )
        );

        assertEquals(20, product.getQuantity());

        verify(stockTransactionRepository, never())
                .save(any(StockTransaction.class));
    }

    @Test
    void shouldRecordStockOutSuccessfully() {

        beginTransactionSynchronization();

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(stockTransactionRepository.save(any(StockTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockTransaction result =
                stockTransactionService.recordStockOut(
                        1L, 5, "Items sold"
                );

        assertEquals(15, product.getQuantity());
        assertEquals(StockTransaction.TransactionType.STOCK_OUT,
                result.getTransactionType());
        assertEquals(-5, result.getQuantityChange());
        assertNull(result.getSupplier());

        verify(productRepository).save(product);

        completeAfterCommit();

        verify(redisTemplate).delete("PRODUCT_V3:1");
    }

    @Test
    void shouldRejectInvalidStockOutQuantity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> stockTransactionService.recordStockOut(
                        1L, 0, "Invalid stock out"
                )
        );

        verify(productRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void shouldRejectStockOutWhenStockIsInsufficient() {

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                IllegalArgumentException.class,
                () -> stockTransactionService.recordStockOut(
                        1L, 25, "Insufficient stock test"
                )
        );

        assertEquals(20, product.getQuantity());

        verify(productRepository, never()).save(any(Product.class));
        verify(stockTransactionRepository, never())
                .save(any(StockTransaction.class));
    }

    @Test
    void shouldAdjustStockSuccessfully() {

        beginTransactionSynchronization();

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(stockTransactionRepository.save(any(StockTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockTransaction result =
                stockTransactionService.adjustStock(
                        1L, 10, "Inventory correction"
                );

        assertEquals(30, product.getQuantity());
        assertEquals(StockTransaction.TransactionType.ADJUSTMENT,
                result.getTransactionType());
        assertEquals(10, result.getQuantityChange());

        completeAfterCommit();

        verify(redisTemplate).delete("PRODUCT_V3:1");
    }

    @Test
    void shouldRejectZeroStockAdjustment() {

        assertThrows(
                IllegalArgumentException.class,
                () -> stockTransactionService.adjustStock(
                        1L, 0, "Invalid adjustment"
                )
        );

        verify(productRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void shouldRejectAdjustmentResultingInNegativeStock() {

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                IllegalArgumentException.class,
                () -> stockTransactionService.adjustStock(
                        1L, -25, "Negative stock test"
                )
        );

        assertEquals(20, product.getQuantity());

        verify(productRepository, never()).save(any(Product.class));
        verify(stockTransactionRepository, never())
                .save(any(StockTransaction.class));
    }

    @Test
    void shouldRejectDeletedProduct() {

        product.setDeleted(true);

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                ResourceNotFoundException.class,
                () -> stockTransactionService.recordStockIn(
                        1L, 5, null, "Stock received"
                )
        );
    }

    @Test
    void shouldGetTransactionsForProductSuccessfully() {

        Page<StockTransaction> expectedPage =
                new PageImpl<>(List.of(new StockTransaction()));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(stockTransactionRepository
                .findByProductIdOrderByCreatedAtDesc(
                        eq(1L), eq(PageRequest.of(0, 10))
                ))
                .thenReturn(expectedPage);

        Page<StockTransaction> result =
                stockTransactionService.getTransactionsForProduct(
                        1L, 0, 10
                );

        assertEquals(1, result.getTotalElements());

        verify(stockTransactionRepository)
                .findByProductIdOrderByCreatedAtDesc(
                        1L, PageRequest.of(0, 10)
                );
    }

    @Test
    void shouldRejectInvalidTransactionPagination() {

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                IllegalArgumentException.class,
                () -> stockTransactionService.getTransactionsForProduct(
                        1L, -1, 10
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> stockTransactionService.getTransactionsForProduct(
                        1L, 0, 0
                )
        );

        verify(stockTransactionRepository, never())
                .findByProductIdOrderByCreatedAtDesc(
                        anyLong(), any(PageRequest.class)
                );
    }

    @Test
    void shouldRejectTransactionHistoryForMissingProduct() {

        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> stockTransactionService.getTransactionsForProduct(
                        99L, 0, 10
                )
        );
    }
}
