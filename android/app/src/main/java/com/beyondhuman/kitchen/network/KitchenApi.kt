package com.beyondhuman.kitchen.network

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI

class KitchenApi(private val baseUrl: String, private val json: Json = Json { ignoreUnknownKeys = true }) {
    fun chat(messages: List<ChatMessageDto>): ChatResponseDto {
        val encoded = json.encodeToString(messages)
        return request("/v1/ai/chat", "{\"messages\":$encoded}", ChatResponseDto.serializer())
    }

    fun importUrl(url: String): ImportResponseDto {
        val encoded = json.encodeToString(url)
        return request("/v1/import/url", "{\"url\":$encoded}", ImportResponseDto.serializer())
    }

    private fun <T> request(path: String, body: String, decoder: DeserializationStrategy<T>): T {
        val connection = URI(baseUrl.trimEnd('/') + path).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 10_000
        connection.readTimeout = 45_000
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) error("Backend request failed: HTTP $status")
        return json.decodeFromString(decoder, response)
    }
}
