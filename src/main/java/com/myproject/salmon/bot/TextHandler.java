package com.myproject.salmon.bot;

import com.myproject.salmon.model.Product;
import com.myproject.salmon.model.User;
import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.PriceParser;
import com.myproject.salmon.service.ProductService;
import com.myproject.salmon.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TextHandler {

    private final ProductService productService;
    private final UserService userService;
    private final List<PriceParser> priceParsers;
    private final Map<Long, BotState> userStates = new ConcurrentHashMap<>();

    public TextHandler(ProductService productService,
                       UserService userService,
                       List<PriceParser> priceParsers) {
        this.productService = productService;
        this.userService = userService;
        this.priceParsers = priceParsers;
    }

    public void handle(long chatId, String username, String text, MyBot bot) {
        if (userStates.get(chatId) == BotState.WAITING_FOR_PRODUCT_URL && isUrl(text)) {
            createProductFromUrl(chatId, username, text, bot);
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        switch (text) {
            case "➕ Добавить":
                userStates.put(chatId, BotState.WAITING_FOR_PRODUCT_URL);
                message.setText("Вы выбрали '➕ Добавить'. Пожалуйста, отправьте ссылку на товар, который хотите отслеживать.");
                break;
            case "❌ Удалить":
                deleteProductUrl(chatId, username, text, bot);
                return;
            case "📋 Список":
                message.setText("Вы выбрали 'Список'. Вот список ваших отслеживаемых товаров:");
                break;
            case "⚙️ Настройки":
                message.setText("Вы выбрали 'Настройки'. Здесь вы можете изменить свои настройки и предпочтения.");
                break;
            case "Какие сайты можно отслеживать?":
                message.setText("Вы можете отслеживать товары с следующих сайтов:\n- Books.com \n- DNS.ru \n\n Список доступных для отслеживания сайтов будет постепенно расширяться, следите за обновлениями!");
                break;
            default:
                return;
        }

        sendMessage(bot, message);
    }

    private void createProductFromUrl(long chatId, String username, String url, MyBot bot) {
        SendMessage messageWait = new SendMessage();
        messageWait.setChatId(chatId);
        messageWait.setText("Подождите...");
        Message waitMessage;
        try {
            waitMessage = bot.execute(messageWait); // возвращает Message
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        userStates.remove(chatId);

        User user = userService.getOrCreateUser(chatId, username);

        Product.ProductBuilder productBuilder = Product.builder()
                .user(user)
                .url(url)
                .createdAt(LocalDateTime.now());

        priceParsers.stream()
                .filter(parser -> parser.supports(url))
                .findFirst()
                .ifPresent(parser -> {
                    try {
                        ParsedProduct parsed = parser.parse(url);
                        productBuilder
                                .name(parsed.name())
                                .currentPrice(parsed.price())
                                .lastCheckedAt(LocalDateTime.now());
                    } catch (Exception e) {
                        log.warn("Failed to parse product from url: {}", url, e);
                    }
                });

        Product product = productBuilder.build();
        productService.createProduct(product);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(product.getName() != null
                ? "Товар добавлен в отслеживание: " + product.getName()
                : "Товар добавлен в отслеживание. Ссылка сохранена.");

        sendMessage(bot, message);

        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(waitMessage.getMessageId());
        try {
            bot.execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteProductUrl(long chatId, String username, String text, MyBot bot) {
        User user = userService.getOrCreateUser(chatId, username);
        List<Product> products = productService.getProductsByUser(user);



        if (products.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("У вас нет отслеживаемых товаров для удаления.");
            sendMessage(bot, message);
            return;
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Product product : products) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getName() != null ? product.getName() : product.getUrl());
            button.setCallbackData("delete_product_" + product.getId()); // "delete_product_1"

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            rows.add(row);
        }

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("❌ Отмена");
        cancelButton.setCallbackData("cancel_delete");

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        cancelRow.add(cancelButton);
        rows.add(cancelRow);

        keyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите товар для удаления:");
        message.setReplyMarkup(keyboard);

        userStates.put(chatId, BotState.WAITING_FOR_PRODUCT_TO_DELETE);

        sendMessage(bot, message);
    }

    private boolean isUrl(String text) {
        return text.startsWith("http://") || text.startsWith("https://");
    }

    private void sendMessage(MyBot bot, SendMessage message) {
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error in text processing: {}", e.getMessage());
        }
    }

    private final Map<Long, Long> pendingDeleteProduct = new ConcurrentHashMap<>();

    public void handleCallback(long chatId, String callbackData, MyBot bot) {
        if (callbackData.startsWith("delete_product_")) {
            Long productId = Long.parseLong(callbackData.replace("delete_product_", ""));
            pendingDeleteProduct.put(chatId, productId); // сохраняем ID продукта

            userStates.put(chatId, BotState.WAITING_FOR_DELETE_CONFIRMATION);

            Product product = productService.getProductById(productId);
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Удалить товар: " + (product.getName() != null ? product.getName() : product.getUrl()) + "?");

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton yesButton = new InlineKeyboardButton();
            yesButton.setText("✅ Да, удалить");
            yesButton.setCallbackData("confirm_delete_" + productId);

            InlineKeyboardButton noButton = new InlineKeyboardButton();
            noButton.setText("❌ Нет, отменить");
            noButton.setCallbackData("cancel_delete");

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(yesButton);
            row.add(noButton);
            rows.add(row);

            keyboard.setKeyboard(rows);
            message.setReplyMarkup(keyboard);

            sendMessage(bot, message);
        }

        else if (callbackData.startsWith("confirm_delete_")) {
            Long productId = Long.parseLong(callbackData.replace("confirm_delete_", ""));
            productService.deleteProduct(productId);

            userStates.remove(chatId);
            pendingDeleteProduct.remove(chatId);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Товар удалён из отслеживания.");
            sendMessage(bot, message);
        }

        else if (callbackData.equals("cancel_delete")) {
            userStates.remove(chatId);
            pendingDeleteProduct.remove(chatId);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Удаление отменено.");
            sendMessage(bot, message);
        }
    }
}
