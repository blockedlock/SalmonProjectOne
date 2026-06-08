package com.myproject.salmon.repository;

import com.myproject.salmon.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
