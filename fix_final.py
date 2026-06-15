import os

for root, _, files in os.walk("src/main/java"):
    for file in files:
        if file.endswith(".java"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
            
            orig = content
            content = content.replace("com.wolfool.relicwars.relic.com.wolfool.relicwars.relic.RelicItemUtil", "com.wolfool.relicwars.relic.RelicItemUtil")
            content = content.replace("TOTEM_OF_UNDYING_OF_UNDYING", "TOTEM_OF_UNDYING")
            
            if file == "SealedRelicManager.java":
                content = content.replace("return activeSealedRelics.values();", "return activeRelics;")
            
            if file == "RelicAbilityHandler.java":
                if "import com.wolfool.relicwars.relic.FootprintTracker;" not in content:
                    content = content.replace("import com.wolfool.relicwars.relic.RelicDefinition;", "import com.wolfool.relicwars.relic.RelicDefinition;\nimport com.wolfool.relicwars.relic.FootprintTracker;")

            if orig != content:
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(content)

print("Final fixes applied.")
