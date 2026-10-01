package com.beyondhuman.kitchen.network

import kotlinx.serialization.Serializable

@Serializable data class ChatMessageDto(val role: String, val content: String)
@Serializable data class ChatResponseDto(val content: String, val model: String = "")
@Serializable data class ImportResponseDto(
    val status: String,
    val sourceUrl: String,
    val title: String = "",
    val requiresConfirmation: Boolean,
    val ingredients: List<String> = emptyList(),
    val method: List<String> = emptyList(),
    val provenance: String = ""
)
