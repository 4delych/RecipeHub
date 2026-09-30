package com.recipehub.repository;

import com.recipehub.model.RecipeTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeTranslationRepository extends JpaRepository<RecipeTranslation, UUID> {

    Optional<RecipeTranslation> findByRecipeIdAndLanguageCode(UUID recipeId, String languageCode);

    List<RecipeTranslation> findByRecipeIdInAndLanguageCode(Collection<UUID> recipeIds, String languageCode);
}
