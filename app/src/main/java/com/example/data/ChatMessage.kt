package com.example.data

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPending: Boolean = false,
    val isError: Boolean = false,
    val showCreatorProfile: Boolean = false
)

enum class MessageSender {
    USER,
    AI
}
