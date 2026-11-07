package com.example

import com.example.data.ai.MemoryExtractor
import com.example.data.local.MemoryEntity
import com.example.data.vector.SemanticVectorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testVectorEmbeddingAndCosineSimilarity() {
        val v1 = SemanticVectorEngine.createEmbedding("Project HealthContinuum AI")
        val v2 = SemanticVectorEngine.createEmbedding("HealthContinuum AI project details")
        val v3 = SemanticVectorEngine.createEmbedding("Cooking pasta recipes dinner")

        val simRelated = SemanticVectorEngine.cosineSimilarity(v1, v2)
        val simUnrelated = SemanticVectorEngine.cosineSimilarity(v1, v3)

        assertTrue("Related texts should have higher similarity", simRelated > simUnrelated)
    }

    @Test
    fun testMemoryExtraction() {
        val userQuery = "My project is called HealthContinuum AI."
        val result = MemoryExtractor.evaluateAndExtract(userQuery, emptyList())

        assertTrue("Should extract project memory", result.shouldStore)
        assertEquals("Projects", result.category)
        assertTrue("Title should contain project name", result.title.contains("HealthContinuum AI"))
    }

    @Test
    fun testSemanticMemoryRetrieval() {
        val memory = MemoryEntity(
            id = 1,
            title = "Project: HealthContinuum AI",
            content = "User leads HealthContinuum AI medical platform",
            category = "Projects",
            importance = 9
        )

        val retrieved = SemanticVectorEngine.retrieveTopMemories(
            query = "What is my project called?",
            memories = listOf(memory),
            minThreshold = 0.20f
        )

        assertEquals(1, retrieved.size)
        assertEquals(1L, retrieved.first().memory.id)
        assertTrue(retrieved.first().similarityScore >= 0.20f)
    }
}
