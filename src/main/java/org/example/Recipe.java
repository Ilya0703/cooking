package org.example;

public class Recipe {
    public int recipe_id;
    private String recipe_name;
    private String ingredients;
    private String recipe;
    private String key_words;

    public Recipe(int id, String name, String ingredients, String instructions, String keywords) {
        this.recipe_id = id;
        this.recipe_name = name;
        this.ingredients = ingredients;
        this.recipe = instructions;
        this.key_words = keywords;
    }

    @Override
    public String toString() {
        return String.format(
            "🍲 *%s*\n🧂 Ингредиенты:\n%s\n📝 Рецепт:\n%s\n",
            recipe_name,
            ingredients,
            recipe == null ? "" : recipe
        );
    }

    public String getName() { return recipe_name; }
}
