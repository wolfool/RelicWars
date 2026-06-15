import os

filepath = "src/main/java/com/wolfool/relicwars/combat/CombatManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """                        player.sendMessage("§c[RelicWars] 구조 시간이 만료되었습니다.");
                        com.wolfool.relicwars.relic.InteractionEffects.playAutoExecuteEffect(player);
                        killPlayer(player, null);
                        return;"""

replacement = """                        player.sendMessage("§c[RelicWars] 구조 시간이 만료되었습니다.");
                        com.wolfool.relicwars.relic.InteractionEffects.playAutoExecuteEffect(player);
                        executePlayer(player, null);
                        return;"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Fixed 60s timeout to use executePlayer")
else:
    print("Failed to find 60s timeout target")
