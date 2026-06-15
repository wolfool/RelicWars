import os

filepath = "src/main/java/com/wolfool/relicwars/combat/CombatManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """    public void killPlayer(Player victim, Player attacker) {"""

replacement = """    public void executePlayer(Player victim, Player attacker) {
        clearDownedState(victim);
        victim.setHealth(0.0);
        victim.sendMessage("§c[RelicWars] 처형당했습니다. (추가 유물 드랍 없음)");
        org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c[처형] §f" + victim.getName() + "§c님이 처형당했습니다."));
    }

    public void killPlayer(Player victim, Player attacker) {"""

if target in content:
    content = content.replace(target, replacement)
    
    # Also change addExecuteHit to call executePlayer instead of killPlayer
    content = content.replace("""        if (currentHits >= requiredHits) {
            killPlayer(victim, attacker);
        }""", """        if (currentHits >= requiredHits) {
            executePlayer(victim, attacker);
        }""")
        
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Fixed executePlayer logic in CombatManager")
else:
    print("Failed to find killPlayer target")
