
package com.stockwise.controller;

import com.stockwise.configs.SecurityConfig;
import com.stockwise.models.Category;
import com.stockwise.models.Product;
import com.stockwise.service.ProductService;

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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "stockwise.security.username=testuser",
        "stockwise.security.password=testpassword"
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser
    void shouldCreateProductSuccessfully() throws Exception {

        Product product = createProduct(1L, "Wireless Mouse");

        when(productService.createProduct(any(Product.class)))
                .thenReturn(product);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Wireless Mouse",
                                    "sku": "SW-MOUSE-001",
                                    "description": "Wireless computer mouse",
                                    "unitPrice": 29.99,
                                    "quantity": 50,
                                    "reorderLevel": 10,
                                    "categoryId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.sku").value("SW-MOUSE-001"))
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Electronics"));

        verify(productService).createProduct(any(Product.class));
    }

    @Test
    @WithMockUser
    void shouldRejectProductWithBlankName() throws Exception {

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": " ",
                                    "sku": "SW-MOUSE-001",
                                    "unitPrice": 29.99,
                                    "quantity": 50,
                                    "reorderLevel": 10,
                                    "categoryId": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(Product.class));
    }

    @Test
    @WithMockUser
    void shouldRejectProductWithInvalidPrice() throws Exception {

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Wireless Mouse",
                                    "sku": "SW-MOUSE-001",
                                    "unitPrice": 0,
                                    "quantity": 50,
                                    "reorderLevel": 10,
                                    "categoryId": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(Product.class));
    }

    @Test
    @WithMockUser
    void shouldRejectProductWithNegativeQuantity() throws Exception {

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Wireless Mouse",
                                    "sku": "SW-MOUSE-001",
                                    "unitPrice": 29.99,
                                    "quantity": -5,
                                    "reorderLevel": 10,
                                    "categoryId": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(Product.class));
    }

    @Test
    @WithMockUser
    void shouldGetProductByIdSuccessfully() throws Exception {

        Product product = createProduct(1L, "Wireless Mouse");

        when(productService.getProductById(1L))
                .thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.sku").value("SW-MOUSE-001"))
                .andExpect(jsonPath("$.quantity").value(50));

        verify(productService).getProductById(1L);
    }

    @Test
    @WithMockUser
    void shouldGetAllProductsSuccessfully() throws Exception {

        Product firstProduct = createProduct(1L, "Wireless Mouse");
        Product secondProduct = createProduct(2L, "USB Keyboard");

        Page<Product> productPage = new PageImpl<>(
                List.of(firstProduct, secondProduct),
                PageRequest.of(0, 10),
                2
        );

        when(productService.getAllProducts(0, 10, "name", "asc"))
                .thenReturn(productPage);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.content[1].name").value("USB Keyboard"));

        verify(productService).getAllProducts(0, 10, "name", "asc");
    }

    @Test
    @WithMockUser
    void shouldForwardPaginationAndSortingParameters() throws Exception {

        Page<Product> productPage = new PageImpl<>(
                List.of(createProduct(1L, "Wireless Mouse")),
                PageRequest.of(1, 5),
                6
        );

        when(productService.getAllProducts(1, 5, "unitPrice", "desc"))
                .thenReturn(productPage);

        mockMvc.perform(get("/api/products")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("sortBy", "unitPrice")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(productService).getAllProducts(
                1, 5, "unitPrice", "desc"
        );
    }

    @Test
    @WithMockUser
    void shouldUpdateProductSuccessfully() throws Exception {

        Product product = createProduct(1L, "Updated Wireless Mouse");
        product.setDescription("Updated product description");

        when(productService.updateProduct(eq(1L), any(Product.class)))
                .thenReturn(product);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Updated Wireless Mouse",
                                    "description": "Updated product description",
                                    "unitPrice": 34.99,
                                    "reorderLevel": 15,
                                    "categoryId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Wireless Mouse"))
                .andExpect(jsonPath("$.description")
                        .value("Updated product description"));

        verify(productService).updateProduct(
                eq(1L), any(Product.class)
        );
    }

    @Test
    @WithMockUser
    void shouldRejectDirectQuantityUpdate() throws Exception {

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 100
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(productService, never())
                .updateProduct(eq(1L), any(Product.class));
    }

    @Test
    @WithMockUser
    void shouldDeleteProductSuccessfully() throws Exception {

        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    @Test
    @WithMockUser
    void shouldGetLowStockProductsSuccessfully() throws Exception {

        Product lowStockProduct = createProduct(1L, "USB Keyboard");
        lowStockProduct.setQuantity(5);
        lowStockProduct.setReorderLevel(10);

        Page<Product> productPage = new PageImpl<>(
                List.of(lowStockProduct),
                PageRequest.of(0, 10),
                1
        );

        when(productService.getLowStockProducts(0, 10, "name", "asc"))
                .thenReturn(productPage);

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("USB Keyboard"))
                .andExpect(jsonPath("$.content[0].quantity").value(5))
                .andExpect(jsonPath("$.content[0].reorderLevel").value(10));

        verify(productService).getLowStockProducts(
                0, 10, "name", "asc"
        );
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());

        verify(productService, never())
                .getAllProducts(anyInt(), anyInt(), anyString(), anyString());
    }

    private Product createProduct(Long id, String name) {

        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSku("SW-MOUSE-001");
        product.setDescription("Test product");
        product.setUnitPrice(new BigDecimal("29.99"));
        product.setQuantity(50);
        product.setReorderLevel(10);
        product.setCategory(category);

        return product;
    }
}
