import os

filepath = "src/main/java/com/wolfool/relicwars/relic/SealedRelicManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """        Long unsealTime = targetItem.getPersistentDataContainer().get(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG);
        if (unsealTime != null && unsealTime > 0) {"""

replacement = """        Long unsealTime = targetItem.getPersistentDataContainer().get(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG);
        
        // #001 태초의 별: 태초의 지배자(Omega) 발동 중이면 모든 봉인 타임 무시
        boolean isOmegaActive = !plugin.getRelicAbilityHandler().active001Omega.isEmpty();
        
        if (!isOmegaActive && unsealTime != null && unsealTime > 0) {"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Updated SealedRelicManager for #001 bypass")
else:
    print("Failed to find target in SealedRelicManager")
