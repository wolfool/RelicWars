import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    lines = f.readlines()

# delete 1313-1322 (0-indexed 1312-1321)
# let's be safe and verify content
start_idx = -1
for i, line in enumerate(lines):
    if "private void execute005(Player player) {" in line:
        start_idx = i + 3
        break

if start_idx != -1 and "        for (Entity e : player.getNearbyEntities(8, 8, 8)) {" in lines[start_idx]:
    end_idx = start_idx
    while "    }" not in lines[end_idx]:
        end_idx += 1
    
    new_lines = lines[:start_idx] + lines[end_idx+1:]
    with open(filepath, "w", encoding="utf-8") as f:
        f.writelines(new_lines)
    print("Deleted dangling execute005 lines")
else:
    print("Failed to find dangling execute005 lines")
