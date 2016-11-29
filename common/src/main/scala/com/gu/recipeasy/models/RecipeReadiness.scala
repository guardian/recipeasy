package com.gu.recipeasy.models

import com.gu.recipeasy.db.DB

object RecipeReadiness {
  def selectRecipes(db: DB): List[Recipe] = {
    db.getRecipes().filter(recipe => ((recipe.ingredientsLists.lists.size >= 2) && (recipe.status == New)))
  }
  def updateRecipesReadiness(db: DB): Int = {
    val recipes: List[Recipe] = selectRecipes(db)
    recipes.foreach(recipe => db.setOriginalRecipeStatus(recipe.id, Ready))
    recipes.size
  }
}
