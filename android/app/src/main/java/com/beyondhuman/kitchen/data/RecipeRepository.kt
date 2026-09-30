package com.beyondhuman.kitchen.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class RecipeRepository(private val context: Context) {
    companion object {
        private const val EXPECTED_RECIPE_COUNT = 424
        private const val DATABASE_NAME = "kitchen.db"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val db = Room.databaseBuilder(context, KitchenDatabase::class.java, DATABASE_NAME)
        .fallbackToDestructiveMigration()
        .build()

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        db.withTransaction {
            val dao = db.recipes()
            if (dao.count() == EXPECTED_RECIPE_COUNT) return@withTransaction

            val catalog = context.assets.open("recipes_424.json").bufferedReader().use {
                json.decodeFromString<RecipeCatalog>(it.readText())
            }
            require(catalog.recipeCount == EXPECTED_RECIPE_COUNT)
            require(catalog.recipes.size == EXPECTED_RECIPE_COUNT)
            require(catalog.recipes.map { it.sourceRecipeNumber } == (1..EXPECTED_RECIPE_COUNT).toList())
            require(catalog.recipes.map { it.id }.distinct().size == EXPECTED_RECIPE_COUNT)

            dao.deleteAll()
            dao.insertAll(catalog.recipes.map { it.toEntity() })
            check(dao.count() == EXPECTED_RECIPE_COUNT)
        }
    }

    suspend fun all(): List<Recipe> = withContext(Dispatchers.IO) {
        db.recipes().all().map { it.toModel() }
    }

    suspend fun search(query: String, ingredients: List<String> = emptyList()): List<Recipe> =
        withContext(Dispatchers.IO) {
            val normalizedQuery = query.trim()
            val normalizedIngredients = ingredients.map(String::trim).filter(String::isNotBlank)
            if (normalizedQuery.isBlank() && normalizedIngredients.isEmpty()) return@withContext all()

            val escapedQuery = normalizedQuery.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            val candidates = db.recipes().search("%$escapedQuery%").map { it.toModel() }
            if (normalizedIngredients.isEmpty()) return@withContext candidates

            val terms = normalizedIngredients.map(String::lowercase)
            candidates.filter { recipe ->
                val haystack = buildString {
                    append(recipe.title).append(' ')
                    append(recipe.description).append(' ')
                    append(recipe.section).append(' ')
                    recipe.shoppingList.forEach { append(it).append(' ') }
                }.lowercase()
                terms.all { it in haystack }
            }
        }

    private fun Recipe.toEntity() = RecipeEntity(
        id, sourceRecipeNumber, title, description, section,
        json.encodeToString(value = shoppingList), json.encodeToString(value = method),
        json.encodeToString(value = sourcePages), json.encodeToString(value = qualityFlags)
    )

    private fun RecipeEntity.toModel() = Recipe(
        id, sourceRecipeNumber, title, description, section,
        json.decodeFromString(shoppingListJson), json.decodeFromString(methodJson),
        json.decodeFromString(sourcePagesJson), json.decodeFromString(qualityFlagsJson)
    )
}
