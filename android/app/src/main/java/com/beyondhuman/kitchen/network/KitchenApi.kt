package com.beyondhuman.kitchen.network

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

class KitchenApi(private val baseUrl: String, private val json: Json = Json { ignoreUnknownKeys = true }) {
    fun chat(messages: List<ChatMessageDto>): ChatResponseDto {
        val encoded = json.encodeToString(ListSerializer(ChatMessageDto.serializer()), messages)
        return request("/v1/ai/chat", "{\"messages\":$encoded}", ChatResponseDto.serializer())
    }

    fun importUrl(url: String): ImportResponseDto {
        val encoded = json.encodeToString(String.serializer(), url)
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

        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            if (status !in 200..299) {
                val detail = parseErrorDetail(response)
                error(detail.ifBlank { "Backend request failed: HTTP $status" })
            }

            return json.decodeFromString(decoder, response)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseErrorDetail(response: String): String {
        if (response.isBlank()) return ""
        return try {
            val parsed = Json.parseToJsonElement(response)
            val detail = parsed.jsonObject["detail"]
            when {
                detail == null -> ""
                detail is kotlinx.serialization.json.JsonPrimitive && detail.isString -> detail.content
                else -> detail.toString()
            }
        } catch (_: Exception) {
            response
        }
    }
}
