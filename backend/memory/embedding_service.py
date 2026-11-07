import math
import re
from typing import List
from backend.config import settings

class EmbeddingService:
    @staticmethod
    def create_embedding(text: str) -> List[float]:
        dim = settings.vector_dimension
        vector = [0.0] * dim
        if not text or not text.strip():
            return vector

        tokens = re.findall(r'\b\w+\b', text.lower())
        for token in tokens:
            idx = abs(hash(token)) % dim
            vector[idx] += 1.0

        # Subword n-grams
        normalized = text.lower().strip()
        if len(normalized) >= 3:
            for i in range(len(normalized) - 2):
                tri = normalized[i:i+3]
                idx = abs(hash(tri)) % dim
                vector[idx] += 0.35

        # L2 norm
        sq_sum = sum(v * v for v in vector)
        norm = math.sqrt(sq_sum)
        if norm > 0:
            vector = [v / norm for v in vector]

        return vector

    @staticmethod
    def cosine_similarity(v1: List[float], v2: List[float]) -> float:
        if not v1 or not v2 or len(v1) != len(v2):
            return 0.0
        dot = sum(a * b for a, b in zip(v1, v2))
        return max(0.0, min(1.0, dot))
