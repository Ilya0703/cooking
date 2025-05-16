import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        String botUsername = "culinary_213_bot";
        String botToken = "7887732990:AAH_859g1je4kSY0CfSAA8JRG4mD_IlB2bM";

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new RecipeBot(botToken, botUsername));
            System.out.println("Бот запущен!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}