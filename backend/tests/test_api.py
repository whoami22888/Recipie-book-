import json
import os

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)
CATALOGUE_PATH = os.path.join(os.path.dirname(__file__), "..", "app", "recipes_424.json")


def load_catalogue():
    with open(CATALOGUE_PATH, encoding="utf-8") as handle:
        return json.load(handle)


def test_catalogue():
    data = load_catalogue()
    assert len(data["recipes"]) == 424
    assert [r["sourceRecipeNumber"] for r in data["recipes"]] == list(range(1, 425))


def test_source_quality_flags_preserved():
    data = load_catalogue()
    assert any(r["qualityFlags"] for r in data["recipes"])


def test_health_reports_catalogue_size():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"ok": True, "recipeCount": 424}


def test_search_matches_multiple_ingredients():
    data = load_catalogue()
    target = data["recipes"][0]
    ingredients = target["shoppingList"][:2]
    response = client.post("/v1/recipes/search", json={"ingredients": ingredients, "limit": 100})
    assert response.status_code == 200
    assert target["id"] in [recipe["id"] for recipe in response.json()]


def test_empty_search_returns_first_page_of_catalogue():
    """Regression test for bug where empty search returned no results"""
    response = client.post("/v1/recipes/search", json={"query": "", "ingredients": [], "limit": 20})
    assert response.status_code == 200
    payload = response.json()
    assert len(payload) == 20, "Empty search should return 20 recipes (default page size)"
    assert payload[0]["sourceRecipeNumber"] == 1, "First recipe should be #1"
    assert payload[-1]["sourceRecipeNumber"] == 20, "Last recipe should be #20"


def test_ai_requires_explicit_provider_configuration(monkeypatch):
    monkeypatch.delenv("AI_API_KEY", raising=False)
    response = client.post("/v1/ai/chat", json={"messages": [{"role": "user", "content": "help"}]})
    assert response.status_code == 503


def test_import_rejects_localhost_before_network_access():
    response = client.post("/v1/import/url", json={"url": "http://127.0.0.1/recipe"})
    assert response.status_code == 400
    assert "non-public" in response.json()["detail"]
