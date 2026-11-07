import re
from typing import Dict, Any, Optional

class MemoryExtractor:
    @staticmethod
    def extract(user_query: str) -> Optional[Dict[str, Any]]:
        text = user_query.strip()
        lower = text.lower()

        # 1. Project name
        proj_match = re.search(r'(?:my project(?: name)? is(?: called)?|project called|building)\s+[:]?\s*([A-Za-z0-9_\-\s]{3,40})', text, re.IGNORECASE)
        if proj_match:
            name = proj_match.group(1).strip().rstrip('.,')
            return {
                "title": f"Project: {name}",
                "content": f"The user's project is called {name}.",
                "category": "Projects",
                "importance": 9
            }

        # 2. Preference
        pref_match = re.search(r'(?:i prefer|my preference is|i like to use)\s+[:]?\s*([^.!?]{4,80})', text, re.IGNORECASE)
        if pref_match:
            pref = pref_match.group(1).strip()
            return {
                "title": f"Preference: {pref[:24]}...",
                "content": f"User preference: {pref}",
                "category": "Preferences",
                "importance": 8
            }

        # 3. Explicit memory directive
        rem_match = re.search(r'(?:remember that|please remember|note that)\s+[:]?\s*([^.!?]{4,100})', text, re.IGNORECASE)
        if rem_match:
            fact = rem_match.group(1).strip()
            return {
                "title": f"Fact: {fact[:24]}",
                "content": fact,
                "category": "Important facts",
                "importance": 9
            }

        # 4. Identity
        ident_match = re.search(r'(?:my name is|i am a|i work as a)\s+[:]?\s*([^.!?]{3,60})', text, re.IGNORECASE)
        if ident_match:
            return {
                "title": f"Personal: {text[:24]}",
                "content": text,
                "category": "Personal information",
                "importance": 9
            }

        return None
