package com.example.data.ai

import com.example.data.local.MemoryEntity
import com.example.data.vector.SemanticVectorEngine

data class ExtractionResult(
    val shouldStore: Boolean,
    val title: String = "",
    val content: String = "",
    val category: String = "Other",
    val importance: Int = 5,
    val isUpdate: Boolean = false,
    val existingMemoryIdToUpdate: Long? = null
)

object MemoryExtractor {

    /**
     * Inspects a user message to extract actionable facts, preferences, project details,
     * or personal information.
     */
    fun evaluateAndExtract(
        userMessage: String,
        existingMemories: List<MemoryEntity>
    ): ExtractionResult {
        val trimmed = userMessage.trim()
        val lower = trimmed.lowercase()

        // 1. Project identification
        val projectRegexes = listOf(
            Regex("(?:my project(?: name)? is(?: called)?|the project is called|i am building|i'm building|we are building)\\s+[:]?\\s*([A-Za-z0-9_\\-\\s]{3,40})", RegexOption.IGNORE_CASE),
            Regex("(?:working on a project named|project called)\\s+[:]?\\s*([A-Za-z0-9_\\-\\s]{3,40})", RegexOption.IGNORE_CASE)
        )
        for (pattern in projectRegexes) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val projectName = match.groupValues[1].trim().trimEnd('.', ',')
                val title = "Project: $projectName"
                val content = "The user's project is called $projectName."
                val existingMatch = existingMemories.firstOrNull {
                    it.category == "Projects" || it.title.contains("project", ignoreCase = true)
                }
                return ExtractionResult(
                    shouldStore = true,
                    title = title,
                    content = content,
                    category = "Projects",
                    importance = 9,
                    isUpdate = existingMatch != null,
                    existingMemoryIdToUpdate = existingMatch?.id
                )
            }
        }

        // 2. Preferences
        val prefRegexes = listOf(
            Regex("(?:i prefer|my preference is|i always prefer|i like to use|i usually use)\\s+[:]?\\s*([^.!?]{4,80})", RegexOption.IGNORE_CASE),
            Regex("(?:favorite [a-z\\s]+ is)\\s+[:]?\\s*([^.!?]{3,60})", RegexOption.IGNORE_CASE)
        )
        for (pattern in prefRegexes) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val preference = match.groupValues[1].trim()
                val title = "Preference: ${preference.take(24)}..."
                val content = "User preference: $preference"
                val existingMatch = existingMemories.firstOrNull {
                    it.category == "Preferences" && (it.content.contains(preference, ignoreCase = true) || preference.contains(it.title, ignoreCase = true))
                }
                return ExtractionResult(
                    shouldStore = true,
                    title = title,
                    content = content,
                    category = "Preferences",
                    importance = 8,
                    isUpdate = existingMatch != null,
                    existingMemoryIdToUpdate = existingMatch?.id
                )
            }
        }

        // 3. Explicit Memory Directive ("Remember that...", "Please remember...")
        val rememberRegex = Regex("(?:remember that|please remember|keep in mind that|note that)\\s+[:]?\\s*([^.!?]{4,100})", RegexOption.IGNORE_CASE)
        val rememberMatch = rememberRegex.find(trimmed)
        if (rememberMatch != null) {
            val fact = rememberMatch.groupValues[1].trim()
            val existing = findClosestMemory(fact, existingMemories)
            return ExtractionResult(
                shouldStore = true,
                title = "Important Fact: ${fact.take(24)}",
                content = fact,
                category = "Important facts",
                importance = 9,
                isUpdate = existing != null,
                existingMemoryIdToUpdate = existing?.id
            )
        }

        // 4. Personal Information ("My name is...", "I work as...", "I am a...")
        val personalRegex = Regex("(?:my name is|i am a|i'm a|i work as a|i live in)\\s+[:]?\\s*([^.!?]{3,60})", RegexOption.IGNORE_CASE)
        val personalMatch = personalRegex.find(trimmed)
        if (personalMatch != null) {
            val info = personalMatch.groupValues[1].trim()
            val existing = existingMemories.firstOrNull { it.category == "Personal information" }
            return ExtractionResult(
                shouldStore = true,
                title = "Personal: ${trimmed.take(24)}",
                content = trimmed,
                category = "Personal information",
                importance = 9,
                isUpdate = existing != null,
                existingMemoryIdToUpdate = existing?.id
            )
        }

        // 5. Learning
        if (lower.startsWith("i am studying") || lower.startsWith("i'm learning") || lower.startsWith("i am reading")) {
            val topic = trimmed.removePrefix("i am studying").removePrefix("i'm learning").removePrefix("i am reading").trim()
            return ExtractionResult(
                shouldStore = true,
                title = "Learning: ${topic.take(24)}",
                content = "User is studying/learning $topic",
                category = "Learning",
                importance = 7
            )
        }

        return ExtractionResult(shouldStore = false)
    }

    private fun findClosestMemory(text: String, memories: List<MemoryEntity>): MemoryEntity? {
        val results = SemanticVectorEngine.retrieveTopMemories(text, memories, minThreshold = 0.65f, topK = 1)
        return results.firstOrNull()?.memory
    }
}
