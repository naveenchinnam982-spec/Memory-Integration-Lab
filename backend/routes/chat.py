from fastapi import APIRouter
from backend.database.models import ChatRequest, ChatResponse
from backend.pipeline.workflow import AIWorkflowPipeline

router = APIRouter(prefix="/api", tags=["Chat"])

@router.post("/chat", response_model=ChatResponse)
def chat_endpoint(request: ChatRequest):
    result = AIWorkflowPipeline.execute(
        query=request.message,
        is_memory_enabled=request.is_memory_enabled,
        min_similarity=request.min_similarity
    )
    return ChatResponse(**result)
