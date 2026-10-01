package com.beyondhuman.kitchen.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.beyondhuman.kitchen.data.Recipe
import com.beyondhuman.kitchen.data.RecipeRepository
import com.beyondhuman.kitchen.network.ChatMessageDto
import com.beyondhuman.kitchen.network.ImportResponseDto
import com.beyondhuman.kitchen.network.KitchenApi
import com.beyondhuman.kitchen.network.NetworkConfig
import com.beyondhuman.kitchen.vision.IngredientAnalyzer
import com.beyondhuman.kitchen.vision.IngredientCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

internal fun extractSharedText(intent: Intent): String = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()

private fun extractSharedUris(intent: Intent): List<Uri> = buildList {
    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::add)
    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(::addAll)
}

@Composable
fun KitchenApp(repo: RecipeRepository, sharedText: String, sharedUris: List<Uri>) {
    var query by remember { mutableStateOf(sharedText) }
    var pantry by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Recipe?>(null) }
    var allRecipes by remember { mutableStateOf(emptyList<Recipe>()) }
    var results by remember { mutableStateOf(emptyList<Recipe>()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showVision by remember { mutableStateOf(sharedUris.isNotEmpty()) }
    var confirmedVisionIngredients by remember { mutableStateOf(emptyList<String>()) }
    var networkScreen by remember { mutableStateOf<String?>(null) }
    val networkApi = remember { KitchenApi(NetworkConfig.BASE_URL) }

    LaunchedEffect(Unit) { allRecipes = repo.all() }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when {
                showVision -> VisionIngredientScreen(
                    initialUris = sharedUris,
                    onConfirmed = { ingredients ->
                        confirmedVisionIngredients = ingredients
                        pantry = ingredients.joinToString(",")
                        showVision = false
                    },
                    onCancel = { showVision = false }
                )
                networkScreen == "chat" -> ChatScreen(api = networkApi, onBack = { networkScreen = null })
                networkScreen == "import" -> ImportScreen(api = networkApi, onBack = { networkScreen = null })
                selected != null -> RecipeScreen(selected!!) { selected = null }
                else -> Column(Modifier.padding(16.dp)) {
                    Text("Beyond Human Kitchen", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search recipes, ingredients or categories") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = pantry, onValueChange = { pantry = it }, modifier = Modifier.fillMaxWidth(), label = { Text("What do I have? (comma separated)") })
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { showVision = true }) { Text("Find recipes from a photo") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { networkScreen = "chat" }) { Text("Ask AI what to cook") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { networkScreen = "import" }) { Text("Import recipe from URL") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { categoryMenuExpanded = true }) { Text(selectedSection ?: "All categories") }
                    DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("All categories") }, onClick = { selectedSection = null; categoryMenuExpanded = false })
                        categoryOptions(allRecipes).forEach { section ->
                            DropdownMenuItem(text = { Text(section) }, onClick = { selectedSection = section; categoryMenuExpanded = false })
                        }
                    }
                    if (confirmedVisionIngredients.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Confirmed photo ingredients: " + confirmedVisionIngredients.joinToString(", "))
                    }
                    LaunchedEffect(query, pantry, selectedSection) {
                        results = filterRecipesBySection(repo.search(query = query, ingredients = pantry.split(',')), selectedSection)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${results.size} recipes", style = MaterialTheme.typography.labelLarge)
                    LazyColumn {
                        items(results, key = { it.id }) { recipe ->
                            ListItem(
                                headlineContent = { Text("${recipe.sourceRecipeNumber}. ${recipe.title}") },
                                supportingContent = { Text(listOf(recipe.section, *recipe.shoppingList.take(2).toTypedArray()).filter(String::isNotBlank).joinToString(" • ")) },
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

fun categoryOptions(recipes: List<Recipe>): List<String> = recipes.map { it.section.trim() }.filter(String::isNotBlank).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

fun filterRecipesBySection(recipes: List<Recipe>, section: String?): List<Recipe> =
    section?.let { selected -> recipes.filter { it.section.equals(selected, ignoreCase = true) } } ?: recipes

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

@Composable
private fun VisionIngredientScreen(initialUris: List<Uri>, onConfirmed: (List<String>) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val analyzer = remember { IngredientAnalyzer() }
    var candidates by remember { mutableStateOf(emptyList<IngredientCandidate>()) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var manual by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(if (initialUris.isEmpty()) "Take a photo of your fridge, cupboard, or both." else "Analysing shared image(s)…") }
    var captureMode by remember { mutableStateOf("fridge") }
    var capturedCount by remember { mutableStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        status = if (granted) "Camera permission granted. Take a photo." else "Camera permission denied. You can still use shared images or add ingredients manually."
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap == null) status = "Camera capture cancelled."
        else {
            capturedCount += 1
            analyzer.analyzeBitmap(bitmap, onSuccess = { found ->
                candidates = (candidates + found).distinctBy { it.label.lowercase() }.sortedByDescending { it.confidence }
                selected = selected + found.map { it.label }.toSet()
                status = if (captureMode == "both" && capturedCount < 2) "Fridge captured. Capture the cupboard next." else "Review and confirm the detected ingredients."
            }, onFailure = { status = "Image analysis failed. Please try another image." })
        }
    }
    LaunchedEffect(initialUris) {
        initialUris.forEach { uri ->
            analyzer.analyze(context, uri, onSuccess = { found ->
                candidates = (candidates + found).distinctBy { it.label.lowercase() }.sortedByDescending { it.confidence }
                selected = selected + found.map { it.label }.toSet()
                status = "Review and confirm the detected ingredients."
            }, onFailure = { status = "Image analysis failed. Please try another image." })
        }
    }
    Column(Modifier.padding(16.dp)) {
        Text("Find recipes from what you have", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(status)
        Spacer(Modifier.height(12.dp))
        PhotoModeMenu(captureMode) { captureMode = it }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                launcher.launch(null)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }) { Text(if (captureMode == "both" && capturedCount == 1) "Capture cupboard" else "Take photo") }
        Spacer(Modifier.height(12.dp))
        Text("Detected labels require your confirmation.", style = MaterialTheme.typography.labelLarge)
        candidates.forEach { candidate ->
            Row {
                Checkbox(checked = candidate.label in selected, onCheckedChange = { checked -> selected = if (checked) selected + candidate.label else selected - candidate.label })
                Text(candidate.label + " (" + (candidate.confidence * 100).toInt() + "%)", modifier = Modifier.padding(top = 12.dp))
            }
        }
        OutlinedTextField(value = manual, onValueChange = { manual = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Add ingredients manually, comma separated") })
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val manualItems = manual.split(',').map(String::trim).filter(String::isNotBlank)
            onConfirmed((selected + manualItems).toList().distinct())
        }) { Text("Use confirmed ingredients") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun PhotoModeMenu(value: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Button(onClick = { expanded = true }) { Text("Photo mode: " + value) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("fridge", "cupboard", "both").forEach { mode ->
                DropdownMenuItem(text = { Text(mode.replaceFirstChar(Char::uppercase)) }, onClick = { expanded = false; onSelected(mode) })
            }
        }
    }
}

@Composable
internal fun ChatScreen(api: KitchenApi, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessageDto>()) }
    var status by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        Button(onClick = onBack) { Text("Back") }
        Text("AI food assistant", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        LazyColumn { items(messages) { message -> Text(message.role + ": " + message.content, modifier = Modifier.padding(vertical = 4.dp)) } }
        OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("What should I cook?") })
        Spacer(Modifier.height(8.dp))
        Button(enabled = input.isNotBlank(), onClick = {
            val userMessage = input.trim()
            input = ""
            val updatedMessages = messages + ChatMessageDto("user", userMessage)
            messages = updatedMessages
            status = "Asking AI…"
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { api.chat(updatedMessages) } }
                    .onSuccess { response -> messages = messages + ChatMessageDto("assistant", response.content); status = "" }
                    .onFailure { status = "AI unavailable. Check the backend connection and try again." }
            }
        }) { Text("Send") }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun ImportScreen(api: KitchenApi, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf<ImportResponseDto?>(null) }
    var confirmed by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp)) {
        Button(onClick = onBack) { Text("Back") }
        Text("Import recipe", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Recipe URL") })
        Spacer(Modifier.height(8.dp))
        Button(enabled = url.isNotBlank(), onClick = {
            status = "Importing…"
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { api.importUrl(url.trim()) } }
                    .onSuccess { response -> draft = response; confirmed = false; status = if (response.requiresConfirmation) "Draft received. Review it before accepting." else "Import complete." }
                    .onFailure { draft = null; status = "Import failed. Check the URL and backend connection." }
            }
        }) { Text("Fetch recipe") }
        if (status.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(status) }
        draft?.let { item ->
            Spacer(Modifier.height(12.dp))
            Text(item.title, style = MaterialTheme.typography.titleLarge)
            Text("Source: " + item.sourceUrl)
            Text("Provenance: " + item.provenance)
            Text("Ingredients: " + item.ingredients.joinToString(", "))
            Text("Method steps: " + item.method.size)
            Spacer(Modifier.height(8.dp))
            if (!confirmed) Button(onClick = { confirmed = true }) { Text("Confirm imported draft") }
            else Text("Import confirmed for review. Persistent saving is intentionally not enabled until the user-data schema is migrated.")
        }
    }
}
