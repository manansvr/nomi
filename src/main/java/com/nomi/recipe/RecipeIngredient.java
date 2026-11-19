package com.nomi.recipe;

public class RecipeIngredient {
    private final String name;
    private final int quantity;
    private final String unit;
    private final String category;

    public RecipeIngredient(String name, int quantity, String unit) {
        this(name, quantity, unit, null);
    }

    public RecipeIngredient(String name, int quantity, String unit, String category) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public String getCategory() {
        return category;
    }

    public String getAmount() {
        return quantity + " " + unit;
    }
}
