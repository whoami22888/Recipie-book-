package com.beyondhuman.kitchen.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import com.beyondhuman.kitchen.network.KitchenApi
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NetworkFlowUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var server: ServerSocket
    private lateinit var thread: Thread
    private lateinit var api: KitchenApi

    @Before
    fun setUp() {
        server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        thread = Thread {
            while (!server.isClosed) {
                try {
                    server.accept().use { socket ->
                        val reader = BufferedReader(
                            InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                        )
                        val headers = buildString {
                            while (true) {
                                val line = reader.readLine() ?: break
                                if (line.isEmpty()) break
                                append(line).append('\n')
                            }
                        }
                        val length = Regex("(?im)^Content-Length: (\\d+)$")
                            .find(headers)?.groupValues?.get(1)?.toInt() ?: 0
                        val body = CharArray(length)
                        var offset = 0
                        while (offset < length) {
                            val read = reader.read(body, offset, length - offset)
                            if (read < 0) break
                            offset += read
                        }
                        val request = String(body, 0, offset)
                        val response = when {
                            request.contains("\"messages\"") ->
                                """{"content":"Make pineapple pie","model":"test-model"}"""
                            request.contains("\"url\"") ->
                                """{"status":"draft","sourceUrl":"https://example.com/recipe","title":"Test Recipe","requiresConfirmation":true,"ingredients":["pineapple"],"method":["Mix"],"provenance":"external-url"}"""
                            else -> """{"error":"unexpected request"}"""
                        }
                        val bytes = response.toByteArray(StandardCharsets.UTF_8)
                        val raw = "HTTP/1.1 200 OK\\r\\nContent-Type: application/json\\r\\nContent-Length: ${bytes.size}\\r\\nConnection: close\\r\\n\\r\\n"
                        socket.getOutputStream().use { output ->
                            output.write(raw.toByteArray(StandardCharsets.UTF_8))
                            output.write(bytes)
                            output.flush()
                        }
                    }
                } catch (error: java.net.SocketException) {
                    if (!server.isClosed) throw error
                }
            }
        }.apply { isDaemon = true }
        thread.start()
        api = KitchenApi("http://127.0.0.1:${server.localPort}")
    }

    @After
    fun tearDown() {
        server.close()
        thread.join(2_000)
    }

    @Test
    fun chatScreenCompletesClientToUiFlow() {
        composeRule.setContent { MaterialTheme { ChatScreen(api = api, onBack = {}) } }
        composeRule.onNodeWithText("What should I cook?").performTextInput("What can I make with pineapple?")
        composeRule.onNodeWithText("Send").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onNodeWithText("Make pineapple pie").isDisplayed()
        }
        composeRule.onNodeWithText("Make pineapple pie").assertIsDisplayed()
    }

    @Test
    fun importScreenCompletesDraftConfirmationFlow() {
        composeRule.setContent { MaterialTheme { ImportScreen(api = api, onBack = {}) } }
        composeRule.onNodeWithText("Recipe URL").performTextInput("https://example.com/recipe")
        composeRule.onNodeWithText("Fetch recipe").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onNodeWithText("Test Recipe").isDisplayed()
        }
        composeRule.onNodeWithText("Test Recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm imported draft").performClick()
        composeRule.onNodeWithText("Import confirmed for review. Persistent saving is intentionally not enabled until the user-data schema is migrated.").assertIsDisplayed()
    }
}
