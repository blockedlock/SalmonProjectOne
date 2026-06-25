package com.myproject.salmon.bot;

import com.myproject.salmon.model.Product;
import com.myproject.salmon.model.User;
import com.myproject.salmon.parser.PriceParser;
import com.myproject.salmon.service.ProductService;
import com.myproject.salmon.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TextHandlerTest {

    @Mock
    private ProductService productService;

    @Mock
    private UserService userService;

    @Mock
    private List<PriceParser> priceParsers;

    @Mock
    private MyBot bot;

    @InjectMocks
    private TextHandler textHandler;

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
    void handle_addCommand_shouldSetWaitingForUrlState() throws TelegramApiException {
        // Arrange
        long chatId = 12345L;
        String username = "testuser";
        String text = "➕ Добавить";

        // Act
        textHandler.handle(chatId, username, text, bot);

        // Assert
        verify(bot, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void handle_deleteCommand_shouldShowProducts() throws TelegramApiException {
        // Arrange
        long chatId = 12345L;
        String username = "testuser";
        String text = "❌ Удалить";
        
        when(userService.getOrCreateUser(chatId, username)).thenReturn(testUser);
        when(productService.getProductsByUser(testUser)).thenReturn(Arrays.asList(testProduct));

        // Act
        textHandler.handle(chatId, username, text, bot);

        // Assert
        verify(userService, times(1)).getOrCreateUser(chatId, username);
        verify(productService, times(1)).getProductsByUser(testUser);
        verify(bot, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void handle_deleteCommand_shouldShowNoProductsMessage() throws TelegramApiException {
        // Arrange
        long chatId = 12345L;
        String username = "testuser";
        String text = "❌ Удалить";
        
        when(userService.getOrCreateUser(chatId, username)).thenReturn(testUser);
        when(productService.getProductsByUser(testUser)).thenReturn(Arrays.asList());

        // Act
        textHandler.handle(chatId, username, text, bot);

        // Assert
        verify(userService, times(1)).getOrCreateUser(chatId, username);
        verify(productService, times(1)).getProductsByUser(testUser);
        verify(bot, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void handle_listCommand_shouldSendListMessage() throws TelegramApiException {
        // Arrange
        long chatId = 12345L;
        String username = "testuser";
        String text = "📋 Список";

        // Act
        textHandler.handle(chatId, username, text, bot);

        // Assert
        verify(bot, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void handle_settingsCommand_shouldSendSettingsMessage() throws TelegramApiException {
        // Arrange
        long chatId = 12345L;
        String username = "testuser";
        String text = "⚙️ Настройки";

        // Act
        textHandler.handle(chatId, username, text, bot);

        // Assert
        verify(bot, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void handle_unknownCommand_shouldNotSendMessage() throws TelegramApiException {
        // Arrange
        long chatId = 12345L;
        String username = "testuser";
        String text = "unknown command";

        // Act
        textHandler.handle(chatId, username, text, bot);

        // Assert
        verify(bot, never()).execute(any(SendMessage.class));
    }

    @Test
    void handleCallback_deleteProduct_shouldConfirm() throws TelegramApiException {
        // Arrange
        long chatId = 12345L;
        String callbackData = "delete_product_1";
        
        when(productService.getProductById(1L)).thenReturn(testProduct);

        // Act
        textHandler.handleCallback(chatId, callbackData, bot);

        // Assert
        verify(productService, times(1)).getProductById(1L);
        verify(bot, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void handleCallback_confirmDelete_shouldDeleteProduct() throws TelegramApiException {
        // Arrange
        long chatId = 12345L;
        String callbackData = "confirm_delete_1";

        // Act
        textHandler.handleCallback(chatId, callbackData, bot);

        // Assert
        verify(productService, times(1)).deleteProduct(1L);
        verify(bot, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void handleCallback_cancelDelete_shouldCancel() throws TelegramApiException {
        // Arrange
        long chatId = 12345L;
        String callbackData = "cancel_delete";

        // Act
        textHandler.handleCallback(chatId, callbackData, bot);

        // Assert
        verify(productService, never()).deleteProduct(anyLong());
        verify(bot, times(1)).execute(any(SendMessage.class));
    }
}
