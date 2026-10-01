from __future__ import annotations

import os
import sys
from urllib.parse import urljoin

import httpx


def require_env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise SystemExit(f"Missing required environment variable: {name}")
    return value


def main() -> None:
    base = require_env("PRODUCTION_BASE_URL").rstrip("/")
    if not base.startswith("https://"):
        raise SystemExit("PRODUCTION_BASE_URL must use HTTPS")

    timeout = httpx.Timeout(30.0, connect=10.0)

    with httpx.Client(base_url=base, timeout=timeout, follow_redirects=False) as client:
        health = client.get("/health")
        health.raise_for_status()
        health_data = health.json()
        assert health_data == {"ok": True, "recipeCount": 424}, health_data

        search = client.post(
            "/v1/recipes/search",
            json={"query": "pineapple", "ingredients": [], "limit": 5},
        )
        search.raise_for_status()
        search_data = search.json()
        assert isinstance(search_data, list), search_data
        assert search_data, "Production recipe search returned no results"

        ai = client.post(
            "/v1/ai/chat",
            json={
                "messages": [
                    {
                        "role": "user",
                        "content": "Reply with exactly: production smoke test",
                    }
                ]
            },
        )
        if ai.status_code == 503:
            raise SystemExit(
                "Production AI is not configured. Configure AI_API_KEY and AI_MODEL "
                "on the deployed backend before claiming production verification."
            )
        ai.raise_for_status()
        ai_data = ai.json()
        assert isinstance(ai_data.get("content"), str) and ai_data["content"], ai_data

        import_response = client.post(
            "/v1/import/url",
            json={"url": "https://example.com/"},
        )
        import_response.raise_for_status()
        import_data = import_response.json()
        assert import_data.get("status") == "draft", import_data
        assert import_data.get("requiresConfirmation") is True, import_data
        assert import_data.get("provenance") == "external-url", import_data

        blocked = client.post(
            "/v1/import/url",
            json={"url": "http://127.0.0.1/recipe"},
        )
        assert blocked.status_code == 400, blocked.text

    print("Production smoke verification passed: HTTPS, 424 catalogue, search, AI, URL import, and SSRF rejection.")


if __name__ == "__main__":
    main()
