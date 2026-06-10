package com.myproject.salmon.service;


import com.myproject.salmon.model.Product;
import com.myproject.salmon.model.User;
import com.myproject.salmon.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;


@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product addProduct(User user, String url) {
        Product newProduct = Product.builder()
                .user(user)
                .url(url)
                .createdAt(LocalDateTime.now())
                .build();

        return productRepository.save(newProduct);
    }


}
