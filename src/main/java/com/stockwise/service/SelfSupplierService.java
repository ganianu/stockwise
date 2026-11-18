
package com.stockwise.service;

import com.stockwise.exceptions.ResourceNotFoundException;
import com.stockwise.models.Supplier;
import com.stockwise.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SelfSupplierService implements SupplierService {

    private final SupplierRepository supplierRepository;

    public SelfSupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public Supplier createSupplier(Supplier supplier) {

        if (supplier == null) {
            throw new IllegalArgumentException(
                    "Supplier details are required"
            );
        }

        if (supplier.getName() == null ||
                supplier.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Supplier name is required"
            );
        }

        if (supplierRepository.existsByNameAndDeletedFalse(
                supplier.getName().trim())) {
            throw new IllegalArgumentException(
                    "Supplier already exists: " + supplier.getName()
            );
        }

        if (supplier.getEmail() != null &&
                supplierRepository.findByEmailAndDeletedFalse(
                        supplier.getEmail()).isPresent()) {
            throw new IllegalArgumentException(
                    "Supplier email already exists: " + supplier.getEmail()
            );
        }

        supplier.setName(supplier.getName().trim());

        return supplierRepository.save(supplier);
    }

    @Override
    public Supplier getSupplierById(Long id) {

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier not found with ID: " + id
                ));

        if (supplier.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Supplier not found with ID: " + id
            );
        }

        return supplier;
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAllByDeletedFalse();
    }

    @Override
    public Supplier updateSupplier(Long id, Supplier updatedSupplier) {

        if (updatedSupplier == null) {
            throw new IllegalArgumentException(
                    "Supplier details are required"
            );
        }

        Supplier existingSupplier = getSupplierById(id);

        if (updatedSupplier.getName() != null) {

            String newName = updatedSupplier.getName().trim();

            if (newName.isBlank()) {
                throw new IllegalArgumentException(
                        "Supplier name cannot be blank"
                );
            }

            if (!newName.equals(existingSupplier.getName()) &&
                    supplierRepository.existsByNameAndDeletedFalse(newName)) {
                throw new IllegalArgumentException(
                        "Supplier already exists: " + newName
                );
            }

            existingSupplier.setName(newName);
        }

        if (updatedSupplier.getContactPerson() != null) {
            existingSupplier.setContactPerson(
                    updatedSupplier.getContactPerson()
            );
        }

        if (updatedSupplier.getEmail() != null &&
                !updatedSupplier.getEmail().equals(existingSupplier.getEmail())) {

            if (supplierRepository.findByEmailAndDeletedFalse(
                    updatedSupplier.getEmail()).isPresent()) {
                throw new IllegalArgumentException(
                        "Supplier email already exists: "
                                + updatedSupplier.getEmail()
                );
            }

            existingSupplier.setEmail(updatedSupplier.getEmail());
        }

        if (updatedSupplier.getPhone() != null) {
            existingSupplier.setPhone(updatedSupplier.getPhone());
        }

        if (updatedSupplier.getAddress() != null) {
            existingSupplier.setAddress(updatedSupplier.getAddress());
        }

        return supplierRepository.save(existingSupplier);
    }

    @Override
    public void deleteSupplier(Long id) {

        Supplier supplier = getSupplierById(id);

        supplier.setDeleted(true);

        supplierRepository.save(supplier);
    }
}
