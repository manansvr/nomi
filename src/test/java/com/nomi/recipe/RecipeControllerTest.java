package com.nomi.recipe;

import com.nomi.ingredient.IngredientService;
import com.nomi.shopping.ShoppingListService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecipeControllerTest {
    private final IngredientService pantry = new IngredientService();
    private final RecipeCatalogService recipeCatalog = new RecipeCatalogService(pantry);
    private final ShoppingListService shoppingList = new ShoppingListService(pantry);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new RecipeController(pantry, recipeCatalog, shoppingList)).build();

    @Test
    void recipeIngredientsRequireAndStoreACategory() throws Exception {
        mockMvc.perform(post("/recipes")
                        .param("name", "Garden salad")
                        .param("description", "A fresh lunch.")
                        .param("minutes", "10")
                        .param("ingredientNames", "Cucumber")
                        .param("ingredientQuantities", "2")
                        .param("ingredientUnits", "pcs")
                        .param("instructions", "Slice the cucumber."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recipes/new"));
        assertTrue(recipeCatalog.findMatches().isEmpty());

        mockMvc.perform(post("/recipes")
                        .param("name", "Garden salad")
                        .param("description", "A fresh lunch.")
                        .param("minutes", "10")
                        .param("ingredientNames", "Cucumber")
                        .param("ingredientQuantities", "2")
                        .param("ingredientUnits", "pcs")
                        .param("ingredientCategories", "Vegetable")
                        .param("instructions", "Slice the cucumber."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recipes"));

        assertEquals("Vegetable", recipeCatalog.findMatches().get(0).getRecipe().getIngredients().get(0).getCategory());
    }

    @Test
    void favoritingRecipeAddsItsMissingIngredientsToShoppingList() throws Exception {
        Recipe recipe = recipeCatalog.addRecipe("Garden salad", "A fresh lunch.", 10,
                List.of(new RecipeIngredient("Cucumber", 2, "pcs")),
                List.of("Slice the cucumber."));

        mockMvc.perform(post("/recipes/{id}/favorite", recipe.getId()).param("categories", "Vegetable"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recipes"));

        assertTrue(recipeCatalog.findMatch(recipe.getId()).isFavorite());
        assertEquals(1, shoppingList.totalCount());
        assertEquals("Cucumber", shoppingList.findAll().get(0).getName());
        assertEquals("Vegetable", shoppingList.findAll().get(0).getCategory());
    }

    @Test
    void missingCategoryPreventsFavoritingAndShoppingListChanges() throws Exception {
        Recipe recipe = recipeCatalog.addRecipe("Garden salad", "A fresh lunch.", 10,
                List.of(new RecipeIngredient("Cucumber", 2, "pcs")),
                List.of("Slice the cucumber."));

        mockMvc.perform(post("/recipes/{id}/favorite", recipe.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recipes"));

        assertFalse(recipeCatalog.findMatch(recipe.getId()).isFavorite());
        assertEquals(0, shoppingList.totalCount());
    }

    @Test
    void refavoritingDoesNotDuplicateIngredientsAlreadyAdded() throws Exception {
        Recipe recipe = recipeCatalog.addRecipe("Garden salad", "A fresh lunch.", 10,
                List.of(new RecipeIngredient("Cucumber", 2, "pcs")),
                List.of("Slice the cucumber."));

        mockMvc.perform(post("/recipes/{id}/favorite", recipe.getId()).param("categories", "Vegetable"));
        mockMvc.perform(post("/recipes/{id}/favorite", recipe.getId()));
        mockMvc.perform(post("/recipes/{id}/favorite", recipe.getId()).param("categories", "Vegetable"));

        assertTrue(recipeCatalog.findMatch(recipe.getId()).isFavorite());
        assertEquals(1, shoppingList.totalCount());
        assertEquals(2, shoppingList.findAll().get(0).getQuantity());
    }
}
