package com.nomi.ingredient;

public class Ingredient {
    private final String id;
    private final String name;
    private final String category;
    private final String unit;
    private int quantity;

    public Ingredient(String id, String name, String category, int quantity, String unit) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isLowStock() {
        return quantity <= 2;
    }

    public void adjustQuantity(int amount) {
        quantity = Math.max(0, quantity + amount);
    }
}
