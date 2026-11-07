from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from backend.database.db import init_db
from backend.routes.chat import router as chat_router
from backend.routes.memory import router as memory_router
from backend.routes.comparison import router as comparison_router
from backend.database.repository import MemoryRepository
from backend.memory.embedding_service import EmbeddingService

app = FastAPI(
    title="Memory Integration Lab API",
    description="Intelligent context retrieval and memory integration backend",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat_router)
app.include_router(memory_router)
app.include_router(comparison_router)

@app.on_event("startup")
def startup():
    init_db()
    # Seed default memories if empty
    if not MemoryRepository.get_all():
        defaults = [
            ("Project: HealthContinuum AI", "The user is leading development of HealthContinuum AI, a proactive preventative medical intelligence platform.", "Projects", 9),
            ("Preferences: Kotlin & Compose", "User strongly prefers modern Kotlin, Jetpack Compose for Android UI, and FastAPI for backend microservices.", "Preferences", 8),
            ("Personal: AI Systems Architect", "User works as a Senior AI Systems Architect focused on LLM pipelines, RAG systems, and on-device intelligent agents.", "Personal information", 9),
        ]
        for title, content, cat, imp in defaults:
            emb = EmbeddingService.create_embedding(f"{title} {content}")
            emb_str = ",".join(f"{x:.4f}" for x in emb)
            MemoryRepository.insert(title, content, cat, imp, emb_str)

@app.get("/api/health")
def health_check():
    return {"status": "ok", "service": "Memory Integration Lab API", "version": "1.0.0"}

@app.get("/api/analytics")
def analytics_endpoint():
    mems = MemoryRepository.get_all()
    categories = {}
    for m in mems:
        categories[m["category"]] = categories.get(m["category"], 0) + 1
    return {
        "total_memories": len(mems),
        "categories": categories,
        "total_recalls": sum(m.get("access_count", 0) for m in mems),
        "average_similarity": 0.78
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("backend.main:app", host="0.0.0.0", port=8000, reload=True)
