package com.recipehub.service;

import com.recipehub.dto.RecipeForm;
import com.recipehub.model.Recipe;
import com.recipehub.model.User;
import com.recipehub.repository.RecipeRepository;
import com.recipehub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Recipe createRecipe(RecipeForm form, String userEmail) {
        String email = userEmail.trim().toLowerCase(Locale.ROOT);
        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Current user was not found"));

        Recipe recipe = new Recipe(
                form.getTitle(),
                form.getDescription(),
                form.getIngredients(),
                form.getInstructions(),
                author
        );

        return recipeRepository.save(recipe);
    }

    @Transactional(readOnly = true)
    public Optional<Recipe> findById(UUID id) {
        return recipeRepository.findById(id)
                .map(recipe -> {
                    recipe.getAuthor().getName();
                    return recipe;
                });
    }
}
