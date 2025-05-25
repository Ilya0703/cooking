package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.*;

public class RecipeBot extends TelegramLongPollingBot {
    private final String botUsername;
    private final String botToken;
    private final Map<Long, String> lastKeywordsMap = new HashMap<>();

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
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                if (messageText.equalsIgnoreCase("/start")) {
                    sendWelcome(chatId);
                } else if (messageText.equalsIgnoreCase("/my")) {
                    List<Recipe> saved = RecipeParser.getSavedRecipes(chatId);
                    if (saved.isEmpty()) {
                        sendMessage(chatId, "У вас пока нет сохранённых рецептов!");
                    } else {
                        sendSavedRecipeButtons(chatId, saved);
                    }
                }
                else {
                    lastKeywordsMap.put(chatId, messageText.trim());
                    List<Recipe> recipes = RecipeParser.searchRecipesList(messageText.trim());
                    if (recipes.isEmpty()) {
                        sendMessage(chatId, "🔍 Рецептов не найдено. Попробуйте изменить запрос.");
                    } else {
                        sendRecipeButtons(chatId, recipes, messageText.trim());
                    }
                }
            }
            else if (update.hasCallbackQuery()) {
                String data = update.getCallbackQuery().getData();
                long chatId = update.getCallbackQuery().getMessage().getChatId();

                if (data.equals("back_to_start")) {
                    sendWelcome(chatId);
                }
                else if (data.startsWith("back_to_list:")) {
                    String keyword = data.substring("back_to_list:".length());
                    lastKeywordsMap.put(chatId, keyword);
                    List<Recipe> recipes = RecipeParser.searchRecipesList(keyword);
                    if (!recipes.isEmpty()) {
                        sendRecipeButtons(chatId, recipes, keyword);
                    } else {
                        sendMessage(chatId, "🔍 Рецептов не найдено. Попробуйте изменить запрос.");
                    }
                }
                else if (data.startsWith("recipe_")) {
                    String[] parts = data.substring(7).split("#", 2);
                    int id = Integer.parseInt(parts[0]);
                    String keyword = (parts.length > 1) ? parts[1] : lastKeywordsMap.getOrDefault(chatId, "");
                    lastKeywordsMap.put(chatId, keyword);
                    Recipe recipe = RecipeParser.getRecipeById(id);
                    if (recipe != null) {
                        sendRecipeDetail(chatId, recipe, keyword);
                    } else {
                        sendMessage(chatId, "Рецепт не найден.");
                    }
                }
                else if (data.startsWith("save_")) {
                    int recId = Integer.parseInt(data.substring(5));
                    RecipeParser.saveRecipeForUser(chatId, recId);
                    sendMessage(chatId, "⭐ Рецепт сохранён в ваши избранные!");
                }
                // открытие из favorite по кнопке
                else if (data.startsWith("fav_")) {
                    int id = Integer.parseInt(data.substring(4));
                    Recipe recipe = RecipeParser.getRecipeById(id);
                    if (recipe != null) {
                        sendRecipeDetailFromFavorites(chatId, recipe);
                    } else {
                        sendMessage(chatId, "Рецепт не найден.");
                    }
                }
            }
        } catch (SQLException e) {
            long chatId = 0;
            if (update.hasMessage() && update.getMessage() != null)
                chatId = update.getMessage().getChatId();
            if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null)
                chatId = update.getCallbackQuery().getMessage().getChatId();

            sendMessage(chatId, "⚠️ Ошибка базы данных. Попробуйте позже.");
            System.out.println(e);
        }
    }

    private void sendWelcome(long chatId) {
        String text = "👋 Привет! Я бот с рецептами.\n" +
                "Напиши *название блюда* или *ингредиент*:\n" +
                "• суп\n• морковь\n• шоколадный торт\n\n" +
                "Или напиши /my — список избранных рецептов.";
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

    private void sendRecipeButtons(long chatId, List<Recipe> recipes, String lastKeyword) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🔎 Найдены рецепты. Выберите нужный:");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (Recipe recipe : recipes) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(recipe.getName());
            btn.setCallbackData("recipe_" + recipe.recipe_id + "#" + lastKeyword);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(btn);
            keyboard.add(row);
        }
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Назад");
        backBtn.setCallbackData("back_to_start");
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(backBtn);
        keyboard.add(backRow);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }

    private void sendSavedRecipeButtons(long chatId, List<Recipe> recipes) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⭐ Ваши сохранённые рецепты:");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (Recipe recipe : recipes) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(recipe.getName());
            btn.setCallbackData("fav_" + recipe.recipe_id);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(btn);
            keyboard.add(row);
        }
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Назад");
        backBtn.setCallbackData("back_to_start");
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(backBtn);
        keyboard.add(backRow);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }

    // Одиночный рецепт (из поиска) — с кнопками "Сохранить" и "Назад"
    private void sendRecipeDetail(long chatId, Recipe recipe, String lastKeyword) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(recipe.toString());
        message.setParseMode("Markdown");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Сохранить
        InlineKeyboardButton saveBtn = new InlineKeyboardButton();
        saveBtn.setText("⭐ Сохранить");
        saveBtn.setCallbackData("save_" + recipe.recipe_id);
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(saveBtn);

        // Назад к списку
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Назад к списку");
        backBtn.setCallbackData("back_to_list:" + lastKeyword);
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(backBtn);

        keyboard.add(row1);
        keyboard.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }

    private void sendRecipeDetailFromFavorites(long chatId, Recipe recipe) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(recipe.toString());
        message.setParseMode("Markdown");
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("⬅️ Назад к избранным");
        backBtn.setCallbackData("/my");
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(backBtn);
        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }
}
