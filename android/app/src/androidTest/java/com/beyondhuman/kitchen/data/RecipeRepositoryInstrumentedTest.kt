package com.beyondhuman.kitchen.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var repository: RecipeRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("kitchen.db")
        repository = RecipeRepository(context)
    }

    @After
    fun tearDown() {
        context.deleteDatabase("kitchen.db")
    }

    @Test
    fun seedsExactly424RecipesInSourceOrder() = runBlocking {
        repository.ensureSeeded()

        val recipes = repository.all()

        assertEquals(424, recipes.size)
        assertEquals((1..424).toList(), recipes.map { it.sourceRecipeNumber })
        assertFalse(recipes.any { it.id.isBlank() })
        assertFalse(recipes.any { it.title.isBlank() })
    }

    @Test
    fun reseedingExistingDatabasePreservesCatalogue() = runBlocking {
        repository.ensureSeeded()
        val first = repository.all()

        repository.ensureSeeded()
        val second = repository.all()

        assertEquals(first, second)
        assertEquals(424, second.size)
    }

    @Test
    fun searchesRecipeTitle() = runBlocking {
        repository.ensureSeeded()

        val results = repository.search("Pineapple")

        assertFalse(results.isEmpty())
        assertEquals(1, results.first().sourceRecipeNumber)
    }

    @Test
    fun searchesIngredients() = runBlocking {
        repository.ensureSeeded()

        val results = repository.search("", listOf("pineapple"))

        assertFalse(results.isEmpty())
        assertEquals(1, results.first().sourceRecipeNumber)
    }

    @Test
    fun literalWildcardCharactersDoNotBecomeSearchWildcards() = runBlocking {
        repository.ensureSeeded()

        val results = repository.search("%")

        assertEquals(emptyList<Recipe>(), results)
    }
}
