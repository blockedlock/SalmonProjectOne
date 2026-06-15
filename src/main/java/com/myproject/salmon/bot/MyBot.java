package com.myproject.salmon.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class MyBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final StartHandler startHandler;
    private final TextHandler textHandler;

    public MyBot(BotConfig config,
                 StartHandler startHandler,
                 TextHandler textHandler) {
        this.config = config;
        this.startHandler = startHandler;
        this.textHandler = textHandler;
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        if (text.equals("/start")) {
            startHandler.handle(chatId, this);
        } else {
            textHandler.handle(chatId, text, this);

        }
    }
}