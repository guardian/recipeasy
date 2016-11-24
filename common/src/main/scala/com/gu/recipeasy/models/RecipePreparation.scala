package com.gu.recipeasy.models

import java.time.OffsetDateTime

import com.gu.recipeasy.db.DB

object RecipePreparation {
  def selectRecipes(db: DB): List[Recipe] = {
    db.getRecipes().filter(recipe => ((recipe.ingredientsLists.lists.size >= 5) && (recipe.status == New)))
  }
}
