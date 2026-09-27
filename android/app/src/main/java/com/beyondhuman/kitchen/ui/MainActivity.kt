package com.beyondhuman.kitchen.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.beyondhuman.kitchen.data.Recipe
import com.beyondhuman.kitchen.data.RecipeRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = RecipeRepository(this)
        lifecycleScope.launch {
            runCatching { repo.ensureSeeded() }
                .onSuccess { setContent { KitchenApp(repo, extractSharedText(intent), extractSharedUris(intent)) } }
                .onFailure { error -> setContent { StartupErrorScreen(error.message ?: "Unable to initialise recipe catalogue") } }
        }
    }
}

private fun extractSharedText(intent: Intent): String = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()

private fun extractSharedUris(intent: Intent): List<Uri> = buildList {
    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::add)
    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(::addAll)
}

@Composable
fun KitchenApp(repo: RecipeRepository, sharedText: String, sharedUris: List<Uri>) {
    var query by remember { mutableStateOf(sharedText) }
    var pantry by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Recipe?>(null) }
    var results by remember { mutableStateOf(emptyList<Recipe>()) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            if (selected != null) {
                RecipeScreen(selected!!) { selected = null }
            } else {
                Column(Modifier.padding(16.dp)) {
                    Text("Beyond Human Kitchen", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search recipes, ingredients or categories") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pantry,
                        onValueChange = { pantry = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("What do I have? (comma separated)") }
                    )
                    if (sharedUris.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("${sharedUris.size} shared image(s) received. Image analysis is not enabled until the vision workflow is connected.")
                    }
                    LaunchedEffect(query, pantry) {
                        results = repo.search(query = query, ingredients = pantry.split(','))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${results.size} recipes", style = MaterialTheme.typography.labelLarge)
                    LazyColumn {
                        items(results, key = { it.id }) { recipe ->
                            ListItem(
                                headlineContent = { Text("${recipe.sourceRecipeNumber}. ${recipe.title}") },
                                supportingContent = {
                                    Text(
                                        listOf(recipe.section, *recipe.shoppingList.take(2).toTypedArray())
                                            .filter(String::isNotBlank)
                                            .joinToString(" • ")
                                    )
                                },
                                modifier = Modifier.clickable { selected = recipe }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartupErrorScreen(message: String) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(24.dp)) {
                Text("Recipe catalogue failed to initialise", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text(message)
            }
        }
    }
}

@Composable
fun RecipeScreen(recipe: Recipe, onBack: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Button(onClick = onBack) { Text("Back") }
        Text(recipe.title, style = MaterialTheme.typography.headlineSmall)
        if (recipe.section.isNotBlank()) Text(recipe.section, style = MaterialTheme.typography.labelLarge)
        if (recipe.description.isNotBlank()) Text(recipe.description)
        Spacer(Modifier.height(12.dp))
        Text("Shopping list", style = MaterialTheme.typography.titleMedium)
        recipe.shoppingList.forEach { Text("• $it") }
        Spacer(Modifier.height(12.dp))
        Text("Method", style = MaterialTheme.typography.titleMedium)
        recipe.method.forEachIndexed { index, step -> Text("${index + 1}. $step") }
    }
}
