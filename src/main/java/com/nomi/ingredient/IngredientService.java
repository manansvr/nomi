package com.nomi.ingredient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nomi.persistence.PersistentJsonStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class IngredientService {
    private final List<Ingredient> ingredients = new ArrayList<>(List.of(
            new Ingredient("1", "Avocados", "Fruits", 3, "pcs"),
            new Ingredient("2", "Baby spinach", "Vegetable", 1, "bag"),
            new Ingredient("3", "Cherry tomatoes", "Vegetable", 2, "cups"),
            new Ingredient("4", "Free-range eggs", "Protein", 6, "pcs"),
            new Ingredient("5", "Feta cheese", "Protein", 1, "block"),
            new Ingredient("6", "Sourdough bread", "Carbohydrate", 4, "slices"),
            new Ingredient("7", "Chickpeas", "Protein", 2, "cans"),
            new Ingredient("8", "Extra virgin olive oil", "Spices", 1, "bottle")
    ));

    private final List<String> categories = List.of("Protein", "Carbohydrate", "Vegetable", "Fruits", "Spices");
    private PersistentJsonStore storage;

    @Autowired
    public synchronized void configureStorage(PersistentJsonStore storage) {
        this.storage = storage;
        if (!reload()) save();
    }

    public synchronized List<Ingredient> find(String search, String category) {
        reload();
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String normalizedCategory = category == null ? "" : category.trim();
        return ingredients.stream()
                .filter(ingredient -> ingredient.getName().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .filter(ingredient -> normalizedCategory.isBlank()
                        || ingredient.getCategory().equals(normalizedCategory))
                .sorted(Comparator.comparing(Ingredient::getCategory).thenComparing(Ingredient::getName))
                .toList();
    }

    public List<String> getCategories() {
        return categories;
    }

    public synchronized int count() {
        reload();
        return ingredients.size();
    }

    public synchronized long lowStockCount() {
        reload();
        return ingredients.stream().filter(Ingredient::isLowStock).count();
    }

    public synchronized boolean hasIngredient(String name) {
        String key = IngredientNames.key(name);
        return ingredients.stream()
                .anyMatch(ingredient -> ingredient.getQuantity() > 0
                        && IngredientNames.key(ingredient.getName()).equals(key));
    }

    public synchronized void add(String name, String category, int quantity, String unit) {
        reload();
        ingredients.add(new Ingredient(
                UUID.randomUUID().toString(),
                name.trim(),
                category,
                quantity,
                unit.trim()
        ));
        save();
    }

    public synchronized String addOrIncrement(String name, String category, int quantity, String unit) {
        reload();
        String normalizedName = name == null ? "" : name.trim();
        String normalizedUnit = unit == null ? "" : unit.trim();
        if (normalizedName.isEmpty() || normalizedUnit.isEmpty() || quantity < 1
                || !categories.contains(category)) {
            throw new IllegalArgumentException("A name, valid category, positive quantity and unit are required.");
        }

        String key = IngredientNames.key(normalizedName);
        Ingredient existing = ingredients.stream()
                .filter(item -> IngredientNames.key(item.getName()).equals(key))
                .filter(item -> item.getUnit().equalsIgnoreCase(normalizedUnit))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.adjustQuantity(quantity);
            save();
            return existing.getCategory();
        } else {
            ingredients.add(new Ingredient(UUID.randomUUID().toString(), normalizedName,
                    category, quantity, normalizedUnit));
        }
        save();
        return category;
    }

    public synchronized boolean adjust(String id, int amount) {
        reload();
        Optional<Ingredient> ingredient = ingredients.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
        ingredient.ifPresent(item -> item.adjustQuantity(amount));
        if (ingredient.isPresent()) save();
        return ingredient.isPresent();
    }

    public synchronized boolean remove(String id) {
        reload();
        boolean removed = ingredients.removeIf(ingredient -> ingredient.getId().equals(id));
        if (removed) save();
        return removed;
    }

    private void save() {
        if (storage == null) return;
        List<StoredIngredient> saved = ingredients.stream()
                .map(item -> new StoredIngredient(item.getId(), item.getName(), item.getCategory(),
                        item.getQuantity(), item.getUnit()))
                .toList();
        storage.write("ingredients.json", saved);
    }

    public synchronized void refresh() {
        reload();
    }

    private boolean reload() {
        if (storage == null) return false;
        Optional<List<StoredIngredient>> saved = storage.read("ingredients.json", new TypeReference<>() { });
        if (saved.isEmpty()) return false;
        ingredients.clear();
        saved.get().stream()
                .map(item -> new Ingredient(item.id(), item.name(), item.category(), item.quantity(), item.unit()))
                .forEach(ingredients::add);
        return true;
    }

    private record StoredIngredient(String id, String name, String category, int quantity, String unit) { }
}
