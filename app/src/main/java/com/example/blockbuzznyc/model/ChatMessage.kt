package com.example.blockbuzznyc.model

data class ChatMessage(
    val senderId: String = "",
    val username: String = "", // Add this line
    val message: String = "",
    val timestamp: Long = 0L
)

