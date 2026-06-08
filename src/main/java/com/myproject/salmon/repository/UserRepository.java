package com.myproject.salmon.repository;

import com.myproject.salmon.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
