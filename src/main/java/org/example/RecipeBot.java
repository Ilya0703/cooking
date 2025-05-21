package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;

public class RecipeBot extends TelegramLongPollingBot {
    private final String botUsername;
    private final String botToken;

    public RecipeBot(String botToken, String botUsername) {
        super(botToken);
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        try {
            String response;
            if (messageText.equalsIgnoreCase("/start")) {
                response = "👋 Привет! Я бот с рецептами. Напиши название блюда или ингредиент:\n" +
                           "• паста\n• курица с овощами\n• шоколадный торт";
            } else {
                response = RecipeParser.searchRecipes(messageText);
            }
            sendMessage(chatId, response);
        } catch (SQLException e) {
            sendMessage(chatId, "⚠️ Ошибка базы данных. Попробуйте позже.");
            System.out.println(e);
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }
}