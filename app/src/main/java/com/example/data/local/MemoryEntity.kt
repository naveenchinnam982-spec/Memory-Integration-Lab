package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an intelligent memory item stored persistently in Room Database.
 */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String, // Personal information, Preferences, Projects, Learning, Important facts, Other
    val importance: Int,  // 1 to 10
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0,
    val isShortTerm: Boolean = false,
    val expirationTimestamp: Long? = null,
    val vectorEmbedding: String = "" // Serialized comma-separated float array
)
