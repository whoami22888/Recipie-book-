import json
import sys

path = sys.argv[1] if len(sys.argv) > 1 else "data/recipes_424.json"
with open(path, encoding="utf-8") as handle:
    data = json.load(handle)

recipes = data["recipes"]
assert data["recipeCount"] == 424
assert len(recipes) == 424
assert [item["sourceRecipeNumber"] for item in recipes] == list(range(1, 425))
assert len({item["id"] for item in recipes}) == 424
assert all(item["title"].strip() for item in recipes)
assert all("_section" in item for item in recipes)
print("CATALOGUE PASS: 424/424")
