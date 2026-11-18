
package com.stockwise.service;

import com.stockwise.exceptions.ResourceNotFoundException;
import com.stockwise.models.Category;
import com.stockwise.repository.CategoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private SelfCategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("Electronics");
    }

    @Test
    void shouldCreateCategorySuccessfully() {

        when(categoryRepository.existsByName("Electronics"))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.createCategory(" Electronics ");

        assertEquals("Electronics", result.getName());

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void shouldRejectDuplicateCategory() {

        when(categoryRepository.existsByName("Electronics"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.createCategory("Electronics")
        );

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldGetCategoryByIdSuccessfully() {

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        Category result = categoryService.getCategoryById(1L);

        assertEquals("Electronics", result.getName());
    }

    @Test
    void shouldThrowExceptionWhenCategoryDoesNotExist() {

        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(99L)
        );
    }

    @Test
    void shouldRejectDeletedCategory() {

        category.setDeleted(true);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(1L)
        );
    }

    @Test
    void shouldReturnOnlyActiveCategories() {

        Category activeCategory = new Category();
        activeCategory.setName("Electronics");

        Category deletedCategory = new Category();
        deletedCategory.setName("Furniture");
        deletedCategory.setDeleted(true);

        when(categoryRepository.findAll())
                .thenReturn(List.of(activeCategory, deletedCategory));

        List<Category> result = categoryService.getAllCategories();

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getName());
    }

    @Test
    void shouldUpdateCategorySuccessfully() {

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Category result =
                categoryService.updateCategory(1L, " Updated Electronics ");

        assertEquals("Updated Electronics", result.getName());

        verify(categoryRepository).save(category);
    }

    @Test
    void shouldRejectDuplicateNameDuringUpdate() {

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(categoryRepository.existsByName("Furniture"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.updateCategory(1L, "Furniture")
        );

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldRejectBlankCategoryNameDuringUpdate() {

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.updateCategory(1L, " ")
        );

        verify(categoryRepository, never()).save(any(Category.class));
    }
}
