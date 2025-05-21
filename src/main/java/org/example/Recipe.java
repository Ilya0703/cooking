package org.example;

public class Recipe {
    
    private int recipe_id;
    private String recipe_name;
    private String ingredients;
    private String recipe;
    private String key_words;

    // Конструктор
    public Recipe(int id, String name, String ingredients, String instructions, String keywords) {
        this.recipe_id = id;
        this.recipe_name = name;
        this.ingredients = ingredients;
        this.recipe = instructions;
        this.key_words = keywords;
    }
    @Override
    public String toString() {
        // Безопасное обрезание текста
        String shortInstructions = recipe.length() > 100 
            ? recipe.substring(0, Math.min(recipe.length(), 100)) + "..."
            : recipe;
        
        return String.format(
            "🍲 *%s*\n🧂 Ингредиенты:\n%s\n📝 Рецепт:\n%s\n",
            recipe_name,
            ingredients,
            shortInstructions
        );
    }

    // Геттеры
    public String getName() { return recipe_name; }
    public String getIngredients() { return ingredients; }
    public String getInstructions() { return recipe; }
}