
package com.stockwise.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseModel {

    @NotBlank(message = "Supplier name is required")
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Contact person is required")
    @Column(length = 100)
    private String contactPerson;

    @Email(message = "Please provide a valid email address")
    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;
}
