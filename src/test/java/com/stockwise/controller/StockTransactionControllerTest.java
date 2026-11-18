
package com.stockwise.controller;

import com.stockwise.configs.SecurityConfig;
import com.stockwise.models.Product;
import com.stockwise.models.StockTransaction;
import com.stockwise.models.Supplier;
import com.stockwise.service.StockTransactionService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockTransactionController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "stockwise.security.username=testuser",
        "stockwise.security.password=testpassword"
})
class StockTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockTransactionService stockTransactionService;

    @Test
    @WithMockUser
    void shouldRecordStockInSuccessfully() throws Exception {

        StockTransaction transaction = createTransaction(
                1L,
                StockTransaction.TransactionType.STOCK_IN,
                20,
                true
        );

        when(stockTransactionService.recordStockIn(
                1L, 20, 2L, "Initial stock"
        )).thenReturn(transaction);

        mockMvc.perform(post("/api/stock-transactions/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 20,
                                    "supplierId": 2,
                                    "notes": "Initial stock"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").value("Wireless Mouse"))
                .andExpect(jsonPath("$.transactionType").value("STOCK_IN"))
                .andExpect(jsonPath("$.quantityChange").value(20))
                .andExpect(jsonPath("$.supplierId").value(2))
                .andExpect(jsonPath("$.supplierName").value("Tech Supplies Ltd"));

        verify(stockTransactionService).recordStockIn(
                1L, 20, 2L, "Initial stock"
        );
    }

    @Test
    @WithMockUser
    void shouldRecordStockOutSuccessfully() throws Exception {

        StockTransaction transaction = createTransaction(
                2L,
                StockTransaction.TransactionType.STOCK_OUT,
                -5,
                false
        );

        when(stockTransactionService.recordStockOut(
                1L, 5, "Customer order"
        )).thenReturn(transaction);

        mockMvc.perform(post("/api/stock-transactions/stock-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 5,
                                    "notes": "Customer order"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.transactionType").value("STOCK_OUT"))
                .andExpect(jsonPath("$.quantityChange").value(-5))
                .andExpect(jsonPath("$.supplierId").doesNotExist());

        verify(stockTransactionService).recordStockOut(
                1L, 5, "Customer order"
        );
    }

    @Test
    @WithMockUser
    void shouldAdjustStockSuccessfully() throws Exception {

        StockTransaction transaction = createTransaction(
                3L,
                StockTransaction.TransactionType.ADJUSTMENT,
                10,
                false
        );

        when(stockTransactionService.adjustStock(
                1L, 10, "Physical stock count"
        )).thenReturn(transaction);

        mockMvc.perform(post("/api/stock-transactions/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantityChange": 10,
                                    "notes": "Physical stock count"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.transactionType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.quantityChange").value(10));

        verify(stockTransactionService).adjustStock(
                1L, 10, "Physical stock count"
        );
    }

    @Test
    @WithMockUser
    void shouldSupportNegativeStockAdjustment() throws Exception {

        StockTransaction transaction = createTransaction(
                4L,
                StockTransaction.TransactionType.ADJUSTMENT,
                -3,
                false
        );

        when(stockTransactionService.adjustStock(
                1L, -3, "Damaged stock"
        )).thenReturn(transaction);

        mockMvc.perform(post("/api/stock-transactions/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantityChange": -3,
                                    "notes": "Damaged stock"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.quantityChange").value(-3));

        verify(stockTransactionService).adjustStock(
                1L, -3, "Damaged stock"
        );
    }

    @Test
    @WithMockUser
    void shouldGetTransactionHistorySuccessfully() throws Exception {

        StockTransaction first = createTransaction(
                1L,
                StockTransaction.TransactionType.STOCK_IN,
                20,
                true
        );

        StockTransaction second = createTransaction(
                2L,
                StockTransaction.TransactionType.STOCK_OUT,
                -5,
                false
        );

        Page<StockTransaction> transactionPage = new PageImpl<>(
                List.of(first, second),
                PageRequest.of(0, 10),
                2
        );

        when(stockTransactionService.getTransactionsForProduct(1L, 0, 10))
                .thenReturn(transactionPage);

        mockMvc.perform(get("/api/stock-transactions/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].transactionType").value("STOCK_IN"))
                .andExpect(jsonPath("$.content[1].transactionType").value("STOCK_OUT"));

        verify(stockTransactionService)
                .getTransactionsForProduct(1L, 0, 10);
    }

    @Test
    @WithMockUser
    void shouldForwardTransactionPaginationParameters() throws Exception {

        Page<StockTransaction> transactionPage = new PageImpl<>(
                List.of(createTransaction(
                        1L,
                        StockTransaction.TransactionType.STOCK_IN,
                        20,
                        true
                )),
                PageRequest.of(1, 5),
                6
        );

        when(stockTransactionService.getTransactionsForProduct(1L, 1, 5))
                .thenReturn(transactionPage);

        mockMvc.perform(get("/api/stock-transactions/products/1")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(stockTransactionService)
                .getTransactionsForProduct(1L, 1, 5);
    }

    @Test
    @WithMockUser
    void shouldRejectStockInWithZeroQuantity() throws Exception {

        mockMvc.perform(post("/api/stock-transactions/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(stockTransactionService, never())
                .recordStockIn(
                        any(), any(), any(), any()
                );
    }

    @Test
    @WithMockUser
    void shouldRejectStockOutWithNegativeQuantity() throws Exception {

        mockMvc.perform(post("/api/stock-transactions/stock-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": -5
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(stockTransactionService, never())
                .recordStockOut(any(), any(), any());
    }

    @Test
    @WithMockUser
    void shouldRejectStockTransactionWithoutProductId() throws Exception {

        mockMvc.perform(post("/api/stock-transactions/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 10
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(stockTransactionService, never())
                .recordStockIn(any(), any(), any(), any());
    }

    @Test
    @WithMockUser
    void shouldRejectAdjustmentWithoutQuantityChange() throws Exception {

        mockMvc.perform(post("/api/stock-transactions/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(stockTransactionService, never())
                .adjustStock(any(), any(), any());
    }

    @Test
    @WithMockUser
    void shouldRejectNotesExceedingMaximumLength() throws Exception {

        String longNotes = "A".repeat(501);

        String requestBody =
                "{\"productId\":1,\"quantity\":10,\"notes\":\""
                        + longNotes + "\"}";

        mockMvc.perform(post("/api/stock-transactions/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(stockTransactionService, never())
                .recordStockIn(any(), any(), any(), any());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(get("/api/stock-transactions/products/1"))
                .andExpect(status().isUnauthorized());

        verify(stockTransactionService, never())
                .getTransactionsForProduct(any(), anyInt(), anyInt());
    }

    private StockTransaction createTransaction(
            Long id,
            StockTransaction.TransactionType type,
            Integer quantityChange,
            boolean includeSupplier
    ) {

        Product product = new Product();
        product.setId(1L);
        product.setName("Wireless Mouse");

        StockTransaction transaction = new StockTransaction();
        transaction.setId(id);
        transaction.setProduct(product);
        transaction.setTransactionType(type);
        transaction.setQuantityChange(quantityChange);
        transaction.setNotes("Test stock transaction");
        transaction.setCreatedAt(LocalDateTime.now());

        if (includeSupplier) {
            Supplier supplier = new Supplier();
            supplier.setId(2L);
            supplier.setName("Tech Supplies Ltd");
            transaction.setSupplier(supplier);
        }

        return transaction;
    }
}
