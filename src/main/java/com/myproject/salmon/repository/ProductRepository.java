package com.myproject.salmon.repository;

import com.myproject.salmon.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByLastCheckedAtBeforeOrLastCheckedAtIsNull(LocalDateTime threshold);

}
