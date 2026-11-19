package com.nomi.recipe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nomi.ingredient.IngredientService;
import com.nomi.persistence.PersistentJsonStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RecipeCatalogService {
    private final IngredientService ingredientService;
    private final Set<String> favorites = ConcurrentHashMap.newKeySet();
    private final List<Recipe> recipes = new CopyOnWriteArrayList<>();
    private PersistentJsonStore storage;

    public RecipeCatalogService(IngredientService ingredientService) { this.ingredientService = ingredientService; }

    @Autowired
    public synchronized void configureStorage(PersistentJsonStore storage) {
        this.storage = storage;
        if (!reload()) save();
    }

    public synchronized List<RecipeMatch> findMatches() {
        reload();
        List<RecipeMatch> matches = new ArrayList<>();
        for (Recipe recipe : recipes) {
            List<RecipeIngredient> missing = recipe.getIngredients().stream().filter(item -> !ingredientService.hasIngredient(item.getName())).toList();
            matches.add(new RecipeMatch(recipe, missing, favorites.contains(recipe.getId())));
        }
        return matches.stream()
                .sorted(Comparator.comparing(RecipeMatch::isReadyToCook).reversed()
                        .thenComparing(RecipeMatch::getMissingCount)
                        .thenComparing(match -> match.getRecipe().getName())).toList();
    }

    public List<RecipeMatch> findFavorites() { return findMatches().stream().filter(RecipeMatch::isFavorite).toList(); }

    public synchronized Recipe addRecipe(String name, String description, int minutes, List<RecipeIngredient> ingredients, List<String> instructions) {
        reload();
        Recipe recipe = new Recipe(UUID.randomUUID().toString(), name.trim(), description.trim(), "🍽️", "sage", minutes, ingredients, instructions);
        recipes.add(recipe); save(); return recipe;
    }

    public synchronized boolean removeRecipe(String recipeId) {
        reload();
        boolean removed = recipes.removeIf(recipe -> recipe.getId().equals(recipeId));
        if (removed) { favorites.remove(recipeId); save(); }
        return removed;
    }

    public synchronized boolean toggleFavorite(String recipeId) {
        reload();
        if (recipes.stream().noneMatch(recipe -> recipe.getId().equals(recipeId))) return false;
        if (!favorites.add(recipeId)) favorites.remove(recipeId);
        save(); return true;
    }

    public RecipeMatch findMatch(String recipeId) {
        return findMatches().stream().filter(match -> match.getRecipe().getId().equals(recipeId)).findFirst().orElse(null);
    }

    public synchronized int favoriteCount() { reload(); return favorites.size(); }

    private void save() {
        if (storage == null) return;
        List<StoredRecipe> savedRecipes = recipes.stream().map(RecipeCatalogService::store).toList();
        storage.write("recipes.json", new StoredCatalog(savedRecipes, new HashSet<>(favorites)));
    }

    private boolean reload() {
        if (storage == null) return false;
        java.util.Optional<StoredCatalog> saved = storage.read("recipes.json", new TypeReference<>() { });
        recipes.clear();
        favorites.clear();
        if (saved.isEmpty()) return false;
        saved.get().recipes().stream().map(RecipeCatalogService::restore).forEach(recipes::add);
        favorites.addAll(saved.get().favorites());
        return true;
    }

    private static StoredRecipe store(Recipe recipe) {
        List<StoredRecipeIngredient> ingredients = recipe.getIngredients().stream()
                .map(item -> new StoredRecipeIngredient(item.getName(), item.getQuantity(), item.getUnit(), item.getCategory())).toList();
        return new StoredRecipe(recipe.getId(), recipe.getName(), recipe.getDescription(), recipe.getEmoji(), recipe.getColor(), recipe.getMinutes(), ingredients, recipe.getInstructions());
    }

    private static Recipe restore(StoredRecipe saved) {
        List<RecipeIngredient> ingredients = saved.ingredients().stream()
                .map(item -> new RecipeIngredient(item.name(), item.quantity(), item.unit(), item.category())).toList();
        return new Recipe(saved.id(), saved.name(), saved.description(), saved.emoji(), saved.color(), saved.minutes(), ingredients, saved.instructions());
    }

    private record StoredCatalog(List<StoredRecipe> recipes, Set<String> favorites) { }
    private record StoredRecipe(String id, String name, String description, String emoji, String color, int minutes, List<StoredRecipeIngredient> ingredients, List<String> instructions) { }
    private record StoredRecipeIngredient(String name, int quantity, String unit, String category) { }
}
