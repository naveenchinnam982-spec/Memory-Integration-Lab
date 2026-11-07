# Memory Integration Lab – API Documentation

Base URL: `http://localhost:8000/api` (or `http://10.0.2.2:8000/api` for Android Emulator)

## Endpoints

### 1. Chat Generation
- **Endpoint**: `POST /api/chat`
- **Description**: Submits a user query through the 9-step pipeline. Retrieves relevant memories, builds context, and generates an AI response.
- **Request Body**:
  ```json
  {
    "message": "What is my project called?",
    "is_memory_enabled": true,
    "min_similarity": 0.25
  }
  ```
- **Response**:
  ```json
  {
    "reply": "Your project is called HealthContinuum AI. Recalled from Memory #1.",
    "is_memory_enabled": true,
    "memories_retrieved": [
      {
        "id": 1,
        "title": "Project: HealthContinuum AI",
        "content": "The user is leading development of HealthContinuum AI...",
        "category": "Projects",
        "importance": 9,
        "similarity_score": 0.82
      }
    ],
    "context_tokens": 74,
    "latency_ms": 215,
    "newly_saved_memory": null
  }
  ```

### 2. Add Memory
- **Endpoint**: `POST /api/memory/add`
- **Request Body**:
  ```json
  {
    "title": "Preferences: Jetpack Compose",
    "content": "User prefers Kotlin and Jetpack Compose for Android.",
    "category": "Preferences",
    "importance": 8,
    "is_short_term": false
  }
  ```

### 3. List Memories
- **Endpoint**: `GET /api/memory/list`
- **Response**: Array of all stored memory records with category, access count, and timestamps.

### 4. Delete Memory
- **Endpoint**: `DELETE /api/memory/{memory_id}`

### 5. Semantic Memory Search
- **Endpoint**: `POST /api/memory/search?query=HealthContinuum&min_threshold=0.25`

### 6. A/B Model Comparison
- **Endpoint**: `POST /api/comparison`
- **Request Body**:
  ```json
  {
    "query": "What is my project called?"
  }
  ```
- **Response**: Provides parallel execution results for Normal AI vs. Memory-Enabled AI on the exact same model.

### 7. Health & Telemetry
- **Endpoint**: `GET /api/health`
- **Endpoint**: `GET /api/analytics`
