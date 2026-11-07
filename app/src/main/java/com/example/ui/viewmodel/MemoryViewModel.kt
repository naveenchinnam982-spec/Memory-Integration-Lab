package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MemoryEntity
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MemoryUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val isAddEditOpen: Boolean = false,
    val memoryUnderEdit: MemoryEntity? = null,
    val memoryToDelete: MemoryEntity? = null,
    val detailedMemoryView: MemoryEntity? = null
)

class MemoryViewModel(
    private val repository: MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeWithDefaultMemoriesIfEmpty()
        }
    }

    val memories: StateFlow<List<MemoryEntity>> = combine(
        repository.allMemories,
        _uiState
    ) { all, state ->
        all.filter { mem ->
            val matchesQuery = state.searchQuery.isBlank() ||
                mem.title.contains(state.searchQuery, ignoreCase = true) ||
                mem.content.contains(state.searchQuery, ignoreCase = true)
            val matchesCategory = state.selectedCategory == null || mem.category == state.selectedCategory
            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onCategorySelected(category: String?) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = if (_uiState.value.selectedCategory == category) null else category
        )
    }

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(isAddEditOpen = true, memoryUnderEdit = null)
    }

    fun openEditDialog(memory: MemoryEntity) {
        _uiState.value = _uiState.value.copy(isAddEditOpen = true, memoryUnderEdit = memory)
    }

    fun closeAddEditDialog() {
        _uiState.value = _uiState.value.copy(isAddEditOpen = false, memoryUnderEdit = null)
    }

    fun openDetails(memory: MemoryEntity) {
        _uiState.value = _uiState.value.copy(detailedMemoryView = memory)
    }

    fun closeDetails() {
        _uiState.value = _uiState.value.copy(detailedMemoryView = null)
    }

    fun confirmDelete(memory: MemoryEntity) {
        _uiState.value = _uiState.value.copy(memoryToDelete = memory)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(memoryToDelete = null)
    }

    fun executeDelete() {
        val target = _uiState.value.memoryToDelete ?: return
        viewModelScope.launch {
            repository.deleteMemory(target.id)
            _uiState.value = _uiState.value.copy(memoryToDelete = null)
        }
    }

    fun saveMemory(
        id: Long?,
        title: String,
        content: String,
        category: String,
        importance: Int,
        isShortTerm: Boolean = false,
        expirationTimestamp: Long? = null
    ) {
        viewModelScope.launch {
            if (id == null || id == 0L) {
                repository.addMemory(title, content, category, importance, isShortTerm, expirationTimestamp)
            } else {
                repository.updateMemory(id, title, content, category, importance, isShortTerm, expirationTimestamp)
            }
            closeAddEditDialog()
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
        }
    }
}
