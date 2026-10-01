package com.beyondhuman.kitchen.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
    fun photoFlowOpensAndConfirmsManualIngredients() {
        composeRule.waitUntil(20_000) {
            composeRule.onNodeWithText("424 recipes").isDisplayed()
        }

        composeRule.onNodeWithText("Find recipes from a photo").performClick()
        composeRule.onNodeWithText("Find recipes from what you have").assertIsDisplayed()
        composeRule.onNodeWithText("Photo mode: fridge").assertIsDisplayed()

        composeRule.onNodeWithText("Photo mode: fridge").performClick()
        composeRule.onNodeWithText("Both").performClick()
        composeRule.onNodeWithText("Photo mode: both").assertIsDisplayed()

        composeRule.onNodeWithText("Add ingredients manually, comma separated")
            .performTextInput("pineapple, flour")
        composeRule.onNodeWithText("Use confirmed ingredients").performClick()

        composeRule.onNodeWithText("Confirmed photo ingredients: pineapple, flour").assertIsDisplayed()
        composeRule.onNodeWithText("424 recipes").assertIsDisplayed()
    }
}
