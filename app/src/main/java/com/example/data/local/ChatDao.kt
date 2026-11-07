package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM chat_messages WHERE memoryRetrievalPerformed = 1")
    suspend fun getRetrievedInteractionsCount(): Int

    @Query("SELECT AVG(latencyMs) FROM chat_messages WHERE sender = 'ai' AND latencyMs > 0")
    suspend fun getAverageLatencyMs(): Double?

    @Query("SELECT AVG(highestSimilarityScore) FROM chat_messages WHERE memoryRetrievalPerformed = 1 AND highestSimilarityScore > 0")
    suspend fun getAverageSimilarityScore(): Double?
}
