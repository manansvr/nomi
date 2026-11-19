package com.nomi.shopping;

import com.nomi.ingredient.IngredientService;
import com.nomi.recipe.RecipeCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ShoppingListController {
    private final ShoppingListService shoppingListService;
    private final RecipeCatalogService recipeCatalogService;
    private final IngredientService ingredientService;

    public ShoppingListController(ShoppingListService shoppingListService,
                                  RecipeCatalogService recipeCatalogService,
                                  IngredientService ingredientService) {
        this.shoppingListService = shoppingListService;
        this.recipeCatalogService = recipeCatalogService;
        this.ingredientService = ingredientService;
    }

    @GetMapping("/shopping-list")
    public String shoppingList(Model model) {
        model.addAttribute("items", shoppingListService.findAll());
        model.addAttribute("itemCount", shoppingListService.activeCount());
        model.addAttribute("favoriteCount", recipeCatalogService.favoriteCount());
        model.addAttribute("categories", ingredientService.getCategories());
        return "shopping-list";
    }

    @PostMapping("/shopping-list/{id}/toggle")
    public String togglePurchased(@PathVariable String id,
                                  @RequestParam String category,
                                  RedirectAttributes redirectAttributes) {
        if (!ingredientService.getCategories().contains(category)) {
            redirectAttributes.addFlashAttribute("error", "Choose a valid category before picking up this item.");
        } else if (!shoppingListService.togglePurchased(id, category)) {
            redirectAttributes.addFlashAttribute("error", "That shopping item could not be found.");
        }
        return "redirect:/shopping-list";
    }

    @PostMapping("/shopping-list/{id}/delete")
    public String removeItem(@PathVariable String id, RedirectAttributes redirectAttributes) {
        if (!shoppingListService.remove(id)) {
            redirectAttributes.addFlashAttribute("error", "That shopping item could not be found.");
        }
        return "redirect:/shopping-list";
    }
}
