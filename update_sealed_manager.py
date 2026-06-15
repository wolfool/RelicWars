import os

filepath = "src/main/java/com/wolfool/relicwars/relic/SealedRelicManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """        // #001 태초의 별: 오메가(Omega) 프로토콜 발동 중이면 모든 봉인 타이머 무시
        boolean isOmegaActive = !plugin.getRelicAbilityHandler().active001Omega.isEmpty();
        
        if (!isOmegaActive && unsealTime != null && unsealTime > 0) {"""

replacement = """        if (unsealTime != null && unsealTime > 0) {"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Fixed SealedRelicManager Omega unseal bypass")
else:
    print("Failed to find target in SealedRelicManager")
