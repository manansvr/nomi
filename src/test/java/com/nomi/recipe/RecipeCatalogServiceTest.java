package com.nomi.recipe;

import com.nomi.ingredient.IngredientService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeCatalogServiceTest {
    private final IngredientService pantry = new IngredientService();
    private final RecipeCatalogService service = new RecipeCatalogService(pantry);

    @Test
    void userRecipesAreMatchedAgainstThePantry() {
        pantry.add("Avocados", "Fruits", 3, "pcs");
        Recipe recipe = service.addRecipe("Garden toast", "A quick lunch.", 10,
                java.util.List.of(
                        new RecipeIngredient("Avocado", 1, "pc"),
                        new RecipeIngredient("Lemon", 1, "pc")
                ),
                java.util.List.of("Toast the bread.", "Add the toppings."));

        RecipeMatch match = service.findMatch(recipe.getId());

        assertEquals(1, match.getAvailableCount());
        assertEquals("Lemon", match.getMissingSummary());
        assertFalse(match.isReadyToCook());
        assertEquals(2, match.getRecipe().getInstructions().size());
    }

    @Test
    void favoritesAndRecipeRemovalWork() {
        Recipe recipe = service.addRecipe("Garden toast", "A quick lunch.", 10,
                java.util.List.of(new RecipeIngredient("Avocado", 1, "pc")),
                java.util.List.of("Toast the bread."));
        assertEquals(0, service.favoriteCount());

        assertTrue(service.toggleFavorite(recipe.getId()));
        assertEquals(1, service.findFavorites().size());
        assertTrue(service.findFavorites().get(0).isFavorite());

        assertTrue(service.toggleFavorite(recipe.getId()));
        assertTrue(service.findFavorites().isEmpty());
        assertFalse(service.toggleFavorite("missing-recipe"));
        assertTrue(service.removeRecipe(recipe.getId()));
        assertTrue(service.findMatches().isEmpty());
    }
}
