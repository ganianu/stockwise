
package com.stockwise.controller;

import com.stockwise.configs.SecurityConfig;
import com.stockwise.models.Category;
import com.stockwise.service.CategoryService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "stockwise.security.username=testuser",
        "stockwise.security.password=testpassword"
})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @WithMockUser
    void shouldCreateCategorySuccessfully() throws Exception {

        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        when(categoryService.createCategory("Electronics"))
                .thenReturn(category);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Electronics"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"));

        verify(categoryService).createCategory("Electronics");
    }

    @Test
    @WithMockUser
    void shouldRejectBlankCategoryName() throws Exception {

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": " "
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(anyString());
    }

    @Test
    @WithMockUser
    void shouldRejectCategoryNameExceedingMaximumLength()
            throws Exception {

        String longName = "A".repeat(101);

        String requestBody =
                "{\"name\":\"" + longName + "\"}";

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(anyString());
    }

    @Test
    @WithMockUser
    void shouldGetCategoryByIdSuccessfully() throws Exception {

        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        when(categoryService.getCategoryById(1L))
                .thenReturn(category);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"));

        verify(categoryService).getCategoryById(1L);
    }

    @Test
    @WithMockUser
    void shouldGetAllCategoriesSuccessfully() throws Exception {

        Category electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");

        Category furniture = new Category();
        furniture.setId(2L);
        furniture.setName("Furniture");

        when(categoryService.getAllCategories())
                .thenReturn(List.of(electronics, furniture));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[1].name").value("Furniture"));
    }

    @Test
    @WithMockUser
    void shouldUpdateCategorySuccessfully() throws Exception {

        Category category = new Category();
        category.setId(1L);
        category.setName("Updated Electronics");

        when(categoryService.updateCategory(1L, "Updated Electronics"))
                .thenReturn(category);

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Updated Electronics"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Electronics"));

        verify(categoryService).updateCategory(
                1L, "Updated Electronics"
        );
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());

        verify(categoryService, never()).getAllCategories();
    }
}
