import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if "private void execute005(Player player) {" in line:
        end_idx = i + 10
        new_lines = lines[:i+1] + [
            "        player.sendMessage(\"§c[불멸의 심장] 이 유물은 직접 사용하는 것이 아닌, 소지 시 자동으로 발동되는 최상급 패시브입니다.\");",
            "    }"
        ] + lines[end_idx+1:]
        with open(filepath, "w", encoding="utf-8") as f:
            f.write('\n'.join(new_lines))
        print("Updated execute005")
        break
else:
    print("Failed to find execute005")
