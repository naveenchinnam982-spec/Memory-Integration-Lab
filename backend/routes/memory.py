from fastapi import APIRouter, HTTPException
from typing import List
from backend.database.models import MemoryCreate, MemoryResponse, MemoryUpdate
from backend.database.repository import MemoryRepository
from backend.memory.embedding_service import EmbeddingService
from backend.memory.vector_store import VectorStore

router = APIRouter(prefix="/api/memory", tags=["Memory"])

@router.get("/list", response_model=List[MemoryResponse])
def list_memories():
    return [MemoryResponse(**m) for m in MemoryRepository.get_all()]

@router.post("/add", response_model=MemoryResponse)
def add_memory(req: MemoryCreate):
    emb = EmbeddingService.create_embedding(f"{req.title} {req.content}")
    emb_str = ",".join(f"{x:.4f}" for x in emb)
    mem_id = MemoryRepository.insert(
        title=req.title,
        content=req.content,
        category=req.category,
        importance=req.importance,
        vector_embedding=emb_str,
        is_short_term=req.is_short_term,
        expiration_timestamp=req.expiration_timestamp
    )
    saved = MemoryRepository.get_by_id(mem_id)
    return MemoryResponse(**saved)

@router.get("/{memory_id}", response_model=MemoryResponse)
def get_memory(memory_id: int):
    mem = MemoryRepository.get_by_id(memory_id)
    if not mem:
        raise HTTPException(status_code=404, detail="Memory not found")
    return MemoryResponse(**mem)

@router.put("/{memory_id}", response_model=MemoryResponse)
def update_memory_endpoint(memory_id: int, req: MemoryUpdate):
    existing = MemoryRepository.get_by_id(memory_id)
    if not existing:
        raise HTTPException(status_code=404, detail="Memory not found")
    
    title = req.title if req.title is not None else existing["title"]
    content = req.content if req.content is not None else existing["content"]
    category = req.category if req.category is not None else existing["category"]
    importance = req.importance if req.importance is not None else existing["importance"]
    
    emb = EmbeddingService.create_embedding(f"{title} {content}")
    emb_str = ",".join(f"{x:.4f}" for x in emb)
    
    MemoryRepository.update(memory_id, title, content, category, importance, emb_str)
    updated = MemoryRepository.get_by_id(memory_id)
    return MemoryResponse(**updated)

@router.delete("/{memory_id}")
def delete_memory(memory_id: int):
    success = MemoryRepository.delete(memory_id)
    if not success:
        raise HTTPException(status_code=404, detail="Memory not found")
    return {"status": "success", "deleted_id": memory_id}

@router.post("/search")
def search_memories(query: str, min_threshold: float = 0.25):
    all_mem = MemoryRepository.get_all()
    results = VectorStore.search(query, all_mem, min_threshold=min_threshold)
    return [{"memory": MemoryResponse(**m), "similarity": s} for m, s in results]

@router.post("/retrieve")
def retrieve_memories(query: str, top_k: int = 3, min_threshold: float = 0.25):
    all_mem = MemoryRepository.get_all()
    results = VectorStore.search(query, all_mem, min_threshold=min_threshold, top_k=top_k)
    return [
        {
            "memory": MemoryResponse(**m),
            "similarity_score": s,
            "rank": idx + 1
        }
        for idx, (m, s) in enumerate(results)
    ]

