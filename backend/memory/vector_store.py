from typing import List, Dict, Any, Tuple
from backend.memory.embedding_service import EmbeddingService

class VectorStore:
    @staticmethod
    def search(
        query: str,
        memories: List[Dict[str, Any]],
        min_threshold: float = 0.25,
        top_k: int = 3
    ) -> List[Tuple[Dict[str, Any], float]]:
        if not query or not memories:
            return []

        query_vec = EmbeddingService.create_embedding(query)
        scored = []

        for mem in memories:
            raw_emb = mem.get("vector_embedding") or ""
            if raw_emb:
                try:
                    mem_vec = [float(x) for x in raw_emb.split(",") if x.strip()]
                except Exception:
                    mem_vec = EmbeddingService.create_embedding(f"{mem['title']} {mem['content']}")
            else:
                mem_vec = EmbeddingService.create_embedding(f"{mem['title']} {mem['content']}")

            sim = EmbeddingService.cosine_similarity(query_vec, mem_vec)

            # Boost for explicit keyword match
            q_lower = query.lower()
            content_lower = f"{mem['title']} {mem['content']}".lower()
            if "project" in q_lower and "project" in content_lower:
                sim += 0.15
            if "prefer" in q_lower and "prefer" in content_lower:
                sim += 0.15

            sim = min(1.0, sim)
            if sim >= min_threshold:
                scored.append((mem, round(sim, 3)))

        scored.sort(key=lambda x: x[1], reverse=True)
        return scored[:top_k]
