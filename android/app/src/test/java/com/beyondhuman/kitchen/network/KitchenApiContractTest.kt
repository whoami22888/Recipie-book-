package com.beyondhuman.kitchen.network

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KitchenApiContractTest {
    private lateinit var server: ServerSocket
    private lateinit var api: KitchenApi
    private lateinit var serverThread: Thread

    @Before
    fun setUp() {
        server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        serverThread = Thread {
            repeat(2) {
                server.accept().use { socket ->
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                    val headers = buildString {
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            append(line).append('\n')
                        }
                    }
                    val contentLength = Regex("(?im)^Content-Length: (\\d+)$").find(headers)?.groupValues?.get(1)?.toInt() ?: 0
                    val body = CharArray(contentLength)
                    var offset = 0
                    while (offset < contentLength) {
                        val read = reader.read(body, offset, contentLength - offset)
                        if (read < 0) break
                        offset += read
                    }
                    val requestBody = String(body, 0, offset)
                    val response = when {
                        requestBody.contains("\"messages\"") -> """{"content":"Make pineapple pie","model":"test-model"}"""
                        requestBody.contains("\"url\"") -> """{"status":"draft","sourceUrl":"https://example.com/recipe","title":"Test Recipe","requiresConfirmation":true,"ingredients":["pineapple"],"method":["Mix"],"provenance":"external-url"}"""
                        else -> """{"error":"unexpected request"}"""
                    }
                    val bytes = response.toByteArray(StandardCharsets.UTF_8)
                    val raw = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + bytes.size + "\r\nConnection: close\r\n\r\n"
                    socket.getOutputStream().use { output ->
                        output.write(raw.toByteArray(StandardCharsets.UTF_8))
                        output.write(bytes)
                        output.flush()
                    }
                }
            }
        }.apply { isDaemon = true }
        serverThread.start()
        api = KitchenApi("http://127.0.0.1:" + server.localPort)
    }

    @After
    fun tearDown() {
        server.close()
        serverThread.join(2_000)
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
}
