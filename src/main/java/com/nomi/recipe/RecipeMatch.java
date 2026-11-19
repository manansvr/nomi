package com.nomi.recipe;

import java.util.List;

public class RecipeMatch {
    private final Recipe recipe;
    private final List<RecipeIngredient> missingIngredients;
    private final boolean favorite;

    public RecipeMatch(Recipe recipe, List<RecipeIngredient> missingIngredients, boolean favorite) {
        this.recipe = recipe;
        this.missingIngredients = List.copyOf(missingIngredients);
        this.favorite = favorite;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public List<RecipeIngredient> getMissingIngredients() {
        return missingIngredients;
    }

    public int getAvailableCount() {
        return recipe.getIngredients().size() - missingIngredients.size();
    }

    public int getTotalCount() {
        return recipe.getIngredients().size();
    }

    public int getMissingCount() {
        return missingIngredients.size();
    }

    public boolean isReadyToCook() {
        return missingIngredients.isEmpty();
    }

    public boolean isFavorite() {
        return favorite;
    }

    public int getMatchPercent() {
        return Math.round(100f * getAvailableCount() / getTotalCount());
    }

    public String getMissingSummary() {
        return missingIngredients.stream()
                .map(RecipeIngredient::getName)
                .reduce((first, second) -> first + ", " + second)
                .orElse("Nothing missing");
    }
}
