package com.myproject.salmon.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class StartHandler {

    public void handle(long chatId, MyBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("привет");

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error with sending message /start: {}", e.getMessage());
        }
    }
}