package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecipeParser {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASS = "1";

    public static String searchRecipes(String keyword) throws SQLException {
        List<Recipe> recipes = new ArrayList<>();
        String sql = "SELECT * FROM recipes WHERE " +
                    "LOWER(recipe_name) LIKE LOWER(?) OR " +
                    "LOWER(ingredients) LIKE LOWER(?) OR " +
                    "LOWER(key_words) LIKE LOWER(?) " +
                    "LIMIT 6"; // Ограничиваем выборку
    
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String query = "%" + keyword + "%";
            stmt.setString(1, query);
            stmt.setString(2, query);
            stmt.setString(3, query);
    
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String recipe = rs.getString("recipe");
                // Проверяем на null
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
    
        return formatRecipes(recipes);
    }

    private static String formatRecipes(List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return "🔍 Рецептов не найдено. Попробуйте изменить запрос.";
        }
    
        // Ограничиваем количество рецептов
        int maxRecipes = 5;
        if (recipes.size() > maxRecipes) {
            recipes = recipes.subList(0, maxRecipes);
        }
    
        StringBuilder result = new StringBuilder();
        for (Recipe recipe : recipes) {
            result.append(recipe.toString()).append("\n----------------\n");
        }
    
        if (recipes.size() == maxRecipes) {
            result.append("\nℹ Показаны первые ").append(maxRecipes).append(" рецептов.");
        }
    
        return result.toString();
    }
}