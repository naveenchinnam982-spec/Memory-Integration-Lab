package com.example.data.repository

import com.example.data.ai.AIResponse
import com.example.data.ai.ExtractionResult
import com.example.data.ai.GeminiApiClient
import com.example.data.ai.MemoryExtractor
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.data.vector.RetrievedMemoryResult
import com.example.data.vector.SemanticVectorEngine
import com.example.data.remote.ApiClient
import com.example.data.remote.MemoryApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class PipelineExecutionResult(
    val userMessage: ChatMessageEntity,
    val aiMessage: ChatMessageEntity,
    val retrievedMemories: List<RetrievedMemoryResult>,
    val extractionResult: ExtractionResult
)

class MemoryRepository(
    private val memoryDao: MemoryDao,
    private val chatDao: ChatDao,
    private val apiService: MemoryApiService = ApiClient.create()
) {
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    suspend fun checkBackendHealth(): String {
        return try {
            val res = apiService.checkHealth()
            "Connected: ${res.service} v${res.version}"
        } catch (_: Exception) {
            "Offline: Local On-Device Engine Active"
        }
    }

    suspend fun initializeWithDefaultMemoriesIfEmpty() {
        val count = memoryDao.getMemoryCount()
        if (count == 0) {
            val defaults = listOf(
                MemoryEntity(
                    title = "Project: HealthContinuum AI",
                    content = "The user is leading development of HealthContinuum AI, a proactive preventative medical intelligence platform.",
                    category = "Projects",
                    importance = 9,
                    vectorEmbedding = SemanticVectorEngine.serializeVector(
                        SemanticVectorEngine.createEmbedding("Project: HealthContinuum AI The user is leading development of HealthContinuum AI, a proactive preventative medical intelligence platform.")
                    )
                ),
                MemoryEntity(
                    title = "Preferences: Kotlin & Jetpack Compose",
                    content = "User strongly prefers modern Kotlin, Jetpack Compose for Android UI, and FastAPI for backend microservices.",
                    category = "Preferences",
                    importance = 8,
                    vectorEmbedding = SemanticVectorEngine.serializeVector(
                        SemanticVectorEngine.createEmbedding("Preferences: Kotlin & Jetpack Compose User strongly prefers modern Kotlin, Jetpack Compose for Android UI, and FastAPI for backend microservices.")
                    )
                ),
                MemoryEntity(
                    title = "Personal: Senior AI Systems Architect",
                    content = "User works as a Senior AI Systems Architect focused on LLM pipelines, RAG systems, and on-device intelligent agents.",
                    category = "Personal information",
                    importance = 9,
                    vectorEmbedding = SemanticVectorEngine.serializeVector(
                        SemanticVectorEngine.createEmbedding("Personal: Senior AI Systems Architect User works as a Senior AI Systems Architect focused on LLM pipelines, RAG systems, and on-device intelligent agents.")
                    )
                ),
                MemoryEntity(
                    title = "Learning: Vector DBs & Quantized Inference",
                    content = "Currently researching vector embeddings indexing, cosine distance thresholds, and memory deduplication algorithms.",
                    category = "Learning",
                    importance = 7,
                    vectorEmbedding = SemanticVectorEngine.serializeVector(
                        SemanticVectorEngine.createEmbedding("Learning: Vector DBs & Quantized Inference Currently researching vector embeddings indexing, cosine distance thresholds, and memory deduplication algorithms.")
                    )
                )
            )
            memoryDao.insertMemories(defaults)
        }
    }

    suspend fun addMemory(
        title: String,
        content: String,
        category: String,
        importance: Int,
        isShortTerm: Boolean = false,
        expirationTimestamp: Long? = null
    ): Long {
        val embedding = SemanticVectorEngine.createEmbedding("$title $content")
        val entity = MemoryEntity(
            title = title.trim(),
            content = content.trim(),
            category = category,
            importance = importance,
            isShortTerm = isShortTerm,
            expirationTimestamp = expirationTimestamp,
            vectorEmbedding = SemanticVectorEngine.serializeVector(embedding)
        )
        return memoryDao.insertMemory(entity)
    }

    suspend fun updateMemory(
        id: Long,
        title: String,
        content: String,
        category: String,
        importance: Int,
        isShortTerm: Boolean = false,
        expirationTimestamp: Long? = null
    ) {
        val existing = memoryDao.getMemoryById(id) ?: return
        val embedding = SemanticVectorEngine.createEmbedding("$title $content")
        val updated = existing.copy(
            title = title.trim(),
            content = content.trim(),
            category = category,
            importance = importance,
            isShortTerm = isShortTerm,
            expirationTimestamp = expirationTimestamp,
            vectorEmbedding = SemanticVectorEngine.serializeVector(embedding)
        )
        memoryDao.updateMemory(updated)
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun clearAllMemories() {
        memoryDao.clearAllMemories()
    }

    suspend fun clearChatHistory() {
        chatDao.clearHistory()
    }

    /**
     * Executes the Complete 9-Step AI Workflow Pipeline:
     * 1. User Query
     * 2. Query Analysis
     * 3. Memory Retrieval (Semantic Cosine Similarity)
     * 4. Context Construction
     * 5. AI Processing (Gemini API / Synthesizer)
     * 6. Response Generation
     * 7. Memory Evaluation
     * 8. Memory Update
     * 9. Response Delivery & DB Logging
     */
    suspend fun executePipeline(
        userQuery: String,
        isMemoryEnabled: Boolean,
        minSimilarityThreshold: Float = 0.25f
    ): PipelineExecutionResult {
        // STEP 1 & 2: User Query & Query Analysis
        val userEntity = ChatMessageEntity(
            sender = "user",
            text = userQuery.trim(),
            isMemoryEnabledMode = isMemoryEnabled
        )
        val userMsgId = chatDao.insertMessage(userEntity)

        // Fetch current active memories
        val allMemoriesList = memoryDao.getAllMemoriesList()

        // STEP 3: Memory Retrieval via Semantic Similarity
        val retrievedMemories = if (isMemoryEnabled) {
            val results = SemanticVectorEngine.retrieveTopMemories(
                query = userQuery,
                memories = allMemoriesList,
                minThreshold = minSimilarityThreshold,
                topK = 3
            )
            // Record access for retrieved memories
            for (res in results) {
                memoryDao.recordAccess(res.memory.id)
            }
            results
        } else {
            emptyList()
        }

        // STEP 4, 5, 6: Context Construction, AI Processing & Response Generation
        val aiResponse: AIResponse = GeminiApiClient.generateResponse(
            userQuery = userQuery,
            isMemoryEnabled = isMemoryEnabled,
            retrievedMemories = retrievedMemories
        )

        // STEP 7 & 8: Memory Evaluation & Update (Auto-extract new facts if applicable)
        val extraction = MemoryExtractor.evaluateAndExtract(userQuery, allMemoriesList)
        var newlyStoredSnippet: String? = null

        if (extraction.shouldStore) {
            if (extraction.isUpdate && extraction.existingMemoryIdToUpdate != null) {
                updateMemory(
                    id = extraction.existingMemoryIdToUpdate,
                    title = extraction.title,
                    content = extraction.content,
                    category = extraction.category,
                    importance = extraction.importance
                )
                newlyStoredSnippet = "Updated existing memory #${extraction.existingMemoryIdToUpdate}: ${extraction.title}"
            } else {
                val newId = addMemory(
                    title = extraction.title,
                    content = extraction.content,
                    category = extraction.category,
                    importance = extraction.importance
                )
                newlyStoredSnippet = "Stored new memory #$newId: ${extraction.title}"
            }
        }

        // STEP 9: Response Delivery and Chat Logging with Transparency Audit
        val highestScore = retrievedMemories.maxOfOrNull { it.similarityScore } ?: 0f
        val summary = retrievedMemories.joinToString("; ") {
            "#${it.memory.id} (${(it.similarityScore * 100).toInt()}%): ${it.memory.title}"
        }

        val aiEntity = ChatMessageEntity(
            sender = "ai",
            text = aiResponse.replyText,
            memoryRetrievalPerformed = isMemoryEnabled && retrievedMemories.isNotEmpty(),
            retrievedMemoryCount = retrievedMemories.size,
            retrievedMemorySummary = summary,
            highestSimilarityScore = highestScore,
            isMemoryEnabledMode = isMemoryEnabled,
            contextTokensUsed = aiResponse.contextTokensUsed,
            latencyMs = aiResponse.latencyMs,
            newlySavedMemoryContent = newlyStoredSnippet
        )
        val aiMsgId = chatDao.insertMessage(aiEntity)

        return PipelineExecutionResult(
            userMessage = userEntity.copy(id = userMsgId),
            aiMessage = aiEntity.copy(id = aiMsgId),
            retrievedMemories = retrievedMemories,
            extractionResult = extraction
        )
    }

    /**
     * Executes side-by-side comparison for a given prompt:
     * Generates Normal AI response (zero long-term memory access) vs.
     * Memory-Enabled AI response (semantic retrieval access).
     */
    suspend fun runComparison(
        prompt: String
    ): Pair<AIResponse, AIResponse> {
        val memories = memoryDao.getAllMemoriesList()
        val retrieved = SemanticVectorEngine.retrieveTopMemories(prompt, memories, minThreshold = 0.25f, topK = 3)

        // Normal AI: no memory injection
        val normalAi = GeminiApiClient.generateResponse(
            userQuery = prompt,
            isMemoryEnabled = false,
            retrievedMemories = emptyList()
        )

        // Memory-Enabled AI: retrieved memories injected
        val memoryAi = GeminiApiClient.generateResponse(
            userQuery = prompt,
            isMemoryEnabled = true,
            retrievedMemories = retrieved
        )

        return normalAi to memoryAi
    }

    // Telemetry and statistics
    suspend fun getRetrievedInteractionsCount(): Int = chatDao.getRetrievedInteractionsCount()
    suspend fun getAverageLatency(): Double = chatDao.getAverageLatencyMs() ?: 240.0
    suspend fun getAverageSimilarity(): Double = chatDao.getAverageSimilarityScore() ?: 0.72
}
