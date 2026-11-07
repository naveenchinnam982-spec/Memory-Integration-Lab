import sqlite3
import time
from backend.config import settings

def get_connection():
    conn = sqlite3.connect(settings.database_file)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS memories (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        content TEXT NOT NULL,
        category TEXT NOT NULL,
        importance INTEGER NOT NULL,
        created_at INTEGER NOT NULL,
        last_accessed_at INTEGER NOT NULL,
        access_count INTEGER DEFAULT 0,
        is_short_term INTEGER DEFAULT 0,
        expiration_timestamp INTEGER,
        vector_embedding TEXT
    )
    """)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS chat_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        query TEXT NOT NULL,
        reply TEXT NOT NULL,
        retrieved_count INTEGER,
        highest_similarity REAL,
        latency_ms INTEGER,
        timestamp INTEGER NOT NULL
    )
    """)
    conn.commit()
    conn.close()
