package com.beyondhuman.kitchen.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.beyondhuman.kitchen.data.Recipe
import org.junit.Rule
import org.junit.Test

class RecipeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersRecipeDetail() {
        val recipe = Recipe(
            id = "test-1",
            sourceRecipeNumber = 1,
            title = "No-Bake Pineapple Pie",
            section = "Recipes by banana",
            shoppingList = listOf("pineapple", "cream"),
            method = listOf("Combine ingredients.", "Chill before serving.")
        )

        composeRule.setContent {
            RecipeScreen(recipe = recipe, onBack = {})
        }

        composeRule.onNodeWithText("No-Bake Pineapple Pie").assertIsDisplayed()
        composeRule.onNodeWithText("Recipes by banana").assertIsDisplayed()
        composeRule.onNodeWithText("Shopping list").assertIsDisplayed()
        composeRule.onNodeWithText("• pineapple").assertIsDisplayed()
        composeRule.onNodeWithText("Method").assertIsDisplayed()
        composeRule.onNodeWithText("1. Combine ingredients.").assertIsDisplayed()
        composeRule.onNodeWithText("2. Chill before serving.").assertIsDisplayed()
    }
}
