# Memory Integration Lab – Technical Documentation & Architecture

## Overview
The **Memory Integration Lab** is a mobile and backend system designed to bridge conversational AI with continuous, persistent, semantic long-term memory. Unlike standard LLMs that suffer from immediate context amnesia between sessions, this application introduces an optimized 9-step intelligent memory retrieval and context construction pipeline.

---

## 9-Step AI Workflow Pipeline
```
[User Query] 
     ↓
[Query Analysis & Intent Identification]
     ↓
[Semantic Vector Memory Retrieval] (Cosine Similarity ranking, >= 0.25 threshold)
     ↓
[Context Construction] (Injecting relevant memories + history into prompt)
     ↓
[AI Processing] (Gemini API with memory context)
     ↓
[Response Generation & Streaming]
     ↓
[Memory Evaluation] (Automatic detection of new facts/preferences)
     ↓
[Memory Update] (Deduplication & persistent SQLite/Room storage)
     ↓
[Response Delivery & Audit Trail]
```

---

## Architecture Components

### 1. Android Application (Jetpack Compose + Kotlin + Room)
- **MVVM Architecture**: Clean separation between UI, ViewModels, Repository, and Local Persistence.
- **Room Database**:
  - `memories` table: Stores title, content, category, importance (1-10), access counts, timestamps, and serialized 96-dimension L2-normalized vector embeddings.
  - `chat_messages` table: Stores conversation history with complete transparency audit metrics (tokens used, latency ms, similarity score, memory IDs).
- **On-Device Semantic Vector Engine**:
  - Dense 96-dimensional subword and term-frequency hashing with L2 normalization.
  - Cosine distance similarity metric with keyword boosting.
  - Offline-first execution: completely functional on-device without requiring external servers.
- **A/B Benchmark Module**: Side-by-side comparative laboratory evaluating Normal AI (zero memory context) vs. Memory-Enabled AI on the exact same model.
- **Transparency Audit**: Every AI message provides an inspectable audit dialog detailing memory IDs used, similarity scores, and tokens consumed.

### 2. Python FastAPI Backend (`/backend`)
- Provides RESTful endpoints matching the OpenAPI specification.
- Local SQLite vector storage with subword n-gram embedding service.
- Full CORS support for seamless multi-client connectivity.

---

## Setup & Execution

### Running the Python Backend:
```bash
cd backend
pip install -r requirements.txt
uvicorn backend.main:app --host 0.0.0.0 --port 8000 --reload
```

### Running Backend Tests:
```bash
pytest backend/tests/
```

### Running the Android App:
The Android app is pre-configured with Room persistence and self-contained semantic vector search.
- If you have a `GEMINI_API_KEY`, add it to the Secrets panel in AI Studio or via `.env`.
- When offline or without an API key, the built-in contextual synthesizer demonstrates both Normal and Memory-Enabled AI behaviors with full transparency.
