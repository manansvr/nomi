package com.nomi.ingredient;

import java.util.Locale;

public final class IngredientNames {
    private IngredientNames() {
    }

    public static String key(String name) {
        String normalized = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .trim()
                .replaceAll("\\s+", " ");

        if (normalized.contains("avocado")) return "avocado";
        if (normalized.contains("spinach")) return "spinach";
        if (normalized.contains("tomato")) return "tomato";
        if (normalized.contains("egg")) return "egg";
        if (normalized.contains("feta")) return "feta";
        if (normalized.contains("sourdough") || normalized.equals("bread")) return "bread";
        if (normalized.contains("chickpea")) return "chickpea";
        if (normalized.contains("olive oil")) return "olive oil";
        if (normalized.endsWith("ies")) return normalized.substring(0, normalized.length() - 3) + "y";
        if (normalized.endsWith("s") && !normalized.endsWith("ss")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
