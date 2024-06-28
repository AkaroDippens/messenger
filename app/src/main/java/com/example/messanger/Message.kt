package com.example.messanger

data class Message(
    val id: String = "",
    val text: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val timestamp: Long = 0,
    val isDeletedForCurrentUser: Boolean = false,
    val deletedFor: List<String> = emptyList()
)