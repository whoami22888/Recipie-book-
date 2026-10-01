#!/usr/bin/env python3
"""Restore the authoritative 424-recipe catalogue from committed transport chunks."""
from __future__ import annotations

import base64
import gzip
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHUNKS = ROOT / "data" / "catalogue_payload"


def main() -> None:
    parts = sorted(CHUNKS.glob("part-*.b64"))
    if not parts:
        raise SystemExit("catalogue payload chunks are missing")
    encoded = "".join(p.read_text(encoding="ascii").strip() for p in parts)
    raw = gzip.decompress(base64.b64decode(encoded))
    recipes = json.loads(raw)
    if len(recipes) != 424:
        raise SystemExit(f"catalogue count mismatch: {len(recipes)}")
    numbers = [r.get("sourceRecipeNumber") for r in recipes]
    if numbers != list(range(1, 425)):
        raise SystemExit("catalogue recipe numbers are not exactly 1..424")
    if len({r.get("id") for r in recipes}) != 424:
        raise SystemExit("catalogue recipe IDs are not unique")
    for recipe in recipes:
        if recipe.get("shoppingList") == [
            "Main protein / base per title",
            "Vegetables as shown in video",
            "Pantry: oil, salt, pepper, garlic",
            "Sauce/cheese per style",
        ]:
            recipe["qualityFlags"] = ["generic-fallback-shopping-list"]
        elif recipe.get("method") == [
            "1. Prep ingredients as shown",
            "2. Heat pan with oil",
            "3. Cook protein until golden",
            "4. Add veg/sauce",
            "5. Simmer until done per video",
            "6. Serve hot",
        ]:
            recipe["qualityFlags"] = ["generic-fallback-method"]
    if not any(recipe.get("qualityFlags") for recipe in recipes):
        raise SystemExit("catalogue quality flags are unexpectedly empty")
    catalog = {
        "schemaVersion": 1,
        "source": "For My Wife - The Complete Collection",
        "recipeCount": 424,
        "recipes": recipes,
    }
    payload = json.dumps(catalog, ensure_ascii=False, indent=2) + "\n"
    targets = [
        ROOT / "data" / "recipes_424.json",
        ROOT / "backend" / "app" / "recipes_424.json",
        ROOT / "android" / "app" / "src" / "main" / "assets" / "recipes_424.json",
    ]
    for target in targets:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(payload, encoding="utf-8")
    print("CATALOGUE RESTORE PASS: 424 recipes -> 3 targets")


if __name__ == "__main__":
    main()
