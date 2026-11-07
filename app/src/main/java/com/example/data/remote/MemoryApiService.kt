package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ChatApiRequest(
    @Json(name = "message") val message: String,
    @Json(name = "is_memory_enabled") val isMemoryEnabled: Boolean = true,
    @Json(name = "min_similarity") val minSimilarity: Float = 0.25f
)

@JsonClass(generateAdapter = true)
data class ChatApiResponse(
    @Json(name = "reply") val reply: String,
    @Json(name = "is_memory_enabled") val isMemoryEnabled: Boolean,
    @Json(name = "memories_retrieved") val memoriesRetrieved: List<MemoryApiItem> = emptyList(),
    @Json(name = "context_tokens") val contextTokens: Int = 0,
    @Json(name = "latency_ms") val latencyMs: Int = 0,
    @Json(name = "newly_saved_memory") val newlySavedMemory: String? = null
)

@JsonClass(generateAdapter = true)
data class MemoryApiItem(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String,
    @Json(name = "category") val category: String,
    @Json(name = "importance") val importance: Int,
    @Json(name = "created_at") val createdAt: Long = 0L,
    @Json(name = "last_accessed_at") val lastAccessedAt: Long = 0L,
    @Json(name = "access_count") val accessCount: Int = 0,
    @Json(name = "is_short_term") val isShortTerm: Boolean = false,
    @Json(name = "similarity_score") val similarityScore: Float? = null
)

@JsonClass(generateAdapter = true)
data class AddMemoryApiRequest(
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String,
    @Json(name = "category") val category: String,
    @Json(name = "importance") val importance: Int,
    @Json(name = "is_short_term") val isShortTerm: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UpdateMemoryApiRequest(
    @Json(name = "title") val title: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "importance") val importance: Int? = null
)

@JsonClass(generateAdapter = true)
data class ComparisonApiRequest(
    @Json(name = "query") val query: String
)

@JsonClass(generateAdapter = true)
data class ComparisonApiResponse(
    @Json(name = "query") val query: String,
    @Json(name = "normal_ai_response") val normalAiResponse: String,
    @Json(name = "memory_ai_response") val memoryAiResponse: String,
    @Json(name = "retrieved_memories") val retrievedMemories: List<MemoryApiItem> = emptyList(),
    @Json(name = "normal_tokens") val normalTokens: Int = 0,
    @Json(name = "memory_tokens") val memoryTokens: Int = 0,
    @Json(name = "normal_latency_ms") val normalLatencyMs: Int = 0,
    @Json(name = "memory_latency_ms") val memoryLatencyMs: Int = 0
)

@JsonClass(generateAdapter = true)
data class AnalyticsApiResponse(
    @Json(name = "total_memories") val totalMemories: Int,
    @Json(name = "categories") val categories: Map<String, Int> = emptyMap(),
    @Json(name = "total_recalls") val totalRecalls: Int = 0,
    @Json(name = "average_similarity") val averageSimilarity: Float = 0f
)

@JsonClass(generateAdapter = true)
data class HealthApiResponse(
    @Json(name = "status") val status: String,
    @Json(name = "service") val service: String,
    @Json(name = "version") val version: String
)

interface MemoryApiService {
    @POST("api/chat")
    suspend fun chat(@Body request: ChatApiRequest): ChatApiResponse

    @GET("api/memory/list")
    suspend fun listMemories(): List<MemoryApiItem>

    @POST("api/memory/add")
    suspend fun addMemory(@Body request: AddMemoryApiRequest): MemoryApiItem

    @GET("api/memory/{id}")
    suspend fun getMemory(@Path("id") id: Long): MemoryApiItem

    @PUT("api/memory/{id}")
    suspend fun updateMemory(@Path("id") id: Long, @Body request: UpdateMemoryApiRequest): MemoryApiItem

    @DELETE("api/memory/{id}")
    suspend fun deleteMemory(@Path("id") id: Long): Map<String, Any>

    @POST("api/comparison")
    suspend fun compare(@Body request: ComparisonApiRequest): ComparisonApiResponse

    @GET("api/analytics")
    suspend fun getAnalytics(): AnalyticsApiResponse

    @GET("api/health")
    suspend fun checkHealth(): HealthApiResponse
}

object ApiClient {
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    fun create(baseUrl: String = DEFAULT_BASE_URL): MemoryApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(MemoryApiService::class.java)
    }
}
