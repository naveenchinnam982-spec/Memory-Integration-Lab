import pytest
from backend.config import settings
settings.database_file = ":memory:"

from backend.database.db import init_db
from backend.database.repository import MemoryRepository
from backend.memory.embedding_service import EmbeddingService
from backend.memory.vector_store import VectorStore

@pytest.fixture(autouse=True)
def setup_db():
    init_db()
    yield
    MemoryRepository.clear_all()

def test_vector_cosine_similarity():
    v1 = EmbeddingService.create_embedding("Android Kotlin Jetpack Compose")
    v2 = EmbeddingService.create_embedding("Kotlin Compose Android application")
    v3 = EmbeddingService.create_embedding("Italian pasta pizza recipe")

    sim_related = EmbeddingService.cosine_similarity(v1, v2)
    sim_unrelated = EmbeddingService.cosine_similarity(v1, v3)

    assert sim_related > sim_unrelated
    assert sim_related > 0.40

def test_semantic_ranking():
    e1 = EmbeddingService.create_embedding("Project: HealthContinuum AI Medical platform")
    MemoryRepository.insert("Project: HealthContinuum AI", "Healthcare platform", "Projects", 9, ",".join(f"{x:.4f}" for x in e1))

    e2 = EmbeddingService.create_embedding("User loves Kotlin and Compose for Android development")
    MemoryRepository.insert("Preferences: Kotlin Compose", "Loves Kotlin Compose", "Preferences", 8, ",".join(f"{x:.4f}" for x in e2))

    all_mem = MemoryRepository.get_all()
    results = VectorStore.search("What is my project name?", all_mem, min_threshold=0.20, top_k=2)

    assert len(results) > 0
    assert "HealthContinuum" in results[0][0]["title"]
