import os
import re

filepath = "src/main/java/com/wolfool/relicwars/combat/CombatListener.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Fallback using regex because of potential mojibake
lines = content.split('\n')
for i, line in enumerate(lines):
    if "private void handleDownedDamage(EntityDamageEvent event, Player victim) {" in line:
        end_idx = i + 10
        new_lines = lines[:i+1] + [
            "        // 보이드 추락은 무적 및 환경 면역을 무시하고 무조건 최종 사망 처리 (무한 추락 방지)",
            "        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {",
            "            event.setCancelled(true);",
            "            combatManager.killPlayer(victim, null);",
            "            return;",
            "        }",
            ""
        ] + lines[i+1:]
        with open(filepath, "w", encoding="utf-8") as f:
            f.write('\n'.join(new_lines))
        print("Fixed void fall bug.")
        break
else:
    print("Could not find insertion point.")
