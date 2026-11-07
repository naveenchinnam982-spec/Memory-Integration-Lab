package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageEntity
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val isMemoryEnabled: Boolean = true,
    val isGenerating: Boolean = false,
    val streamingText: String = "",
    val activeQueryInput: String = "",
    val selectedMessageForAudit: ChatMessageEntity? = null,
    val newlyLearnedBanner: String? = null
)

class ChatViewModel(
    private val repository: MemoryRepository
) : ViewModel() {

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeWithDefaultMemoriesIfEmpty()
        }
    }

    fun onQueryInputChange(input: String) {
        _uiState.value = _uiState.value.copy(activeQueryInput = input)
    }

    fun toggleMemoryEnabled() {
        _uiState.value = _uiState.value.copy(isMemoryEnabled = !_uiState.value.isMemoryEnabled)
    }

    fun selectMessageForAudit(message: ChatMessageEntity?) {
        _uiState.value = _uiState.value.copy(selectedMessageForAudit = message)
    }

    fun dismissLearnedBanner() {
        _uiState.value = _uiState.value.copy(newlyLearnedBanner = null)
    }

    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || _uiState.value.isGenerating) return

        _uiState.value = _uiState.value.copy(
            isGenerating = true,
            activeQueryInput = "",
            streamingText = "Processing Query Analysis & Memory Retrieval..."
        )

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(streamingText = "Step 2: Analyzing Query Intent...")
                kotlinx.coroutines.delay(180)
                _uiState.value = _uiState.value.copy(streamingText = "Step 3: Searching Semantic Vectors (Cosine Similarity)...")
                kotlinx.coroutines.delay(180)
                _uiState.value = _uiState.value.copy(streamingText = "Step 4 & 5: Context Integration & AI Processing...")

                val result = repository.executePipeline(
                    userQuery = trimmed,
                    isMemoryEnabled = _uiState.value.isMemoryEnabled
                )

                val banner = if (result.extractionResult.shouldStore) {
                    if (result.extractionResult.isUpdate) {
                        "Updated Memory: ${result.extractionResult.title}"
                    } else {
                        "Learned New Memory: ${result.extractionResult.title}"
                    }
                } else null

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    streamingText = "",
                    newlyLearnedBanner = banner
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    streamingText = ""
                )
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            _uiState.value = _uiState.value.copy(selectedMessageForAudit = null, newlyLearnedBanner = null)
        }
    }

    fun startNewConversation() {
        clearChat()
    }
}
