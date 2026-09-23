package com.recipehub.controller;

import com.recipehub.dto.RecipeForm;
import com.recipehub.model.Recipe;
import com.recipehub.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

@Controller
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/recipes/new")
    public String newRecipeForm(Model model) {
        if (!model.containsAttribute("recipeForm")) {
            model.addAttribute("recipeForm", new RecipeForm());
        }
        return "recipes/new";
    }

    @PostMapping("/recipes/new")
    public String createRecipe(
            @Valid @ModelAttribute("recipeForm") RecipeForm form,
            BindingResult bindingResult,
            Principal principal
    ) {
        if (bindingResult.hasErrors()) {
            return "recipes/new";
        }

        Recipe recipe = recipeService.createRecipe(form, principal.getName());
        return "redirect:/recipes/" + recipe.getId();
    }

    @GetMapping("/recipes/{id}")
    public String recipeDetails(@PathVariable UUID id, Model model) {
        Recipe recipe = recipeService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("recipe", recipe);
        return "recipes/details";
    }
}
