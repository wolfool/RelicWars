import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "private void execute005(Player player) {" in line:
        new_lines.append(line)
        continue
        
    if "player.sendMessage(\"§c[불멸의 심장] 이 유물은 직접 사용하는 것이 아닌, 소지 시 자동으로 발동되는 최상급 패시브입니다.\");" in line:
        new_lines.append(line)
        new_lines.append("    }\n")
        skip = True
        continue
        
    if skip:
        if "    // ======================== Batch 6: #004 ~ #001 ========================" in line:
            skip = False
            new_lines.append(line)
        continue
        
    new_lines.append(line)

with open(filepath, "w", encoding="utf-8") as f:
    f.writelines(new_lines)
print("Deleted more dangling lines")
