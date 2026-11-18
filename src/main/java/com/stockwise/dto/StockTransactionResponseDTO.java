
package com.stockwise.dto;

import com.stockwise.models.StockTransaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionResponseDTO {

    private Long id;
    private Long productId;
    private String productName;
    private StockTransaction.TransactionType transactionType;
    private Integer quantityChange;
    private Long supplierId;
    private String supplierName;
    private String notes;
    private LocalDateTime createdAt;
}
