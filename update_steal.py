import os

filepath = "src/main/java/com/wolfool/relicwars/relic/RelicManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = "relics.sort((i1, i2) -> Integer.compare(RelicItemUtil.getRelicNumber(i2), RelicItemUtil.getRelicNumber(i1)));"
replacement = "java.util.Collections.shuffle(relics);"

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added shuffle to extractStealDrop.")
else:
    print("Target not found.")
