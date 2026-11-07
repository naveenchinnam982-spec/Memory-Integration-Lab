import pytest
from backend.config import settings
settings.database_file = ":memory:"

from backend.database.db import init_db
from backend.database.repository import MemoryRepository
from backend.memory.embedding_service import EmbeddingService
from backend.pipeline.workflow import AIWorkflowPipeline

@pytest.fixture(autouse=True)
def setup_db():
    init_db()
    yield
    MemoryRepository.clear_all()

def test_full_pipeline_with_memory():
    # 1. Insert a seed memory
    emb = EmbeddingService.create_embedding("Project: HealthContinuum AI Healthcare Medical Intelligence")
    MemoryRepository.insert("Project: HealthContinuum AI", "Healthcare Medical Intelligence", "Projects", 9, ",".join(f"{x:.4f}" for x in emb))

    # 2. Execute pipeline with memory enabled
    res = AIWorkflowPipeline.execute("What is my project called?", is_memory_enabled=True)

    assert res["is_memory_enabled"] is True
    assert len(res["memories_retrieved"]) > 0
    assert "HealthContinuum AI" in res["reply"]
    assert res["context_tokens"] > 0
    assert res["latency_ms"] > 0

def test_full_pipeline_without_memory():
    # Execute pipeline with memory disabled
    res = AIWorkflowPipeline.execute("What is my project called?", is_memory_enabled=False)

    assert res["is_memory_enabled"] is False
    assert len(res["memories_retrieved"]) == 0
    assert "don't have that information" in res["reply"].lower() or "standard assistant" in res["reply"].lower()
