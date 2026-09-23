package com.recipehub.repository;

import com.recipehub.model.Recipe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    @EntityGraph(attributePaths = "author")
    List<Recipe> findAllByOrderByCreatedAtDesc();

    List<Recipe> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    @EntityGraph(attributePaths = "author")
    Optional<Recipe> findWithAuthorById(UUID id);
}
