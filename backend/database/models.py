from typing import List, Optional
from pydantic import BaseModel, Field
import time

class MemoryCreate(BaseModel):
    title: str
    content: str
    category: str = "Other"
    importance: int = Field(default=5, ge=1, le=10)
    is_short_term: bool = False
    expiration_timestamp: Optional[int] = None

class MemoryUpdate(BaseModel):
    title: Optional[str] = None
    content: Optional[str] = None
    category: Optional[str] = None
    importance: Optional[int] = Field(default=None, ge=1, le=10)
    is_short_term: Optional[bool] = None

class MemoryResponse(BaseModel):
    id: int
    title: str
    content: str
    category: str
    importance: int
    created_at: int
    last_accessed_at: int
    access_count: int
    is_short_term: bool
    expiration_timestamp: Optional[int] = None
    similarity_score: Optional[float] = None

class ChatRequest(BaseModel):
    message: str
    is_memory_enabled: bool = True
    min_similarity: float = 0.25

class ChatResponse(BaseModel):
    reply: str
    is_memory_enabled: bool
    memories_retrieved: List[MemoryResponse] = []
    context_tokens: int
    latency_ms: int
    newly_saved_memory: Optional[str] = None

class ComparisonRequest(BaseModel):
    query: str

class ComparisonResponse(BaseModel):
    query: str
    normal_ai_response: str
    memory_ai_response: str
    retrieved_memories: List[MemoryResponse]
    normal_tokens: int
    memory_tokens: int
    normal_latency_ms: int
    memory_latency_ms: int
