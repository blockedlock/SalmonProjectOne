package com.myproject.salmon.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
@Component
public class StartHandler {

    public void handle(long chatId, MyBot bot) {
        SendMessage message = new SendMessage();
        message.setReplyMarkup(createMainKeyboard());
        message.setChatId(chatId);
        message.setText("Привет! \n\nЯ постоянно мониторю цены в магазинах и фиксирую любые изменения. Как только цена на интересующий товар снижается, вы сразу получаете уведомление. Это позволяет не следить за ценниками вручную и покупать тогда, когда это действительно выгодно.\n\n" +
                "Настройки, управление товарами и другие функции — в меню ниже \uD83D\uDC47");

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error with sending message /start: {}", e.getMessage());
        }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        KeyboardRow row = new KeyboardRow();
        row.add("➕ Добавить");
        row.add("❌ Удалить");
        KeyboardRow row2 = new KeyboardRow();
        row2.add("📋 Список");
        row2.add("⚙️ Настройки");
        KeyboardRow row3 = new KeyboardRow();
        row3.add("Какие сайты можно отслеживать?");
        keyboard.setKeyboard(List.of(row, row2, row3));
        return keyboard;
    }
}