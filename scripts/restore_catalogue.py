#!/usr/bin/env python3
"""Restore the authoritative 424-recipe catalogue from committed transport chunks."""
from __future__ import annotations

import base64
import gzip
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHUNKS = ROOT / "data" / "catalogue_payload"
OUTPUT = json.dumps  # keep the implementation dependency-free


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
    payload = json.dumps(recipes, ensure_ascii=False, indent=2) + "\n"
    targets = [
        ROOT / "data" / "recipes_424.json",
        ROOT / "backend" / "app" / "recipes_424.json",
        ROOT / "android" / "app" / "src" / "main" / "assets" / "recipes_424.json",
    ]
    for target in targets:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(payload, encoding="utf-8")
    print(f"CATALOGUE RESTORE PASS: 424 recipes -> {len(targets)} targets")


if __name__ == "__main__":
    main()
