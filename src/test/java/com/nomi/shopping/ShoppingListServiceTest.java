package com.nomi.shopping;

import com.nomi.recipe.RecipeIngredient;
import com.nomi.recipe.Recipe;
import com.nomi.recipe.RecipeMatch;
import com.nomi.ingredient.Ingredient;
import com.nomi.ingredient.IngredientService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoppingListServiceTest {
    private final IngredientService pantry = new IngredientService();
    private final ShoppingListService service = new ShoppingListService(pantry);

    @Test
    void consolidatesRepeatedIngredientsAndTracksPickedUpItems() {
        service.addMissing(List.of(
                new RecipeIngredient("Red onion", 1, "pc"),
                new RecipeIngredient("Red onions", 2, "pc")
        ), List.of("Vegetable", "Vegetable"));

        assertEquals(1, service.totalCount());
        assertEquals(3, service.findAll().get(0).getQuantity());
        assertEquals(1, service.activeCount());

        String itemId = service.findAll().get(0).getId();
        assertTrue(service.togglePurchased(itemId, "Vegetable"));
        assertEquals(0, service.activeCount());
    }

    @Test
    void pickedUpIngredientsEnterPantryOnlyOnce() {
        service.addMissing(List.of(new RecipeIngredient("Red onion", 2, "pc")), List.of("Vegetable"));
        ShoppingItem item = service.findAll().get(0);

        assertTrue(service.togglePurchased(item.getId(), "Vegetable"));
        Ingredient pantryItem = pantry.find("Red onion", "Vegetable").get(0);
        assertEquals(2, pantryItem.getQuantity());
        assertEquals("Vegetable", item.getCategory());
        assertTrue(item.isAddedToPantry());

        assertTrue(service.togglePurchased(item.getId(), "Vegetable"));
        assertTrue(service.togglePurchased(item.getId(), "Vegetable"));
        assertEquals(2, pantry.find("Red onion", "Vegetable").get(0).getQuantity());
    }

    @Test
    void pickedUpIngredientsIncreaseMatchingPantryStockAndKeepItsCategory() {
        pantry.add("Avocados", "Fruits", 3, "pcs");
        service.addMissing(List.of(new RecipeIngredient("Avocado", 2, "pcs")), List.of("Protein"));
        ShoppingItem item = service.findAll().get(0);

        assertTrue(service.togglePurchased(item.getId(), "Protein"));

        Ingredient existing = pantry.find("Avocados", "Fruits").get(0);
        assertEquals(5, existing.getQuantity());
        assertEquals("Fruits", item.getCategory());
        assertEquals(1, pantry.find("Avocados", "").size());
    }

    @Test
    void addingTheSameRecipeTwiceDoesNotDoubleItsIngredients() {
        Recipe recipe = new Recipe("recipe-1", "Soup", "A bowl of soup.", "🍲", "sage", 20,
                List.of(new RecipeIngredient("Lemon", 1, "pc")), List.of("Cook."));
        RecipeMatch match = new RecipeMatch(recipe, recipe.getIngredients(), false);

        assertEquals(1, service.addMissingFromRecipes(List.of(match), List.of("Fruits")));
        assertEquals(0, service.addMissingFromRecipes(List.of(match), List.of("Fruits")));
        assertEquals(1, service.totalCount());
        assertEquals(1, service.findAll().get(0).getQuantity());
    }
}
