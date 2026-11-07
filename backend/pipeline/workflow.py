from typing import Dict, Any, List
from backend.database.repository import MemoryRepository
from backend.memory.vector_store import VectorStore
from backend.memory.memory_extractor import MemoryExtractor
from backend.memory.embedding_service import EmbeddingService
from backend.ai.gemini_client import GeminiClient

class AIWorkflowPipeline:
    @staticmethod
    def execute(query: str, is_memory_enabled: bool, min_similarity: float = 0.25) -> Dict[str, Any]:
        # 1. Fetch current stored memories
        all_memories = MemoryRepository.get_all()

        # 2. Semantic retrieval if enabled
        retrieved = []
        if is_memory_enabled:
            scored = VectorStore.search(query, all_memories, min_threshold=min_similarity, top_k=3)
            for mem, sim in scored:
                m = dict(mem)
                m["similarity_score"] = sim
                MemoryRepository.record_access(m["id"])
                retrieved.append(m)

        # 3. AI Generation
        reply, tokens, latency = GeminiClient.generate(
            prompt=query,
            is_memory_enabled=is_memory_enabled,
            retrieved_memories=retrieved
        )

        # 4. Memory extraction and update
        extracted = MemoryExtractor.extract(query)
        newly_saved = None
        if extracted:
            embedding = EmbeddingService.create_embedding(f"{extracted['title']} {extracted['content']}")
            emb_str = ",".join(f"{x:.4f}" for x in embedding)
            new_id = MemoryRepository.insert(
                title=extracted["title"],
                content=extracted["content"],
                category=extracted["category"],
                importance=extracted["importance"],
                vector_embedding=emb_str
            )
            newly_saved = f"Stored Memory #{new_id}: {extracted['title']}"

        return {
            "reply": reply,
            "is_memory_enabled": is_memory_enabled,
            "memories_retrieved": retrieved,
            "context_tokens": tokens,
            "latency_ms": latency,
            "newly_saved_memory": newly_saved
        }
