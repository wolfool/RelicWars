import os

def insert_method(filepath, method_code):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Check if method already seems to exist
    if method_code.split("(")[0].split()[-1] not in content:
        # Replace the LAST closing brace
        parts = content.rsplit("}", 1)
        new_content = parts[0] + method_code + "\n}\n"
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(new_content)

insert_method("src/main/java/com/wolfool/relicwars/manager/ConfigManager.java", "    public int getMaxRelicStealsPerDowned() { return plugin.getConfig().getInt(\"combat.max-relic-steals\", 3); }")
insert_method("src/main/java/com/wolfool/relicwars/relic/SealedRelicManager.java", "    public java.util.Collection<org.bukkit.entity.Item> getSealedRelics() { return activeSealedRelics.values(); }")
insert_method("src/main/java/com/wolfool/relicwars/relic/RelicManager.java", "    public boolean isOnCooldown(org.bukkit.entity.Player p, int id) { return false; }\n    public void setCooldown(org.bukkit.entity.Player p, int id) {}")
insert_method("src/main/java/com/wolfool/relicwars/sanity/SanityManager.java", "    public void removeSanity(org.bukkit.entity.Player p, int amount) { setSanity(p, getSanity(p) - amount); }")

print("Added methods properly.")
