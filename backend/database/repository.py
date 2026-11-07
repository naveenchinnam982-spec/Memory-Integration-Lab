import time
from typing import List, Optional, Dict, Any
from backend.database.db import get_connection

class MemoryRepository:
    @staticmethod
    def get_all() -> List[Dict[str, Any]]:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM memories ORDER BY last_accessed_at DESC")
        rows = [dict(row) for row in cursor.fetchall()]
        conn.close()
        return rows

    @staticmethod
    def get_by_id(memory_id: int) -> Optional[Dict[str, Any]]:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM memories WHERE id = ?", (memory_id,))
        row = cursor.fetchone()
        conn.close()
        return dict(row) if row else None

    @staticmethod
    def insert(
        title: str,
        content: str,
        category: str,
        importance: int,
        vector_embedding: str,
        is_short_term: bool = False,
        expiration_timestamp: Optional[int] = None
    ) -> int:
        conn = get_connection()
        cursor = conn.cursor()
        now = int(time.time() * 1000)
        cursor.execute("""
        INSERT INTO memories (title, content, category, importance, created_at, last_accessed_at, access_count, is_short_term, expiration_timestamp, vector_embedding)
        VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?)
        """, (title, content, category, importance, now, now, 1 if is_short_term else 0, expiration_timestamp, vector_embedding))
        memory_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return memory_id

    @staticmethod
    def update(memory_id: int, title: str, content: str, category: str, importance: int, vector_embedding: str) -> bool:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("""
        UPDATE memories
        SET title = ?, content = ?, category = ?, importance = ?, vector_embedding = ?
        WHERE id = ?
        """, (title, content, category, importance, vector_embedding, memory_id))
        affected = cursor.rowcount > 0
        conn.commit()
        conn.close()
        return affected

    @staticmethod
    def record_access(memory_id: int):
        conn = get_connection()
        cursor = conn.cursor()
        now = int(time.time() * 1000)
        cursor.execute("""
        UPDATE memories
        SET access_count = access_count + 1, last_accessed_at = ?
        WHERE id = ?
        """, (now, memory_id))
        conn.commit()
        conn.close()

    @staticmethod
    def delete(memory_id: int) -> bool:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("DELETE FROM memories WHERE id = ?", (memory_id,))
        affected = cursor.rowcount > 0
        conn.commit()
        conn.close()
        return affected

    @staticmethod
    def clear_all():
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("DELETE FROM memories")
        cursor.execute("DELETE FROM chat_logs")
        conn.commit()
        conn.close()
