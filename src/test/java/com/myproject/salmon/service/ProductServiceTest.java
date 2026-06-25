package com.myproject.salmon.service;

import com.myproject.salmon.model.Product;
import com.myproject.salmon.model.User;
import com.myproject.salmon.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .chatId(12345L)
                .username("testuser")
                .build();

        testProduct = Product.builder()
                .id(1L)
                .user(testUser)
                .url("https://example.com/product")
                .name("Test Product")
                .currentPrice(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void addProduct_shouldSaveToDatabase() {
        // Arrange
        String url = "https://example.com/new-product";
        Product expectedProduct = Product.builder()
                .user(testUser)
                .url(url)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(expectedProduct);

        // Act
        Product result = productService.addProduct(testUser, url);

        // Assert
        assertNotNull(result);
        assertEquals(url, result.getUrl());
        assertEquals(testUser, result.getUser());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void getAllProducts_shouldReturnAllProducts() {
        // Arrange
        List<Product> expectedProducts = Arrays.asList(testProduct);
        when(productRepository.findAll()).thenReturn(expectedProducts);

        // Act
        List<Product> result = productService.getAllProducts();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testProduct.getId(), result.get(0).getId());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_shouldReturnProduct() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        Product result = productService.getProductById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testProduct.getId(), result.getId());
        assertEquals(testProduct.getName(), result.getName());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getProductById_shouldReturnNullIfNotFound() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Product result = productService.getProductById(999L);

        // Assert
        assertNull(result);
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    void createProduct_shouldSaveProduct() {
        // Arrange
        when(productRepository.save(testProduct)).thenReturn(testProduct);

        // Act
        Product result = productService.createProduct(testProduct);

        // Assert
        assertNotNull(result);
        assertEquals(testProduct.getId(), result.getId());
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    void deleteProduct_shouldCallRepositoryDelete() {
        // Arrange
        Long productId = 1L;
        doNothing().when(productRepository).deleteById(productId);

        // Act
        productService.deleteProduct(productId);

        // Assert
        verify(productRepository, times(1)).deleteById(productId);
    }

    @Test
    void getProductsByUser_shouldReturnUserProducts() {
        // Arrange
        Product product2 = Product.builder()
                .id(2L)
                .user(testUser)
                .url("https://example.com/product2")
                .build();

        List<Product> expectedProducts = Arrays.asList(testProduct, product2);
        when(productRepository.findByUser(testUser)).thenReturn(expectedProducts);

        // Act
        List<Product> result = productService.getProductsByUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getUser().equals(testUser)));
        verify(productRepository, times(1)).findByUser(testUser);
    }
}
