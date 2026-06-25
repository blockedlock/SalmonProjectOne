package com.myproject.salmon.service;

import com.myproject.salmon.model.User;
import com.myproject.salmon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .chatId(12345L)
                .username("testuser")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getOrCreateUser_shouldReturnExistingUser() {
        // Arrange
        Long chatId = 12345L;
        String username = "testuser";
        when(userRepository.findByChatId(chatId)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getOrCreateUser(chatId, username);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getChatId(), result.getChatId());
        verify(userRepository, times(1)).findByChatId(chatId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getOrCreateUser_shouldCreateNewUserIfNotFound() {
        // Arrange
        Long chatId = 12345L;
        String username = "newuser";
        when(userRepository.findByChatId(chatId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.getOrCreateUser(chatId, username);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).findByChatId(chatId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void getOrCreateUser_shouldCreateUserWithCorrectData() {
        // Arrange
        Long chatId = 12345L;
        String username = "newuser";
        when(userRepository.findByChatId(chatId)).thenReturn(Optional.empty());
        
        final User[] capturedUser = new User[1];
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            capturedUser[0] = invocation.getArgument(0);
            return capturedUser[0];
        });

        // Act
        User result = userService.getOrCreateUser(chatId, username);

        // Assert
        assertEquals(chatId, result.getChatId());
        assertEquals(username, result.getUsername());
        assertNotNull(result.getCreatedAt());
    }
}
