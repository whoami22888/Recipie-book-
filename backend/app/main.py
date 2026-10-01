from __future__ import annotations

import ipaddress
import json
import os
import socket
from typing import Any
from urllib.parse import urljoin, urlparse

import httpx
from bs4 import BeautifulSoup
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, ConfigDict, Field, HttpUrl

app = FastAPI(title="Beyond Human Kitchen API", version="0.4.0")
DATA = os.path.join(os.path.dirname(__file__), "recipes_424.json")
with open(DATA, encoding="utf-8") as handle:
    CATALOGUE = json.load(handle)
RECIPES = CATALOGUE["recipes"]

MAX_IMPORT_BYTES = 2 * 1024 * 1024
MAX_REDIRECTS = 5
ALLOWED_IMPORT_SCHEMES = {"http", "https"}

class SearchRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    query: str = Field(default="", max_length=200)
    ingredients: list[str] = Field(default_factory=list, max_length=30)
    limit: int = Field(default=20, ge=1, le=100)

class ChatMessage(BaseModel):
    model_config = ConfigDict(extra="forbid")
    role: str = Field(pattern="^(system|user|assistant)$")
    content: str = Field(min_length=1, max_length=12000)

class ChatRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    messages: list[ChatMessage] = Field(min_length=1, max_length=30)

class ImportRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    url: HttpUrl

def _search_haystack(recipe: dict[str, Any]) -> str:
    return " ".join([recipe.get("title", ""), recipe.get("description", ""), recipe.get("_section", ""), *recipe.get("shoppingList", [])]).lower()

def _score_recipe(recipe: dict[str, Any], query: str, ingredients: list[str]) -> int:
    title = recipe.get("title", "").lower()
    haystack = _search_haystack(recipe)
    terms = [term for term in query.lower().split() if term]
    score = 0
    if query and title == query.lower(): score += 1000
    if query and query.lower() in title: score += 300
    score += sum(20 for term in terms if term in haystack)
    score += sum(100 for ingredient in ingredients if ingredient.lower() in haystack)
    return score

@app.get("/health")
def health() -> dict[str, Any]:
    return {"ok": True, "recipeCount": len(RECIPES)}

@app.post("/v1/recipes/search")
def search(req: SearchRequest) -> list[dict[str, Any]]:
    query = req.query.strip()
    ingredients = [item.strip() for item in req.ingredients if item.strip()]
    scored = [(recipe, _score_recipe(recipe, query, ingredients)) for recipe in RECIPES]
    return [recipe for recipe, score in sorted(scored, key=lambda item: item[1], reverse=True) if score > 0][: req.limit]

@app.post("/v1/ai/chat")
async def ai_chat(payload: ChatRequest) -> dict[str, str]:
    api_key = os.getenv("AI_API_KEY")
    if not api_key: raise HTTPException(status_code=503, detail="AI provider is not configured")
    model = os.getenv("AI_MODEL")
    if not model: raise HTTPException(status_code=503, detail="AI_MODEL is not configured")
    base = os.getenv("AI_BASE_URL", "https://api.openai.com/v1").rstrip("/")
    body = {"model": model, "messages": [message.model_dump() for message in payload.messages], "temperature": 0.2}
    try:
        async with httpx.AsyncClient(timeout=httpx.Timeout(45.0, connect=10.0)) as client:
            response = await client.post(f"{base}/chat/completions", headers={"Authorization": f"Bearer {api_key}"}, json=body)
            response.raise_for_status()
            data = response.json()
    except (httpx.HTTPError, ValueError) as exc:
        raise HTTPException(status_code=502, detail="AI provider request failed") from exc
    try:
        content = data["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as exc:
        raise HTTPException(status_code=502, detail="AI provider returned an invalid response") from exc
    return {"content": content, "model": data.get("model", model)}

def _validate_import_target(url: str) -> None:
    parsed = urlparse(url)
    if parsed.scheme not in ALLOWED_IMPORT_SCHEMES or not parsed.hostname:
        raise HTTPException(status_code=400, detail="Only HTTP(S) URLs with a hostname are supported")
    try:
        addresses = socket.getaddrinfo(parsed.hostname, parsed.port or (443 if parsed.scheme == "https" else 80), type=socket.SOCK_STREAM)
    except socket.gaierror as exc:
        raise HTTPException(status_code=400, detail="Import host could not be resolved") from exc
    for address in {item[4][0] for item in addresses}:
        ip = ipaddress.ip_address(address)
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_multicast or ip.is_unspecified or ip.is_reserved:
            raise HTTPException(status_code=400, detail="Import target resolves to a non-public address")

async def _fetch_public_document(url: str) -> tuple[str, str]:
    current = url
    headers = {"User-Agent": "BeyondHumanKitchen/0.4 (+recipe-import)"}
    async with httpx.AsyncClient(follow_redirects=False, timeout=httpx.Timeout(15.0, connect=5.0)) as client:
        for _ in range(MAX_REDIRECTS + 1):
            _validate_import_target(current)
            try:
                response = await client.get(current, headers=headers)
                response.raise_for_status()
            except httpx.HTTPError as exc:
                raise HTTPException(status_code=502, detail="Unable to fetch recipe source") from exc
            if response.status_code in {301, 302, 303, 307, 308}:
                location = response.headers.get("location")
                if not location: raise HTTPException(status_code=502, detail="Recipe source returned an invalid redirect")
                current = urljoin(current, location)
                continue
            content_type = response.headers.get("content-type", "").lower()
            if "text/html" not in content_type and "application/xhtml+xml" not in content_type:
                raise HTTPException(status_code=415, detail="Recipe source is not HTML")
            body = response.content
            if len(body) > MAX_IMPORT_BYTES: raise HTTPException(status_code=413, detail="Recipe source exceeds the import size limit")
            return current, body.decode(response.encoding or "utf-8", errors="replace")
    raise HTTPException(status_code=502, detail="Too many redirects while importing recipe")

@app.post("/v1/import/url")
async def import_url(req: ImportRequest) -> dict[str, Any]:
    source_url, html = await _fetch_public_document(str(req.url))
    soup = BeautifulSoup(html, "html.parser")
    title_node = soup.find("meta", property="og:title") or soup.find("title")
    title = title_node.get("content", "") if title_node and title_node.name == "meta" else title_node.get_text(" ", strip=True) if title_node else ""
    ingredients: list[str] = []
    instructions: list[str] = []
    for node in soup.find_all("script", type="application/ld+json"):
        try: data = json.loads(node.string or "")
        except (TypeError, json.JSONDecodeError): continue
        candidates = data if isinstance(data, list) else data.get("@graph", []) if isinstance(data, dict) else []
        if isinstance(data, dict) and "Recipe" in str(data.get("@type", "")): candidates = [data, *candidates]
        for candidate in candidates:
            if not isinstance(candidate, dict) or "Recipe" not in str(candidate.get("@type", "")): continue
            ingredients = [str(item).strip() for item in candidate.get("recipeIngredient", []) if str(item).strip()]
            raw_instructions = candidate.get("recipeInstructions", [])
            instructions = [(item.get("text", "") if isinstance(item, dict) else str(item)).strip() for item in raw_instructions if (item.get("text", "") if isinstance(item, dict) else str(item)).strip()]
            break
        if ingredients or instructions: break
    return {"status": "draft", "sourceUrl": source_url, "title": title, "requiresConfirmation": True, "ingredients": ingredients, "method": instructions, "provenance": "external-url"}
