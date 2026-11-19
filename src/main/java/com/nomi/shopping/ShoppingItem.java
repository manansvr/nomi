package com.nomi.shopping;

public class ShoppingItem {
    private final String id;
    private final String name;
    private final String unit;
    private String category;
    private int quantity;
    private boolean purchased;
    private boolean addedToPantry;

    public ShoppingItem(String id, String name, int quantity, String unit) {
        this(id, name, quantity, unit, null, false, false);
    }

    public ShoppingItem(String id, String name, int quantity, String unit, boolean purchased) {
        this(id, name, quantity, unit, null, purchased, false);
    }

    public ShoppingItem(String id, String name, int quantity, String unit, String category) {
        this(id, name, quantity, unit, category, false, false);
    }

    public ShoppingItem(String id, String name, int quantity, String unit, String category,
                        boolean purchased, boolean addedToPantry) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.purchased = purchased;
        this.addedToPantry = addedToPantry;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public String getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public boolean isAddedToPantry() {
        return addedToPantry;
    }

    public String getAmount() {
        return quantity + " " + unit;
    }

    void addQuantity(int amount) {
        quantity += amount;
    }

    void setCategory(String category) {
        this.category = category;
    }

    void markAddedToPantry() {
        addedToPantry = true;
    }

    void togglePurchased() {
        purchased = !purchased;
    }
}
