import os

# Fix RelicAbilityHandler
with open("src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java", "r", encoding="utf-8") as f:
    rah = f.read()

if "import java.util.Queue;" not in rah:
    rah = rah.replace("import java.util.Map;", "import java.util.Map;\nimport java.util.Queue;")

rah = rah.replace("plugin.getSealedRelicManager().getSealedRelics()", "plugin.getSealedRelicManager().getActiveSealedRelics()")

with open("src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java", "w", encoding="utf-8") as f:
    f.write(rah)

print("Fixed.")
