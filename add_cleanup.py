import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

cleanup_method = """
    // ======================== 유틸리티 ========================
    
    public void cleanupPlayer(Player player) {
        java.util.UUID id = player.getUniqueId();
        active001Omega.remove(id);
        active005DamageReduction.remove(id);
        active029FallImmunity.remove(id);
        active027FireImmunity.remove(id);
        active025FastRevive.remove(id);
        active023Marked.remove(id);
        active021Duel.remove(id);
        active020ScanMode.remove(id);
        active015Casting.remove(id);
        active010EMP.remove(id);
        active008Shadow.remove(id);
        active006Leap.remove(id);
        active003TrackerWait.remove(id);
        
        java.util.Iterator<Map.Entry<java.util.UUID, java.util.UUID>> it = active021Duel.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<java.util.UUID, java.util.UUID> entry = it.next();
            if (entry.getValue().equals(id)) {
                it.remove();
            }
        }
    }
"""

if "public String getCardinalDirection(Vector dir)" in content:
    content = content.replace("    // ======================== 유틸리티 ========================", cleanup_method)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added cleanupPlayer to RelicAbilityHandler.java")
else:
    print("Could not find insertion point in RelicAbilityHandler")

filepath = "src/main/java/com/wolfool/relicwars/combat/CombatListener.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = "combatManager.killPlayer(player, null);"
replacement = "combatManager.killPlayer(player, null);\n        plugin.getRelicAbilityHandler().cleanupPlayer(player);"
target2 = "// 랜뽑 (전투 태그 중 강제 종료) 처리"
replacement2 = "plugin.getRelicAbilityHandler().cleanupPlayer(player);\n            // 랜뽑 (전투 태그 중 강제 종료) 처리"

if target in content and target2 in content:
    content = content.replace(target, replacement)
    content = content.replace(target2, replacement2)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added cleanupPlayer call to CombatListener.java")
else:
    print("Could not find targets in CombatListener")
