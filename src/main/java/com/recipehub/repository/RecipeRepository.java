package com.recipehub.repository;

import com.recipehub.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    List<Recipe> findAllByOrderByCreatedAtDesc();

    List<Recipe> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);
}
