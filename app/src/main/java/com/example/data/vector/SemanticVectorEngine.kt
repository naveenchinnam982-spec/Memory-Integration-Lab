package com.example.data.vector

import com.example.data.local.MemoryEntity
import kotlin.math.sqrt

/**
 * Result of a semantic memory retrieval operation.
 */
data class RetrievedMemoryResult(
    val memory: MemoryEntity,
    val similarityScore: Float,
    val rank: Int
)

/**
 * High-performance on-device Semantic Vector Engine.
 * Implements dense subword & term-frequency semantic vector hashing,
 * L2 normalization, and cosine similarity ranking.
 */
object SemanticVectorEngine {
    private const val VECTOR_DIM = 96

    // Domain keywords that get higher semantic weight
    private val SEMANTIC_ANCHORS = listOf(
        "project", "name", "called", "healthcontinuum", "kotlin", "compose", "fastapi",
        "work", "prefer", "preference", "like", "favorite", "study", "learning", "facts",
        "user", "engineer", "role", "system", "architecture", "goal", "timeline", "important",
        "database", "vector", "ai", "gemini", "embedding", "model", "python", "developer"
    )

    /**
     * Converts a text string into an L2-normalized dense vector embedding.
     */
    fun createEmbedding(text: String): FloatArray {
        val vector = FloatArray(VECTOR_DIM)
        if (text.isBlank()) return vector

        val normalized = text.lowercase().trim()
        val tokens = normalized.split(Regex("[\\s,.;:!?\\-\"'\\[\\]()]+")).filter { it.isNotBlank() }

        // 1. Word token contribution with semantic anchoring
        for (token in tokens) {
            val hash = (token.hashCode() and 0x7fffffff) % VECTOR_DIM
            val anchorBonus = if (SEMANTIC_ANCHORS.any { token.contains(it) || it.contains(token) }) 2.5f else 1.0f
            vector[hash] += 1.0f * anchorBonus

            // Secondary hash for dispersion
            val secondaryHash = ((token.reversed().hashCode() and 0x7fffffff) + 13) % VECTOR_DIM
            vector[secondaryHash] += 0.5f * anchorBonus
        }

        // 2. Character 3-gram contribution to capture morphological similarity
        if (normalized.length >= 3) {
            for (i in 0..normalized.length - 3) {
                val triGram = normalized.substring(i, i + 3)
                val triHash = (triGram.hashCode() and 0x7fffffff) % VECTOR_DIM
                vector[triHash] += 0.35f
            }
        }

        // 3. L2 Normalization
        var sumSquares = 0.0
        for (value in vector) {
            sumSquares += value * value
        }
        val norm = sqrt(sumSquares).toFloat()

        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }

        return vector
    }

    /**
     * Calculates cosine similarity between two normalized vectors: dot product.
     */
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0f
        var dot = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
        }
        return dot.coerceIn(0f, 1f)
    }

    /**
     * Serializes a float array to a comma-separated string for SQLite/Room.
     */
    fun serializeVector(vector: FloatArray): String {
        return vector.joinToString(",") { "%.4f".format(it) }
    }

    /**
     * Deserializes a comma-separated string back to a float array.
     */
    fun deserializeVector(encoded: String): FloatArray {
        if (encoded.isBlank()) return FloatArray(0)
        return try {
            encoded.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
        } catch (_: Exception) {
            FloatArray(0)
        }
    }

    /**
     * Performs semantic similarity search against stored memories.
     * Ranks memories, filters by similarity threshold, and returns top K.
     */
    fun retrieveTopMemories(
        query: String,
        memories: List<MemoryEntity>,
        minThreshold: Float = 0.25f,
        topK: Int = 3
    ): List<RetrievedMemoryResult> {
        if (query.isBlank() || memories.isEmpty()) return emptyList()

        val queryVector = createEmbedding(query)
        val scoredList = mutableListOf<Pair<MemoryEntity, Float>>()

        for (memory in memories) {
            val memoryVector = if (memory.vectorEmbedding.isNotBlank()) {
                val cached = deserializeVector(memory.vectorEmbedding)
                if (cached.size == VECTOR_DIM) cached else createEmbedding("${memory.title} ${memory.content}")
            } else {
                createEmbedding("${memory.title} ${memory.content}")
            }

            val similarity = cosineSimilarity(queryVector, memoryVector)

            // Direct keyword matching boost for exact acronyms/names like "HealthContinuum AI"
            val queryLower = query.lowercase()
            val contentLower = "${memory.title} ${memory.content}".lowercase()
            var boost = 0f
            if (queryLower.contains("project") && contentLower.contains("project")) boost += 0.15f
            if (queryLower.contains("prefer") && contentLower.contains("prefer")) boost += 0.15f
            if (queryLower.contains("name") && contentLower.contains("name")) boost += 0.12f

            val finalScore = (similarity + boost).coerceIn(0f, 1f)
            if (finalScore >= minThreshold) {
                scoredList.add(memory to finalScore)
            }
        }

        return scoredList
            .sortedByDescending { it.second }
            .take(topK)
            .mapIndexed { index, pair ->
                RetrievedMemoryResult(
                    memory = pair.first,
                    similarityScore = pair.second,
                    rank = index + 1
                )
            }
    }
}
