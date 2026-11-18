
package com.stockwise.controller;

import com.stockwise.advice.GlobalExceptionHandler;
import com.stockwise.configs.SecurityConfig;
import com.stockwise.models.Supplier;
import com.stockwise.service.SupplierService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupplierController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "stockwise.security.username=stockwise_admin",
        "stockwise.security.password=test-password"
})
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierService supplierService;

    private Supplier createSupplier() {
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Tech Supplies Ltd");
        supplier.setContactPerson("David Miller");
        supplier.setEmail("david@techsupplies.com");
        supplier.setPhone("1234567890");
        supplier.setAddress("Dublin, Ireland");
        supplier.setCreatedAt(LocalDateTime.now());
        supplier.setUpdatedAt(LocalDateTime.now());
        return supplier;
    }

    // 1. Create supplier successfully
    @Test
    @WithMockUser
    void shouldCreateSupplierSuccessfully() throws Exception {

        when(supplierService.createSupplier(any(Supplier.class)))
                .thenReturn(createSupplier());

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Tech Supplies Ltd",
                                    "contactPerson": "David Miller",
                                    "email": "david@techsupplies.com",
                                    "phone": "1234567890",
                                    "address": "Dublin, Ireland"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tech Supplies Ltd"))
                .andExpect(jsonPath("$.contactPerson").value("David Miller"))
                .andExpect(jsonPath("$.email").value("david@techsupplies.com"));
    }

    // 2. Reject blank supplier name
    @Test
    @WithMockUser
    void shouldRejectBlankSupplierName() throws Exception {

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "",
                                    "contactPerson": "David Miller",
                                    "email": "david@techsupplies.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Supplier name is required")));
    }

    // 3. Reject blank contact person
    @Test
    @WithMockUser
    void shouldRejectBlankContactPerson() throws Exception {

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Tech Supplies Ltd",
                                    "contactPerson": "",
                                    "email": "david@techsupplies.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Contact person is required")));
    }

    // 4. Reject invalid email
    @Test
    @WithMockUser
    void shouldRejectInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Tech Supplies Ltd",
                                    "contactPerson": "David Miller",
                                    "email": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Please provide a valid email address")));
    }

    // 5. Reject phone number exceeding maximum length
    @Test
    @WithMockUser
    void shouldRejectPhoneNumberExceedingMaximumLength() throws Exception {

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Tech Supplies Ltd",
                                    "contactPerson": "David Miller",
                                    "phone": "123456789012345678901"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Phone number cannot exceed 20 characters")));
    }

    // 6. Reject address exceeding maximum length
    @Test
    @WithMockUser
    void shouldRejectAddressExceedingMaximumLength() throws Exception {

        String longAddress = "A".repeat(256);

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Tech Supplies Ltd",
                                    "contactPerson": "David Miller",
                                    "address": "%s"
                                }
                                """.formatted(longAddress)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Address cannot exceed 255 characters")));
    }

    // 7. Get supplier by ID
    @Test
    @WithMockUser
    void shouldGetSupplierById() throws Exception {

        when(supplierService.getSupplierById(1L))
                .thenReturn(createSupplier());

        mockMvc.perform(get("/api/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tech Supplies Ltd"))
                .andExpect(jsonPath("$.contactPerson").value("David Miller"));
    }

    // 8. Get all suppliers
    @Test
    @WithMockUser
    void shouldGetAllSuppliers() throws Exception {

        when(supplierService.getAllSuppliers())
                .thenReturn(List.of(createSupplier()));

        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Tech Supplies Ltd"));
    }

    // 9. Update supplier
    @Test
    @WithMockUser
    void shouldUpdateSupplierSuccessfully() throws Exception {

        Supplier updatedSupplier = createSupplier();
        updatedSupplier.setName("Updated Tech Supplies Ltd");

        when(supplierService.updateSupplier(
                org.mockito.ArgumentMatchers.eq(1L),
                any(Supplier.class)))
                .thenReturn(updatedSupplier);

        mockMvc.perform(put("/api/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Updated Tech Supplies Ltd",
                                    "contactPerson": "David Miller",
                                    "email": "david@techsupplies.com",
                                    "phone": "1234567890",
                                    "address": "Dublin, Ireland"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Tech Supplies Ltd"));
    }

    // 10. Delete supplier
    @Test
    @WithMockUser
    void shouldDeleteSupplierSuccessfully() throws Exception {

        mockMvc.perform(delete("/api/suppliers/1"))
                .andExpect(status().isNoContent());

        verify(supplierService).deleteSupplier(1L);
    }

    // 11. Reject duplicate supplier details
    @Test
    @WithMockUser
    void shouldRejectDuplicateSupplierDetails() throws Exception {

        when(supplierService.createSupplier(any(Supplier.class)))
                .thenThrow(new IllegalArgumentException("Supplier already exists"));

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Tech Supplies Ltd",
                                    "contactPerson": "David Miller",
                                    "email": "david@techsupplies.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Supplier already exists"));
    }

    // 12. Reject unauthenticated request
    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isUnauthorized());
    }
}
