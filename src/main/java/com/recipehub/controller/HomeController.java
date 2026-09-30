package com.recipehub.controller;

import com.recipehub.service.RecipeService;
import com.recipehub.service.RecipeTranslationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Locale;

@Controller
public class HomeController {

    private final RecipeService recipeService;
    private final RecipeTranslationService recipeTranslationService;

    public HomeController(RecipeService recipeService, RecipeTranslationService recipeTranslationService) {
        this.recipeService = recipeService;
        this.recipeTranslationService = recipeTranslationService;
    }

    @GetMapping("/")
    public String index(Model model, Locale locale) {
        var recipes = recipeService.findAll();
        model.addAttribute("recipes", recipes);
        model.addAttribute("recipeTranslations", recipeTranslationService.findEnglishTranslations(recipes, locale));
        return "index";
    }
}
