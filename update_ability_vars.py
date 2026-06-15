import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if "public final Set<UUID> active025FastRevive" in line:
        new_lines = lines[:i] + [
            "    public final java.util.Map<java.util.UUID, Long> cooldown005 = new java.util.HashMap<>(); // #005 쿨타임 (90분)",
            "    public final java.util.Set<java.util.UUID> active005DamageReduction = new java.util.HashSet<>(); // #005 데미지 감소 50% 상태",
            "    public final java.util.Set<java.util.UUID> active001Omega = new java.util.HashSet<>(); // #001 태초의 지배자 상태",
            ""
        ] + lines[i:]
        with open(filepath, "w", encoding="utf-8") as f:
            f.write('\n'.join(new_lines))
        print("Added variables for #005 and #001")
        break
else:
    print("Failed to find variable insertion point")
