import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecipeParser {
    private static final String BASE_URL = "https://www.russianfood.com";
    private static final int TIMEOUT = 15000;

    public static String getRecipe(String dishName) throws IOException {
        // 1. Ищем рецепты через поиск
        String searchUrl = BASE_URL + "/recipes/search.php?srch=" + dishName.replace(" ", "+");

        Document searchPage = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .referrer(BASE_URL)
                .timeout(TIMEOUT)
                .get();

        // 2. Новые точные селекторы (актуальные на 2024 год)
        Elements recipeItems = searchPage.select("div.recipe_list_item");
        if (recipeItems.isEmpty()) {
            throw new IOException("Рецепты не найдены");
        }

        // 3. Берем первый найденный рецепт
        Element firstRecipe = recipeItems.first();
        String recipeUrl = BASE_URL + firstRecipe.selectFirst("a.recipe_title").attr("href");
        String recipeTitle = firstRecipe.selectFirst("a.recipe_title").text();

        // 4. Получаем детали рецепта
        Document recipePage = Jsoup.connect(recipeUrl)
                .timeout(TIMEOUT)
                .get();

        // 5. Форматируем результат
        return formatFullRecipe(recipeTitle, recipePage);
    }

    private static String formatFullRecipe(String title, Document page) {
        StringBuilder result = new StringBuilder();
        result.append("🍲 *").append(title).append("*\n\n");

        // Ингредиенты
        result.append("🛒 *Ингредиенты:*\n");
        Elements ingredients = page.select("table.ingr_table tr:not(.header)");
        for (Element ingr : ingredients) {
            result.append("- ")
                    .append(ingr.select(".name").text())
                    .append(" — ")
                    .append(ingr.select(".value").text())
                    .append("\n");
        }

        // Шаги приготовления
        result.append("\n📝 *Приготовление:*\n");
        Elements steps = page.select("div.step_n, div.step_txt");
        for (int i = 0; i < steps.size(); i += 2) {
            if (i+1 < steps.size()) {
                result.append(i/2 + 1).append(". ")
                        .append(steps.get(i+1).text())
                        .append("\n\n");
            }
        }

        // Время и сложность
        Element info = page.selectFirst("div.recipe_info");
        if (info != null) {
            result.append("\n⏱ *Время:* ").append(info.select("span.time").text()).append("\n");
            result.append("⚙ *Сложность:* ").append(info.select("span.level").text());
        }

        return result.toString();
    }
}