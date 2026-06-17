package com.myproject.salmon.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class TextHandler {

    public void handle(long chatId, String text, MyBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        switch (message.getText()) {
            case "➕ Добавить":
                message.setText("Вы выбрали 'Добавить'. Пожалуйста, отправьте ссылку на товар, который хотите отслеживать.");
                break;
            case "❌ Удалить":
                message.setText("Вы выбрали 'Удалить'. Пожалуйста, отправьте ссылку на товар, который хотите удалить из отслеживания.");
                break;
            case "📋 Список":
                message.setText("Вы выбрали 'Список'. Вот список ваших отслеживаемых товаров:");
                break;
            case "⚙️ Настройки":
                message.setText("Вы выбрали 'Настройки'. Здесь вы можете изменить свои настройки и предпочтения.");
                break;
            case "Какие сайты можно отслеживать?":
                message.setText("Вы можете отслеживать товары с следующих сайтов:\n- Books.com \n- DNS.ru \n\n Список доступных для отслеживания сайтов будет постепенно расширяться, следите за обновлениями!");
                break;
        }


        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error in text processing: {}", e.getMessage());
        }
    }
}