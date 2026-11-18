
package com.stockwise.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductUpdateRequestDTO {

    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    @Pattern(regexp = ".*\\S.*", message = "Product name cannot be blank")
    private String name;

    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    @Pattern(regexp = ".*\\S.*", message = "SKU cannot be blank")
    private String sku;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Unit price must be greater than zero"
    )
    private BigDecimal unitPrice;

    @PositiveOrZero(message = "Reorder level cannot be negative")
    private Integer reorderLevel;

    private Long categoryId;

    @Null(message = "Product quantity cannot be updated directly. Use stock-in, stock-out or stock-adjustment transactions.")
    private Integer quantity;
}
