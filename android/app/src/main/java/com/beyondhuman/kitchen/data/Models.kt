package com.beyondhuman.kitchen.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    val id: String,
    val sourceRecipeNumber: Int,
    val title: String,
    val description: String = "",
    @SerialName("_section") val section: String = "",
    val shoppingList: List<String> = emptyList(),
    val method: List<String> = emptyList(),
    val sourcePages: List<Int> = emptyList(),
    val qualityFlags: List<String> = emptyList()
)

@Serializable
data class RecipeCatalog(
    val schemaVersion: Int,
    val source: String,
    val recipeCount: Int,
    val recipes: List<Recipe>
)
