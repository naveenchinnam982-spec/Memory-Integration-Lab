from fastapi import APIRouter
from backend.database.models import ComparisonRequest, ComparisonResponse, MemoryResponse
from backend.database.repository import MemoryRepository
from backend.memory.vector_store import VectorStore
from backend.ai.gemini_client import GeminiClient

router = APIRouter(prefix="/api", tags=["Comparison"])

@router.post("/comparison", response_model=ComparisonResponse)
def compare_endpoint(req: ComparisonRequest):
    all_memories = MemoryRepository.get_all()
    scored = VectorStore.search(req.query, all_memories, min_threshold=0.25, top_k=3)

    retrieved = []
    for mem, sim in scored:
        m = dict(mem)
        m["similarity_score"] = sim
        retrieved.append(m)

    # Normal AI
    norm_reply, norm_tokens, norm_lat = GeminiClient.generate(
        prompt=req.query,
        is_memory_enabled=False,
        retrieved_memories=[]
    )

    # Memory AI
    mem_reply, mem_tokens, mem_lat = GeminiClient.generate(
        prompt=req.query,
        is_memory_enabled=True,
        retrieved_memories=retrieved
    )

    return ComparisonResponse(
        query=req.query,
        normal_ai_response=norm_reply,
        memory_ai_response=mem_reply,
        retrieved_memories=[MemoryResponse(**m) for m in retrieved],
        normal_tokens=norm_tokens,
        memory_tokens=mem_tokens,
        normal_latency_ms=norm_lat,
        memory_latency_ms=mem_lat
    )
