package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a chat message along with its memory retrieval audit trail and metrics.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "user" or "ai" or "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val memoryRetrievalPerformed: Boolean = false,
    val retrievedMemoryCount: Int = 0,
    val retrievedMemorySummary: String = "",
    val highestSimilarityScore: Float = 0f,
    val isMemoryEnabledMode: Boolean = true,
    val contextTokensUsed: Int = 0,
    val latencyMs: Long = 0L,
    val newlySavedMemoryContent: String? = null
)
