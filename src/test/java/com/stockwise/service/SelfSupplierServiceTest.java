
package com.stockwise.service;

import com.stockwise.exceptions.ResourceNotFoundException;
import com.stockwise.models.Supplier;
import com.stockwise.repository.SupplierRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfSupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SelfSupplierService supplierService;

    private Supplier supplier;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Tech Supplies Ltd");
        supplier.setContactPerson("David Miller");
        supplier.setEmail("david@techsupplies.com");
        supplier.setPhone("1234567890");
        supplier.setAddress("Dublin, Ireland");
    }

    @Test
    void shouldCreateSupplierSuccessfully() {

        when(supplierRepository.existsByNameAndDeletedFalse(
                "Tech Supplies Ltd")).thenReturn(false);

        when(supplierRepository.findByEmailAndDeletedFalse(
                "david@techsupplies.com")).thenReturn(Optional.empty());

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.createSupplier(supplier);

        assertEquals("Tech Supplies Ltd", result.getName());
        assertEquals("david@techsupplies.com", result.getEmail());

        verify(supplierRepository).save(supplier);
    }

    @Test
    void shouldRejectNullSupplier() {

        assertThrows(
                IllegalArgumentException.class,
                () -> supplierService.createSupplier(null)
        );

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldRejectBlankSupplierName() {

        supplier.setName(" ");

        assertThrows(
                IllegalArgumentException.class,
                () -> supplierService.createSupplier(supplier)
        );
    }

    @Test
    void shouldRejectDuplicateSupplierName() {

        when(supplierRepository.existsByNameAndDeletedFalse(
                "Tech Supplies Ltd")).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> supplierService.createSupplier(supplier)
        );

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldRejectDuplicateSupplierEmail() {

        when(supplierRepository.existsByNameAndDeletedFalse(
                "Tech Supplies Ltd")).thenReturn(false);

        when(supplierRepository.findByEmailAndDeletedFalse(
                "david@techsupplies.com")).thenReturn(Optional.of(supplier));

        assertThrows(
                IllegalArgumentException.class,
                () -> supplierService.createSupplier(supplier)
        );

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldGetSupplierByIdSuccessfully() {

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        Supplier result = supplierService.getSupplierById(1L);

        assertEquals("Tech Supplies Ltd", result.getName());
    }

    @Test
    void shouldThrowExceptionWhenSupplierDoesNotExist() {

        when(supplierRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.getSupplierById(99L)
        );
    }

    @Test
    void shouldRejectDeletedSupplier() {

        supplier.setDeleted(true);

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.getSupplierById(1L)
        );
    }

    @Test
    void shouldReturnOnlyActiveSuppliers() {

        Supplier activeSupplier = new Supplier();
        activeSupplier.setName("Active Supplier");

        when(supplierRepository.findAllByDeletedFalse())
                .thenReturn(List.of(activeSupplier));

        List<Supplier> result = supplierService.getAllSuppliers();

        assertEquals(1, result.size());
        assertEquals("Active Supplier", result.get(0).getName());

        verify(supplierRepository).findAllByDeletedFalse();
    }

    @Test
    void shouldUpdateSupplierSuccessfully() {

        Supplier updatedSupplier = new Supplier();
        updatedSupplier.setName("Updated Supplies Ltd");
        updatedSupplier.setContactPerson("John Smith");
        updatedSupplier.setPhone("9876543210");
        updatedSupplier.setAddress("Cork, Ireland");

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.existsByNameAndDeletedFalse(
                "Updated Supplies Ltd")).thenReturn(false);

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result =
                supplierService.updateSupplier(1L, updatedSupplier);

        assertEquals("Updated Supplies Ltd", result.getName());
        assertEquals("John Smith", result.getContactPerson());
        assertEquals("9876543210", result.getPhone());
        assertEquals("Cork, Ireland", result.getAddress());

        verify(supplierRepository).save(supplier);
    }

    @Test
    void shouldRejectDuplicateNameDuringUpdate() {

        Supplier updatedSupplier = new Supplier();
        updatedSupplier.setName("Another Supplier");

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.existsByNameAndDeletedFalse(
                "Another Supplier")).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> supplierService.updateSupplier(1L, updatedSupplier)
        );

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldRejectDuplicateEmailDuringUpdate() {

        Supplier updatedSupplier = new Supplier();
        updatedSupplier.setEmail("another@techsupplies.com");

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.findByEmailAndDeletedFalse(
                "another@techsupplies.com"))
                .thenReturn(Optional.of(new Supplier()));

        assertThrows(
                IllegalArgumentException.class,
                () -> supplierService.updateSupplier(1L, updatedSupplier)
        );

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldRejectBlankNameDuringUpdate() {

        Supplier updatedSupplier = new Supplier();
        updatedSupplier.setName(" ");

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        assertThrows(
                IllegalArgumentException.class,
                () -> supplierService.updateSupplier(1L, updatedSupplier)
        );

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldRejectNullSupplierDuringUpdate() {

        assertThrows(
                IllegalArgumentException.class,
                () -> supplierService.updateSupplier(1L, null)
        );

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldSoftDeleteSupplierSuccessfully() {

        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        supplierService.deleteSupplier(1L);

        assertTrue(supplier.isDeleted());

        verify(supplierRepository).save(supplier);
    }
}
