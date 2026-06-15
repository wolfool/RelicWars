import os

filepath = "src/main/java/com/wolfool/relicwars/combat/CombatManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """    public void setDowned(Player player) {"""

replacement = """    public void setDowned(Player player) {
        // #005 불멸의 심장 소지 여부 감지 및 발동
        if (plugin.getRelicManager().hasRelic(player, 5)) {
            if (plugin.getRelicAbilityHandler().trigger005ImmortalHeart(player)) {
                return; // 발동 성공 시 다운을 무시하고 빠져나감!
            }
        }
"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Intercepted setDowned with #005")
else:
    print("Failed to find setDowned target")
