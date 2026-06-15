import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """    private String getCardinalDirection(Vector dir) {"""

replacement = """    public String getCardinalDirection(Vector dir) {"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Made getCardinalDirection public")
else:
    print("Failed to find getCardinalDirection")
