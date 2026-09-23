package com.recipehub.service;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.recipehub.model.Recipe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DeepLTranslationClient {

    private static final String TARGET_LANGUAGE = "EN-US";

    private final DeepLClient client;

    public DeepLTranslationClient(@Value("${deepl.api-key:}") String apiKey) {
        this.client = StringUtils.hasText(apiKey) ? new DeepLClient(apiKey.trim()) : null;
    }

    public boolean isAvailable() {
        return client != null;
    }

    public Optional<TranslatedRecipe> translateToEnglish(Recipe recipe) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            List<String> texts = new ArrayList<>();
            texts.add(recipe.getTitle());
            if (StringUtils.hasText(recipe.getDescription())) {
                texts.add(recipe.getDescription());
            }
            texts.add(recipe.getIngredients());
            texts.add(recipe.getInstructions());

            List<TextResult> results = client.translateText(texts, null, TARGET_LANGUAGE);
            int index = 0;
            String title = results.get(index++).getText();
            String description = null;
            if (StringUtils.hasText(recipe.getDescription())) {
                description = results.get(index++).getText();
            }
            String ingredients = results.get(index++).getText();
            String instructions = results.get(index).getText();

            return Optional.of(new TranslatedRecipe(title, description, ingredients, instructions));
        } catch (DeepLException exception) {
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    public record TranslatedRecipe(
            String title,
            String description,
            String ingredients,
            String instructions
    ) {
    }
}
