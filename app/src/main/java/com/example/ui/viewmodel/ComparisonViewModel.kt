package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AIResponse
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ComparisonState(
    val selectedPresetPrompt: String = "What is my project called?",
    val customPromptInput: String = "",
    val isLoading: Boolean = false,
    val normalAiResponse: AIResponse? = null,
    val memoryAiResponse: AIResponse? = null
)

class ComparisonViewModel(
    private val repository: MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComparisonState())
    val uiState: StateFlow<ComparisonState> = _uiState.asStateFlow()

    val presetPrompts = listOf(
        "What is my project called?",
        "What are my programming language preferences?",
        "Who am I and what is my role?",
        "What am I currently researching?"
    )

    init {
        // Run comparison for the first preset by default
        runComparison(presetPrompts.first())
    }

    fun selectPreset(prompt: String) {
        _uiState.value = _uiState.value.copy(
            selectedPresetPrompt = prompt,
            customPromptInput = ""
        )
        runComparison(prompt)
    }

    fun onCustomPromptChanged(input: String) {
        _uiState.value = _uiState.value.copy(customPromptInput = input)
    }

    fun runCustomComparison() {
        val prompt = _uiState.value.customPromptInput.trim()
        if (prompt.isBlank() || _uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(selectedPresetPrompt = prompt)
        runComparison(prompt)
    }

    fun runComparison(prompt: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val (normal, memory) = repository.runComparison(prompt)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    normalAiResponse = normal,
                    memoryAiResponse = memory
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
