import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.IOException;

public class RecipeBot extends TelegramLongPollingBot {
    private final String botUsername;

    public RecipeBot(String botToken, String botUsername) {
        super(botToken);
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        try {
            String response;
            if (messageText.equalsIgnoreCase("/start")) {
                response = "Привет! Я бот с рецептами. Напишите название блюда, например:\n"
                        + "• борщ\n• пицца\n• плов\n• оливье\n• блины";
            } else {
                response = RecipeParser.getRecipe(messageText);
            }
            sendFormattedMessage(chatId, response);
        } catch (IOException e) {
            sendFormattedMessage(chatId, "🔍 Рецепт не найден. Попробуйте:\n"
                    + "• Проверить орфографию\n"
                    + "• Использовать более общее название\n"
                    + "• Попробовать другой рецепт");
        } catch (Exception e) {
            sendFormattedMessage(chatId, "⚠ Ошибка при поиске рецепта. Попробуйте позже.");
        }
    }

    private void sendFormattedMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }
}