package com.beyondhuman.kitchen.ui

import com.beyondhuman.kitchen.data.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryFilterTest {
    private fun recipe(id: String, section: String) = Recipe(
        id = id,
        sourceRecipeNumber = id.toInt(),
        title = "Recipe $id",
        section = section
    )

    @Test
    fun categoryOptions_areDistinctNonBlankAndSorted() {
        val recipes = listOf(
            recipe("1", "Desserts"),
            recipe("2", "breakfast"),
            recipe("3", "Desserts"),
            recipe("4", " "),
            recipe("5", "Mains")
        )

        assertEquals(listOf("breakfast", "Desserts", "Mains"), categoryOptions(recipes))
    }

    @Test
    fun filterRecipesBySection_isCaseInsensitive_andNullMeansAll() {
        val recipes = listOf(
            recipe("1", "Desserts"),
            recipe("2", "Mains"),
            recipe("3", "desserts")
        )

        assertEquals(3, filterRecipesBySection(recipes, null).size)
        assertEquals(listOf("1", "3"), filterRecipesBySection(recipes, "DESSERTS").map { it.id })
        assertTrue(filterRecipesBySection(recipes, "Unknown").isEmpty())
    }
}
