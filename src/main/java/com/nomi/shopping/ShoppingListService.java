package com.nomi.shopping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nomi.ingredient.IngredientNames;
import com.nomi.ingredient.IngredientService;
import com.nomi.persistence.PersistentJsonStore;
import com.nomi.recipe.RecipeIngredient;
import com.nomi.recipe.RecipeMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ShoppingListService {
    private final List<ShoppingItem> items = new ArrayList<>();
    private final Set<String> addedRecipeIds = new HashSet<>();
    private final IngredientService ingredientService;
    private PersistentJsonStore storage;

    @Autowired
    public ShoppingListService(IngredientService ingredientService) { this.ingredientService = ingredientService; }
    public ShoppingListService() { this(new IngredientService()); }

    @Autowired
    public synchronized void configureStorage(PersistentJsonStore storage) {
        this.storage = storage;
        if (!reload()) save();
    }

    private void validateCategoryValues(List<String> categories, int expectedCount) {
        if (categories == null || categories.size() != expectedCount || categories.stream().anyMatch(category -> !ingredientService.getCategories().contains(category))) {
            throw new IllegalArgumentException("Choose a valid category for each missing ingredient.");
        }
    }

    private void validateCategories(List<RecipeIngredient> ingredients, List<String> categories) { validateCategoryValues(categories, ingredients.size()); }

    public synchronized List<ShoppingItem> findAll() {
        return items.stream().sorted(Comparator.comparing(ShoppingItem::isPurchased).thenComparing(ShoppingItem::getName)).toList();
    }

    public synchronized int addMissing(List<RecipeIngredient> ingredients, List<String> categories) {
        addMissingItems(ingredients, categories); save(); return ingredients.size();
    }

    public synchronized boolean addMissingFromRecipe(String recipeId, List<RecipeIngredient> ingredients, List<String> categories) {
        validateCategories(ingredients, categories);
        if (!addedRecipeIds.add(recipeId)) return false;
        addMissingItems(ingredients, categories); save(); return true;
    }

    public synchronized int addMissingFromRecipes(List<RecipeMatch> recipes, List<String> categories) {
        int expectedCategories = recipes.stream().mapToInt(RecipeMatch::getMissingCount).sum();
        validateCategoryValues(categories, expectedCategories);
        int addedCount = 0; int categoryOffset = 0;
        for (RecipeMatch match : recipes) {
            int missingCount = match.getMissingCount();
            List<String> recipeCategories = categories.subList(categoryOffset, categoryOffset + missingCount);
            if (addedRecipeIds.add(match.getRecipe().getId())) { addMissingItems(match.getMissingIngredients(), recipeCategories); addedCount += missingCount; }
            categoryOffset += missingCount;
        }
        save(); return addedCount;
    }

    public synchronized void forgetRecipe(String recipeId) { if (addedRecipeIds.remove(recipeId)) save(); }

    private void addMissingItems(List<RecipeIngredient> ingredients, List<String> categories) {
        validateCategories(ingredients, categories);
        for (int i = 0; i < ingredients.size(); i++) {
            RecipeIngredient ingredient = ingredients.get(i); String category = categories.get(i);
            ShoppingItem existing = items.stream().filter(item -> !item.isPurchased())
                    .filter(item -> item.getUnit().equalsIgnoreCase(ingredient.getUnit()))
                    .filter(item -> IngredientNames.key(item.getName()).equals(IngredientNames.key(ingredient.getName()))).findFirst().orElse(null);
            if (existing == null) {
                items.add(new ShoppingItem(UUID.randomUUID().toString(), ingredient.getName(), ingredient.getQuantity(), ingredient.getUnit(), category));
            } else {
                existing.addQuantity(ingredient.getQuantity());
                if (existing.getCategory() == null) existing.setCategory(category);
            }
        }
    }

    public synchronized boolean togglePurchased(String id, String category) {
        ShoppingItem item = findById(id); if (item == null) return false;
        if (!item.isPurchased() && !item.isAddedToPantry()) {
            String selectedCategory = category == null || category.isBlank() ? item.getCategory() : category;
            if (!ingredientService.getCategories().contains(selectedCategory)) throw new IllegalArgumentException("Choose a valid ingredient category before picking up this item.");
            String pantryCategory = ingredientService.addOrIncrement(item.getName(), selectedCategory, item.getQuantity(), item.getUnit());
            item.setCategory(pantryCategory); item.markAddedToPantry();
        }
        item.togglePurchased(); save(); return true;
    }

    public synchronized boolean remove(String id) {
        boolean removed = items.removeIf(item -> item.getId().equals(id)); if (removed) save(); return removed;
    }

    public synchronized int activeCount() { return (int) items.stream().filter(item -> !item.isPurchased()).count(); }
    public synchronized int totalCount() { return items.size(); }

    private ShoppingItem findById(String id) { return items.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null); }

    private void save() {
        if (storage == null) return;
        List<StoredShoppingItem> savedItems = items.stream().map(item -> new StoredShoppingItem(item.getId(), item.getName(), item.getQuantity(), item.getUnit(), item.isPurchased(), item.getCategory(), item.isAddedToPantry())).toList();
        storage.write("shopping-list.json", new StoredShoppingList(savedItems, new HashSet<>(addedRecipeIds)));
    }

    private boolean reload() {
        if (storage == null) return false;
        java.util.Optional<StoredShoppingList> saved = storage.read("shopping-list.json", new TypeReference<>() { });
        if (saved.isEmpty()) return false;
        items.clear();
        saved.get().items().stream().map(item -> new ShoppingItem(item.id(), item.name(), item.quantity(), item.unit(), item.category(), item.purchased(), item.addedToPantry())).forEach(items::add);
        addedRecipeIds.clear(); addedRecipeIds.addAll(saved.get().addedRecipeIds());
        return true;
    }

    private record StoredShoppingList(List<StoredShoppingItem> items, Set<String> addedRecipeIds) { }
    private record StoredShoppingItem(String id, String name, int quantity, String unit, boolean purchased, String category, boolean addedToPantry) { }
}
