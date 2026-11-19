package com.nomi.ingredient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientServiceTest {
    private final IngredientService service = new IngredientService();

    @Test
    void startsWithAnEmptyPantry() {
        assertEquals(0, service.count());
        assertTrue(service.find("", "").isEmpty());
    }

    @Test
    void filtersIngredientsByCaseInsensitiveSearchAndCategory() {
        service.add("Baby spinach", "Vegetable", 1, "bag");
        service.add("Cherry tomatoes", "Vegetable", 2, "cups");

        List<Ingredient> results = service.find("SPINACH", "Vegetable");

        assertEquals(1, results.size());
        assertEquals("Baby spinach", results.get(0).getName());
    }

    @Test
    void quantityChangesCannotGoBelowZero() {
        service.add("Baby spinach", "Vegetable", 1, "bag");
        String spinachId = service.find("spinach", "").get(0).getId();

        assertTrue(service.adjust(spinachId, -1));
        assertEquals(0, service.find("", "Vegetable").stream()
                .filter(ingredient -> ingredient.getId().equals(spinachId))
                .findFirst().orElseThrow().getQuantity());
        assertFalse(service.adjust("missing", 1));
    }

    @Test
    void addingAndRemovingIngredientsUpdatesPantryCount() {
        int initialCount = service.count();

        service.add("Lemons", "Vegetable", 4, "pcs");

        assertEquals(initialCount + 1, service.count());
        Ingredient added = service.find("lemons", "").get(0);
        assertTrue(service.remove(added.getId()));
        assertEquals(initialCount, service.count());
    }

    @Test
    void recognizesRecipeNamesAndTreatsZeroQuantityAsUnavailable() {
        service.add("Avocados", "Fruits", 3, "pcs");
        service.add("Cherry tomatoes", "Vegetable", 2, "cups");
        service.add("Baby spinach", "Vegetable", 1, "bag");

        assertTrue(service.hasIngredient("Avocado"));
        assertTrue(service.hasIngredient("Tomato"));
        String spinachId = service.find("spinach", "").get(0).getId();
        assertTrue(service.adjust(spinachId, -1));
        assertFalse(service.hasIngredient("Spinach"));
    }

    @Test
    void exposesTheRequestedIngredientCategories() {
        assertEquals(java.util.List.of("Protein", "Carbohydrate", "Vegetable", "Fruits", "Spices"),
                service.getCategories());
    }
}
