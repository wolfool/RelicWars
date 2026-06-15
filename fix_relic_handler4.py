import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "private void execute001(Player player) {" in line:
        new_lines.append(line)
        continue
        
    if "        }.runTaskTimer(plugin, 0L, 40L);" in line:
        new_lines.append(line)
        new_lines.append("    }\n")
        skip = True
        continue
        
    if skip:
        if "    // ======================== 유틸리티 ========================" in line:
            skip = False
            new_lines.append(line)
        continue
        
    new_lines.append(line)

with open(filepath, "w", encoding="utf-8") as f:
    f.writelines(new_lines)
print("Deleted dangling execute001 lines")
