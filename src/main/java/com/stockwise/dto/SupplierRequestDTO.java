
package com.stockwise.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupplierRequestDTO {

    @NotBlank(message = "Supplier name is required")
    @Size(max = 150, message = "Supplier name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Contact person is required")
    @Size(max = 100, message = "Contact person cannot exceed 100 characters")
    private String contactPerson;

    @Email(message = "Please provide a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;
}
