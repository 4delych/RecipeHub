package com.recipehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RecipeForm {

    @NotBlank(message = "{validation.recipe.title.required}")
    @Size(max = 150, message = "{validation.recipe.title.size}")
    private String title;

    private String description;

    @NotBlank(message = "{validation.recipe.ingredients.required}")
    private String ingredients;

    @NotBlank(message = "{validation.recipe.instructions.required}")
    private String instructions;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
