package com.recipehub.service;

import com.recipehub.model.Recipe;
import com.recipehub.model.RecipeTranslation;
import com.recipehub.repository.RecipeTranslationRepository;
import com.recipehub.service.DeepLTranslationClient.TranslatedRecipe;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecipeTranslationService {

    private static final String ENGLISH = "en";
    private static final String RUSSIAN = "ru";

    private final RecipeTranslationRepository recipeTranslationRepository;
    private final DeepLTranslationClient deepLTranslationClient;

    public RecipeTranslationService(
            RecipeTranslationRepository recipeTranslationRepository,
            DeepLTranslationClient deepLTranslationClient
    ) {
        this.recipeTranslationRepository = recipeTranslationRepository;
        this.deepLTranslationClient = deepLTranslationClient;
    }

    public RecipeContent getRecipeContent(Recipe recipe, Locale locale) {
        String languageCode = normalizeLanguage(locale);
        if (RUSSIAN.equals(languageCode)) {
            return original(recipe, false);
        }

        Optional<RecipeTranslation> existingTranslation = findTranslation(recipe.getId(), ENGLISH);
        if (existingTranslation.isPresent()) {
            return translated(existingTranslation.get());
        }

        if (!deepLTranslationClient.isAvailable()) {
            return original(recipe, true);
        }

        Optional<TranslatedRecipe> translatedRecipe = deepLTranslationClient.translateToEnglish(recipe);
        if (translatedRecipe.isEmpty()) {
            return original(recipe, true);
        }

        Optional<RecipeTranslation> savedTranslation = saveTranslation(recipe, translatedRecipe.get());
        return savedTranslation.map(this::translated).orElseGet(() -> original(recipe, true));
    }

    @Transactional(readOnly = true)
    public Map<UUID, RecipeTranslation> findEnglishTranslations(List<Recipe> recipes, Locale locale) {
        if (!ENGLISH.equals(normalizeLanguage(locale)) || recipes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> recipeIds = recipes.stream()
                .map(Recipe::getId)
                .toList();

        return recipeTranslationRepository.findByRecipeIdInAndLanguageCode(recipeIds, ENGLISH)
                .stream()
                .collect(Collectors.toMap(translation -> translation.getRecipe().getId(), Function.identity()));
    }

    private Optional<RecipeTranslation> findTranslation(UUID recipeId, String languageCode) {
        return recipeTranslationRepository.findByRecipeIdAndLanguageCode(recipeId, languageCode);
    }

    private Optional<RecipeTranslation> saveTranslation(Recipe recipe, TranslatedRecipe translatedRecipe) {
        try {
            RecipeTranslation translation = new RecipeTranslation(
                    recipe,
                    ENGLISH,
                    translatedRecipe.title(),
                    translatedRecipe.description(),
                    translatedRecipe.ingredients(),
                    translatedRecipe.instructions()
            );
            return Optional.of(recipeTranslationRepository.save(translation));
        } catch (DataIntegrityViolationException exception) {
            return findTranslation(recipe.getId(), ENGLISH);
        }
    }

    private String normalizeLanguage(Locale locale) {
        String language = locale == null ? RUSSIAN : locale.getLanguage();
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return ENGLISH.equals(normalized) ? ENGLISH : RUSSIAN;
    }

    private RecipeContent original(Recipe recipe, boolean translationUnavailable) {
        return new RecipeContent(
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getIngredients(),
                recipe.getInstructions(),
                false,
                translationUnavailable
        );
    }

    private RecipeContent translated(RecipeTranslation translation) {
        return new RecipeContent(
                translation.getTitle(),
                translation.getDescription(),
                translation.getIngredients(),
                translation.getInstructions(),
                true,
                false
        );
    }

    public record RecipeContent(
            String title,
            String description,
            String ingredients,
            String instructions,
            boolean translated,
            boolean translationUnavailable
    ) {
    }
}
