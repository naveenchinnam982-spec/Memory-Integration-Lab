import os
from pydantic import BaseModel

class Settings(BaseModel):
    app_name: str = "Memory Integration Lab API"
    version: str = "1.0.0"
    gemini_api_key: str = os.getenv("GEMINI_API_KEY", "")
    vector_dimension: int = 96
    similarity_threshold: float = 0.28
    max_retrieved_memories: int = 3
    database_file: str = os.getenv("DATABASE_FILE", "memory_lab.db")

settings = Settings()
