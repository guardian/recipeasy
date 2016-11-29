package com.gu.recipeasy.models

import com.gu.recipeasy.db.DB

object RecipeReadiness {
  def selectNewRecipesWithNonEmptyIngredientLists(db: DB): List[Recipe] = {
    db.getRecipes().filter(recipe => ((recipe.ingredientsLists.lists.size > 0) && (recipe.status == New)))
  }
  def migrateNewRecipesWithNonEmptyIngredientLists(db: DB): Int = {
    val recipes: List[Recipe] = selectNewRecipesWithNonEmptyIngredientLists(db)
    recipes.foreach(recipe => db.setOriginalRecipeStatus(recipe.id, Ready))
    recipes.size
  }
  def selectNigelSlaterRecipes(db: DB): List[Recipe] = {
    def recipeIsNigelSlaterRecipe(recipe: Recipe): Boolean = {
      recipe.credit.contains("Nigel") && recipe.credit.contains("Slater")
    }
    db.getRecipes().filter(recipe => recipeIsNigelSlaterRecipe(recipe))
  }
  def migrateNigelSlaterRecipes(db: DB): Int = {
    val recipes: List[Recipe] = selectNigelSlaterRecipes(db)
    recipes.foreach(recipe => db.setOriginalRecipeStatus(recipe.id, Impossible))
    recipes.size
  }
  def updateRecipesReadiness(db: DB): Int = {
    val count1: Int = migrateNewRecipesWithNonEmptyIngredientLists(db: DB)
    val count2: Int = migrateNigelSlaterRecipes(db: DB)
    count1 + count2
  }
}
