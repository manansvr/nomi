package com.nomi.recipe;

import com.nomi.ingredient.IngredientService;
import com.nomi.shopping.ShoppingListService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class RecipeController {
    private final IngredientService ingredientService;
    private final RecipeCatalogService recipeCatalogService;
    private final ShoppingListService shoppingListService;

    public RecipeController(IngredientService ingredientService,
                            RecipeCatalogService recipeCatalogService,
                            ShoppingListService shoppingListService) {
        this.ingredientService = ingredientService;
        this.recipeCatalogService = recipeCatalogService;
        this.shoppingListService = shoppingListService;
    }

    @GetMapping({"/", "/recipes"})
    public String recipes(Model model) {
        addRecipePageData(model);
        model.addAttribute("ingredientCount", ingredientService.count());
        model.addAttribute("lowStockCount", ingredientService.lowStockCount());
        model.addAttribute("today", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")));
        return "recipes";
    }

    @GetMapping("/recipe-ingredients")
    public String recipeIngredients(Model model) {
        addRecipePageData(model);
        return "recipe-ingredients";
    }

    @GetMapping("/recipes/new")
    public String newRecipe(Model model) {
        model.addAttribute("categories", ingredientService.getCategories());
        return "recipe-form";
    }

    @PostMapping("/recipes")
    public String addRecipe(@RequestParam String name,
                            @RequestParam String description,
                            @RequestParam int minutes,
                            @RequestParam(required = false) List<String> ingredientNames,
                            @RequestParam(required = false) List<Integer> ingredientQuantities,
                            @RequestParam(required = false) List<String> ingredientUnits,
                            @RequestParam(required = false) List<String> ingredientCategories,
                            @RequestParam String instructions,
                            RedirectAttributes redirectAttributes) {
        if (name.isBlank() || description.isBlank() || minutes < 1 || minutes > 600
                || ingredientNames == null || ingredientNames.isEmpty()
                || ingredientQuantities == null || ingredientUnits == null
                || ingredientNames.size() != ingredientQuantities.size()
                || ingredientNames.size() != ingredientUnits.size()) {
            redirectAttributes.addFlashAttribute("error", "Complete the recipe details and at least one ingredient.");
            return "redirect:/recipes/new";
        }
        if (ingredientCategories == null || ingredientCategories.size() != ingredientNames.size()
                || ingredientCategories.stream().anyMatch(category -> !ingredientService.getCategories().contains(category))) {
            redirectAttributes.addFlashAttribute("error", "Choose a category for each ingredient.");
            return "redirect:/recipes/new";
        }

        List<RecipeIngredient> ingredients = new ArrayList<>();
        for (int i = 0; i < ingredientNames.size(); i++) {
            String ingredientName = ingredientNames.get(i).trim();
            String unit = ingredientUnits.get(i).trim();
            int quantity = ingredientQuantities.get(i);
            if (ingredientName.isEmpty() || unit.isEmpty() || quantity < 1 || quantity > 999) {
                redirectAttributes.addFlashAttribute("error", "Each ingredient needs a name, amount and unit.");
                return "redirect:/recipes/new";
            }
            ingredients.add(new RecipeIngredient(ingredientName, quantity, unit, ingredientCategories.get(i)));
        }

        List<String> steps = instructions.lines().map(String::trim).filter(step -> !step.isEmpty()).toList();
        if (steps.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Add at least one cooking instruction.");
            return "redirect:/recipes/new";
        }

        recipeCatalogService.addRecipe(name, description, minutes, ingredients, steps);
        redirectAttributes.addFlashAttribute("notice", name.trim() + " added to your recipes.");
        return "redirect:/recipes";
    }

    @PostMapping("/recipes/{id}/delete")
    public String removeRecipe(@PathVariable String id, RedirectAttributes redirectAttributes) {
        if (recipeCatalogService.removeRecipe(id)) {
            shoppingListService.forgetRecipe(id);
            redirectAttributes.addFlashAttribute("notice", "Recipe removed.");
        } else {
            redirectAttributes.addFlashAttribute("error", "That recipe could not be found.");
        }
        return "redirect:/recipes";
    }

    @PostMapping("/recipes/{id}/favorite")
    public String toggleFavorite(@PathVariable String id,
                                 @RequestParam(required = false) List<String> categories,
                                 RedirectAttributes redirectAttributes) {
        RecipeMatch match = recipeCatalogService.findMatch(id);
        if (match == null) {
            redirectAttributes.addFlashAttribute("error", "That recipe could not be found.");
        } else if (match.isFavorite()) {
            recipeCatalogService.toggleFavorite(id);
            redirectAttributes.addFlashAttribute("notice",
                    "Removed from favorites. Existing shopping list items were kept.");
        } else if (!hasValidCategories(categories, match.getMissingCount())) {
            redirectAttributes.addFlashAttribute("error", "Choose a category for each missing ingredient.");
        } else {
            boolean addedMissing = match.getMissingCount() > 0
                    && shoppingListService.addMissingFromRecipe(id, match.getMissingIngredients(), categories);
            recipeCatalogService.toggleFavorite(id);
            redirectAttributes.addFlashAttribute("notice",
                    match.getMissingCount() == 0
                            ? "Added to favorites."
                            : addedMissing
                                    ? "Added to favorites and added missing ingredients to your shopping list."
                                    : "Added to favorites. Missing ingredients are already on your shopping list.");
        }
        return "redirect:/recipes";
    }

    @PostMapping("/shopping-list/add/{recipeId}")
    public String addRecipeMissingIngredients(@PathVariable String recipeId,
                                              @RequestParam(required = false) List<String> categories,
                                              RedirectAttributes redirectAttributes) {
        RecipeMatch match = recipeCatalogService.findMatch(recipeId);
        if (match == null) {
            redirectAttributes.addFlashAttribute("error", "That recipe could not be found.");
        } else if (match.isReadyToCook()) {
            redirectAttributes.addFlashAttribute("notice",
                    "You already have everything for " + match.getRecipe().getName() + ".");
        } else if (!hasValidCategories(categories, match.getMissingCount())) {
            redirectAttributes.addFlashAttribute("error", "Choose a category for each missing ingredient.");
        } else if (!shoppingListService.addMissingFromRecipe(recipeId, match.getMissingIngredients(), categories)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Missing ingredients from " + match.getRecipe().getName() + " are already on your list.");
        } else {
            redirectAttributes.addFlashAttribute("notice",
                    "Missing ingredients from " + match.getRecipe().getName() + " added to your list.");
        }
        return "redirect:/recipe-ingredients";
    }

    private void addRecipePageData(Model model) {
        var matches = recipeCatalogService.findMatches();
        model.addAttribute("recipeMatches", matches);
        model.addAttribute("readyRecipeCount",
                matches.stream().filter(RecipeMatch::isReadyToCook).count());
        model.addAttribute("categories", ingredientService.getCategories());
    }

    private boolean hasValidCategories(List<String> categories, int expectedCount) {
        if (expectedCount == 0) return categories == null || categories.isEmpty();
        return categories != null
                && categories.size() == expectedCount
                && categories.stream().allMatch(ingredientService.getCategories()::contains);
    }
}
