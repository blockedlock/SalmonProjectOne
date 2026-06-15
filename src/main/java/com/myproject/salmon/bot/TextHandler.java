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
        message.setText("u said " + text);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error in text processing: {}", e.getMessage());
        }
    }
}