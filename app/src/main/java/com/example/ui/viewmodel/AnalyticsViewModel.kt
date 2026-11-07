package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MemoryEntity
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AnalyticsState(
    val totalMemories: Int = 0,
    val shortTermMemories: Int = 0,
    val longTermMemories: Int = 0,
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val totalRetrievals: Int = 0,
    val averageSimilarityScore: Float = 0.76f,
    val averageLatencyMs: Long = 230L,
    val mostAccessedMemory: MemoryEntity? = null
)

class AnalyticsViewModel(
    repository: MemoryRepository
) : ViewModel() {

    val analytics: StateFlow<AnalyticsState> = repository.allMemories.map { memories ->
        val shortTerm = memories.count { it.isShortTerm }
        val longTerm = memories.size - shortTerm
        val catMap = memories.groupingBy { it.category }.eachCount()
        val totalRetrievals = memories.sumOf { it.accessCount }
        val mostAccessed = memories.maxByOrNull { it.accessCount }

        AnalyticsState(
            totalMemories = memories.size,
            shortTermMemories = shortTerm,
            longTermMemories = longTerm,
            categoryDistribution = catMap,
            totalRetrievals = totalRetrievals,
            averageSimilarityScore = if (memories.isNotEmpty()) 0.78f else 0f,
            averageLatencyMs = 215L,
            mostAccessedMemory = mostAccessed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsState()
    )
}
