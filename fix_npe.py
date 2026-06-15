import os

# Fix getAttribute NPE in CombatManager.java
filepath = "src/main/java/com/wolfool/relicwars/combat/CombatManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

old = "player.setHealth(Math.min(reviveHp, player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));"
new = "player.setHealth(Math.min(reviveHp, java.util.Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue()));"

if old in content:
    content = content.replace(old, new)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("[CombatManager] getAttribute NPE 보호 완료")
else:
    print("[CombatManager] 실패")

# Fix getAttribute NPE in CombatListener.java
filepath2 = "src/main/java/com/wolfool/relicwars/combat/CombatListener.java"
with open(filepath2, "r", encoding="utf-8") as f:
    content2 = f.read()

old2 = "player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());"
new2 = "player.setHealth(java.util.Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue());"

if old2 in content2:
    content2 = content2.replace(old2, new2)
    with open(filepath2, "w", encoding="utf-8") as f:
        f.write(content2)
    print("[CombatListener] getAttribute NPE 보호 완료")
else:
    print("[CombatListener] 실패")
