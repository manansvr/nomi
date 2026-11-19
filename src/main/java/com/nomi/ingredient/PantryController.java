package com.nomi.ingredient;

import com.nomi.recipe.RecipeCatalogService;
import com.nomi.recipe.RecipeMatch;
import com.nomi.shopping.ShoppingListService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class PantryController {
    private final IngredientService ingredientService;
    private final RecipeCatalogService recipeCatalogService;
    private final ShoppingListService shoppingListService;

    public PantryController(IngredientService ingredientService,
                            RecipeCatalogService recipeCatalogService,
                            ShoppingListService shoppingListService) {
        this.ingredientService = ingredientService;
        this.recipeCatalogService = recipeCatalogService;
        this.shoppingListService = shoppingListService;
    }

    @GetMapping("/pantry")
    public String home(@RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "") String category,
                       Model model) {
        model.addAttribute("ingredients", ingredientService.find(search, category));
        model.addAttribute("categories", ingredientService.getCategories());
        model.addAttribute("ingredientCount", ingredientService.count());
        model.addAttribute("lowStockCount", ingredientService.lowStockCount());
        model.addAttribute("search", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("today", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")));
        var recipeMatches = recipeCatalogService.findMatches();
        model.addAttribute("recipeMatches", recipeMatches);
        model.addAttribute("readyRecipeCount",
                recipeMatches.stream().filter(RecipeMatch::isReadyToCook).count());
        model.addAttribute("favoriteCount", recipeCatalogService.favoriteCount());
        model.addAttribute("shoppingListCount", shoppingListService.activeCount());
        return "index";
    }

    @PostMapping("/ingredients")
    public String addIngredient(@RequestParam String name,
                                @RequestParam String category,
                                @RequestParam int quantity,
                                @RequestParam String unit,
                                RedirectAttributes redirectAttributes) {
        if (name.isBlank() || quantity < 1 || quantity > 999 || unit.isBlank()
                || !ingredientService.getCategories().contains(category)) {
            redirectAttributes.addFlashAttribute("error", "Add a name, category, quantity and unit to continue.");
            return "redirect:/pantry";
        }

        ingredientService.add(name, category, quantity, unit);
        redirectAttributes.addFlashAttribute("notice", name.trim() + " added to your pantry.");
        return "redirect:/pantry";
    }

    @PostMapping("/ingredients/{id}/quantity")
    public String adjustQuantity(@PathVariable String id,
                                 @RequestParam int amount,
                                 RedirectAttributes redirectAttributes) {
        if ((amount != -1 && amount != 1) || !ingredientService.adjust(id, amount)) {
            redirectAttributes.addFlashAttribute("error", "That pantry update could not be made.");
        }
        return "redirect:/pantry";
    }

    @PostMapping("/ingredients/{id}/delete")
    public String removeIngredient(@PathVariable String id, RedirectAttributes redirectAttributes) {
        if (ingredientService.remove(id)) {
            redirectAttributes.addFlashAttribute("notice", "Ingredient removed from your pantry.");
        } else {
            redirectAttributes.addFlashAttribute("error", "That ingredient could not be found.");
        }
        return "redirect:/pantry";
    }
}
