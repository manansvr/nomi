package com.nomi.recipe;

import java.util.List;

public class Recipe {
    private final String id;
    private final String name;
    private final String description;
    private final String emoji;
    private final String color;
    private final int minutes;
    private final List<RecipeIngredient> ingredients;
    private final List<String> instructions;

    public Recipe(String id, String name, String description, String emoji, String color,
                  int minutes, List<RecipeIngredient> ingredients, List<String> instructions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.emoji = emoji;
        this.color = color;
        this.minutes = minutes;
        this.ingredients = List.copyOf(ingredients);
        this.instructions = List.copyOf(instructions);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getColor() {
        return color;
    }

    public int getMinutes() {
        return minutes;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public List<String> getInstructions() {
        return instructions;
    }
}
