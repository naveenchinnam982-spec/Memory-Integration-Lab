import os
import pytest
from backend.config import settings

# Use in-memory or test db
settings.database_file = ":memory:"

from backend.database.db import init_db
from backend.database.repository import MemoryRepository
from backend.memory.embedding_service import EmbeddingService
from backend.memory.vector_store import VectorStore
from backend.memory.memory_extractor import MemoryExtractor

@pytest.fixture(autouse=True)
def setup_test_db():
    init_db()
    yield
    MemoryRepository.clear_all()

def test_add_and_retrieve_memory():
    emb = EmbeddingService.create_embedding("Project HealthContinuum")
    emb_str = ",".join(f"{x:.4f}" for x in emb)
    mem_id = MemoryRepository.insert("Project: HealthContinuum", "Medical AI platform", "Projects", 9, emb_str)
    assert mem_id > 0

    mem = MemoryRepository.get_by_id(mem_id)
    assert mem is not None
    assert mem["title"] == "Project: HealthContinuum"

def test_semantic_vector_retrieval():
    emb = EmbeddingService.create_embedding("Project: HealthContinuum AI Medical platform")
    emb_str = ",".join(f"{x:.4f}" for x in emb)
    MemoryRepository.insert("Project: HealthContinuum AI", "Healthcare platform", "Projects", 9, emb_str)

    all_mem = MemoryRepository.get_all()
    results = VectorStore.search("What is my project called?", all_mem, min_threshold=0.20)
    assert len(results) > 0
    assert results[0][0]["title"] == "Project: HealthContinuum AI"
    assert results[0][1] >= 0.20

def test_memory_extraction():
    text = "My project is called HealthContinuum AI."
    extracted = MemoryExtractor.extract(text)
    assert extracted is not None
    assert "HealthContinuum AI" in extracted["title"]
    assert extracted["category"] == "Projects"
