package com.beyondhuman.kitchen.ui

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    private fun waitForText(device: UiDevice, text: String, timeoutMs: Long = 15_000): Boolean {
        return device.wait(Until.findObject(By.text(text)), timeoutMs) != null
    }

    @Test
    fun launchesAndShowsCatalogue() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertTrue(waitForText(device, "Beyond Human Kitchen"))
            assertTrue(waitForText(device, "424 recipes"))
            assertTrue(waitForText(device, "1. No-Bake Pineapple Pie"))
        }
    }

    @Test
    fun shareTextStartsSearchWithSharedContent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Pineapple")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        ActivityScenario.launch<MainActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertTrue(waitForText(device, "Beyond Human Kitchen"))
            assertNotNull(device.wait(Until.findObject(By.text("Pineapple")), 15_000))
            assertTrue(waitForText(device, "1. No-Bake Pineapple Pie"))
        }
    }
}
