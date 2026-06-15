import os

filepath = "src/main/java/com/wolfool/relicwars/manager/ConfigManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

parts = content.rsplit("}", 1)
new_content = parts[0] + "    public int getMaxRelicsPerTeam() { return plugin.getConfig().getInt(\"team.max-relics\", 3); }\n}\n"

with open(filepath, "w", encoding="utf-8") as f:
    f.write(new_content)
print("Added getMaxRelicsPerTeam")
