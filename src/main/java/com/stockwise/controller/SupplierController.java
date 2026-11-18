
package com.stockwise.controller;

import com.stockwise.dto.SupplierRequestDTO;
import com.stockwise.dto.SupplierResponseDTO;
import com.stockwise.models.Supplier;
import com.stockwise.service.SupplierService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<SupplierResponseDTO> createSupplier(
            @Valid @RequestBody SupplierRequestDTO request
    ) {
        Supplier supplier = toSupplier(request);

        Supplier savedSupplier = supplierService.createSupplier(supplier);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponseDTO(savedSupplier));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> getSupplierById(
            @PathVariable Long id
    ) {
        Supplier supplier = supplierService.getSupplierById(id);

        return ResponseEntity.ok(toResponseDTO(supplier));
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponseDTO>> getAllSuppliers() {

        List<SupplierResponseDTO> suppliers =
                supplierService.getAllSuppliers()
                        .stream()
                        .map(this::toResponseDTO)
                        .toList();

        return ResponseEntity.ok(suppliers);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDTO request
    ) {
        Supplier supplier = toSupplier(request);

        Supplier updatedSupplier =
                supplierService.updateSupplier(id, supplier);

        return ResponseEntity.ok(toResponseDTO(updatedSupplier));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable Long id
    ) {
        supplierService.deleteSupplier(id);

        return ResponseEntity.noContent().build();
    }

    private Supplier toSupplier(SupplierRequestDTO request) {

        Supplier supplier = new Supplier();

        supplier.setName(request.getName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());

        return supplier;
    }

    private SupplierResponseDTO toResponseDTO(Supplier supplier) {

        return new SupplierResponseDTO(
                supplier.getId(),
                supplier.getName(),
                supplier.getContactPerson(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}
