package com.beyondhuman.kitchen.network

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KitchenApiContractTest {
    private lateinit var server: HttpServer
    private lateinit var api: KitchenApi

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/ai/chat") { exchange ->
            val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
            assertTrue(body.contains("\"role\":\"user\""))
            assertTrue(body.contains("\"content\":\"What should I cook?\""))
            respond(exchange, 200, """{"content":"Make pineapple pie","model":"test-model"}""")
        }
        server.createContext("/v1/import/url") { exchange ->
            val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
            assertTrue(body.contains("\"url\":\"https://example.com/recipe\""))
            respond(exchange, 200, """{"status":"draft","sourceUrl":"https://example.com/recipe","title":"Test Recipe","requiresConfirmation":true,"ingredients":["pineapple"],"method":["Mix"],"provenance":"external-url"}""")
        }
        server.start()
        api = KitchenApi("http://127.0.0.1:" + server.address.port)
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun chatMatchesBackendContract() {
        val response = api.chat(listOf(ChatMessageDto("user", "What should I cook?")))
        assertEquals("Make pineapple pie", response.content)
        assertEquals("test-model", response.model)
    }

    @Test
    fun urlImportMatchesBackendContract() {
        val response = api.importUrl("https://example.com/recipe")
        assertEquals("draft", response.status)
        assertTrue(response.requiresConfirmation)
        assertEquals("Test Recipe", response.title)
        assertEquals(listOf("pineapple"), response.ingredients)
        assertEquals(listOf("Mix"), response.method)
        assertEquals("external-url", response.provenance)
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
