import os
import re

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Replace getSanityCost call
content = content.replace("int sanityCost = getSanityCost(num);", "int sanityCost = getSanityCost(num, player);")

# Replace getSanityCost definition
lines = content.split('\n')
for i, line in enumerate(lines):
    if "private int getSanityCost(int relicNumber) {" in line:
        end_idx = i + 5
        new_lines = lines[:i] + [
            "    private int getSanityCost(int relicNumber, Player player) {",
            "        if (active001Omega.contains(player.getUniqueId())) return 0; // 태초의 별: 코스트 무시",
            "        if (relicNumber >= 20) return 0;",
            "        if (relicNumber >= 11) return 10;",
            "        if (relicNumber >= 6) return 20;",
            "        if (relicNumber >= 1) return 30;",
            "        return 0;",
            "    }"
        ] + lines[end_idx+2:] # +2 to skip the original lines + 'return 0;'
        with open(filepath, "w", encoding="utf-8") as f:
            f.write('\n'.join(new_lines))
        print("Updated getSanityCost")
        break
else:
    print("Failed to find getSanityCost definition")
