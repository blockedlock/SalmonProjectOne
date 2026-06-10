package com.myproject.salmon.service;

import com.myproject.salmon.model.User;
import com.myproject.salmon.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getOrCreateUser(Long chatId,String username) {
        return userRepository.findByChatId(chatId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .chatId(chatId)
                            .username(username)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }

}