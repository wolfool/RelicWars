import os

filepath = "src/main/java/com/wolfool/relicwars/combat/CombatListener.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """        // 무적 시간 중이면 무조건 캔슬
        if (combatManager.isInvincible(victim)) {
            event.setCancelled(true);
            return;
        }

        // 환경 데미지 무시 처리 (용암, 낙하 등)
        if (plugin.getConfigManager().isDownedEnvironmentalImmunity()) {"""

replacement = """        // 무적 시간 중이면 무조건 캔슬
        if (combatManager.isInvincible(victim)) {
            // 단, 보이드 추락은 무적을 무시하고 즉시 최종 사망 처리 (무한 추락 방지)
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
                combatManager.killPlayer(victim, null);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        // 보이드 추락은 환경 면역을 무시하고 무조건 최종 사망 처리
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            combatManager.killPlayer(victim, null);
            return;
        }

        // 환경 데미지 무시 처리 (용암, 낙하 등)
        if (plugin.getConfigManager().isDownedEnvironmentalImmunity()) {"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Fixed downed void damage bug.")
else:
    print("Could not find target block.")
