package com.beyondhuman.kitchen.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesAndShowsCatalogue() {
        composeRule.waitUntil(20_000) {
            composeRule.onAllNodesWithText("424 recipes").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Beyond Human Kitchen").assertIsDisplayed()
        composeRule.onNodeWithText("424 recipes").assertIsDisplayed()
        composeRule.onNodeWithText("1. No-Bake Pineapple Pie").assertIsDisplayed()
    }

    @Test
    fun shareIntentDeliversTextToActivity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Pineapple")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Pineapple", extractSharedText(activity.intent))
            }
        }
    }

    @Test
    fun sharedTextIsUsedAsInitialSearch() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.intent = Intent(activity, MainActivity::class.java).apply {
                putExtra(Intent.EXTRA_TEXT, "Pineapple")
            }
        }
        composeRule.setContent {
            KitchenApp(
                repo = com.beyondhuman.kitchen.data.RecipeRepository(composeRule.activity),
                sharedText = "Pineapple",
                sharedUris = emptyList()
            )
        }

        composeRule.waitUntil(20_000) {
            composeRule.onAllNodesWithText("1. No-Bake Pineapple Pie").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("1. No-Bake Pineapple Pie").assertIsDisplayed()
    }
}
