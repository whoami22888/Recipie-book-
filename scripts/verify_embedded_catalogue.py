import json

with open("data/recipes_424.json", encoding="utf-8") as handle:
    source = json.load(handle)
with open("android/app/src/main/assets/recipes_424.json", encoding="utf-8") as handle:
    embedded = json.load(handle)

assert source == embedded, "Android embedded catalogue differs from source catalogue"
assert len(embedded["recipes"]) == 424
assert all("_section" in recipe for recipe in embedded["recipes"])
print("CATALOGUE EMBED + SECTION MATCH PASS")
