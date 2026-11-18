
package com.stockwise.controller;

import com.stockwise.dto.StockAdjustmentRequestDTO;
import com.stockwise.dto.StockTransactionRequestDTO;
import com.stockwise.dto.StockTransactionResponseDTO;
import com.stockwise.models.StockTransaction;
import com.stockwise.service.StockTransactionService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock-transactions")
public class StockTransactionController {

    private final StockTransactionService stockTransactionService;

    public StockTransactionController(
            StockTransactionService stockTransactionService
    ) {
        this.stockTransactionService = stockTransactionService;
    }

    @PostMapping("/stock-in")
    public ResponseEntity<StockTransactionResponseDTO> recordStockIn(
            @Valid @RequestBody StockTransactionRequestDTO request
    ) {
        StockTransaction transaction =
                stockTransactionService.recordStockIn(
                        request.getProductId(),
                        request.getQuantity(),
                        request.getSupplierId(),
                        request.getNotes()
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponseDTO(transaction));
    }

    @PostMapping("/stock-out")
    public ResponseEntity<StockTransactionResponseDTO> recordStockOut(
            @Valid @RequestBody StockTransactionRequestDTO request
    ) {
        StockTransaction transaction =
                stockTransactionService.recordStockOut(
                        request.getProductId(),
                        request.getQuantity(),
                        request.getNotes()
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponseDTO(transaction));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<StockTransactionResponseDTO> adjustStock(
            @Valid @RequestBody StockAdjustmentRequestDTO request
    ) {
        StockTransaction transaction =
                stockTransactionService.adjustStock(
                        request.getProductId(),
                        request.getQuantityChange(),
                        request.getNotes()
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponseDTO(transaction));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<Page<StockTransactionResponseDTO>>
    getTransactionsForProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        Page<StockTransactionResponseDTO> transactions =
                stockTransactionService.getTransactionsForProduct(
                        productId,
                        pageNumber,
                        pageSize
                ).map(this::toResponseDTO);

        return ResponseEntity.ok(transactions);
    }

    private StockTransactionResponseDTO toResponseDTO(
            StockTransaction transaction
    ) {
        Long productId = transaction.getProduct().getId();
        String productName = transaction.getProduct().getName();

        Long supplierId = null;
        String supplierName = null;

        if (transaction.getSupplier() != null) {
            supplierId = transaction.getSupplier().getId();
            supplierName = transaction.getSupplier().getName();
        }

        return new StockTransactionResponseDTO(
                transaction.getId(),
                productId,
                productName,
                transaction.getTransactionType(),
                transaction.getQuantityChange(),
                supplierId,
                supplierName,
                transaction.getNotes(),
                transaction.getCreatedAt()
        );
    }
}
