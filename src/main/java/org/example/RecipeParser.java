package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecipeParser {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/culinary_bot";
    private static final String USER = "postgres";
    private static final String PASS = "1";

    public static List<Recipe> searchRecipesList(String keyword) throws SQLException {
        List<Recipe> recipes = new ArrayList<>();
        String sql = "SELECT * FROM recipes WHERE " +
                     "LOWER(recipe_name) LIKE LOWER(?) OR " +
                     "LOWER(ingredients) LIKE LOWER(?) OR " +
                     "LOWER(key_words) LIKE LOWER(?) " +
                     "LIMIT 6";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String query = "%" + keyword + "%";
            stmt.setString(1, query);
            stmt.setString(2, query);
            stmt.setString(3, query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String recipe = rs.getString("recipe");
                if (recipe == null) recipe = "";
                recipes.add(new Recipe(
                    rs.getInt("recipe_id"),
                    rs.getString("recipe_name"),
                    rs.getString("ingredients"),
                    recipe,
                    rs.getString("key_words")
                ));
            }
        }
        return recipes;
    }

    public static Recipe getRecipeById(int id) throws SQLException {
        Recipe recipe = null;
        String sql = "SELECT * FROM recipes WHERE recipe_id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                recipe = new Recipe(
                    rs.getInt("recipe_id"),
                    rs.getString("recipe_name"),
                    rs.getString("ingredients"),
                    rs.getString("recipe"),
                    rs.getString("key_words")
                );
            }
        }
        return recipe;
    }

    public static int getOrCreateUserByTelegramId(long telegramId) throws SQLException {
        int userId = -1;
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            // Проверяем — есть ли уже пользователь
            String select = "SELECT user_id FROM users WHERE telegram_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(select)) {
                stmt.setLong(1, telegramId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                    userId = rs.getInt("user_id");
            }
            // Если нет — добавляем
            if (userId == -1) {
                String insert = "INSERT INTO users (telegram_id) VALUES (?) RETURNING user_id";
                try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                    stmt.setLong(1, telegramId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next())
                        userId = rs.getInt("user_id");
                }
            }
        }
        return userId;
    }

    public static void saveRecipeForUser(long telegramId, int recipeId) throws SQLException {
        int userId = getOrCreateUserByTelegramId(telegramId);
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "INSERT INTO saved_recipes (user_id, recipe_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, recipeId);
                stmt.executeUpdate();
            }
        }
    }

    public static List<Recipe> getSavedRecipes(long telegramId) throws SQLException {
        int userId = getOrCreateUserByTelegramId(telegramId);
        List<Recipe> recipes = new ArrayList<>();
        String sql = "SELECT r.* FROM saved_recipes s " +
                "JOIN recipes r ON s.recipe_id = r.recipe_id " +
                "WHERE s.user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                recipes.add(new Recipe(
                        rs.getInt("recipe_id"),
                        rs.getString("recipe_name"),
                        rs.getString("ingredients"),
                        rs.getString("recipe"),
                        rs.getString("key_words")
                ));
            }
        }
        return recipes;
    }
}
