import os

filepath = "src/main/java/com/wolfool/relicwars/relic/RelicManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """    public int countPlayerRelics(Player player) {"""

replacement = """    public boolean hasRelic(Player player, int relicNumber) {
        return getPlayerRelicNumbers(player).contains(relicNumber);
    }

    public int countPlayerRelics(Player player) {"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added hasRelic to RelicManager")
else:
    print("Failed to find target in RelicManager")

# Now fix RelicAbilityHandler
filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Add cooldown map
target2 = """    public final Set<UUID> active005DamageReduction = new HashSet<>();"""
replacement2 = """    public final Set<UUID> active005DamageReduction = new HashSet<>();
    private final java.util.Map<UUID, Long> cooldown005 = new java.util.HashMap<>();"""
content = content.replace(target2, replacement2)

# Fix cooldown usage
target3 = """        long next = plugin.getRelicManager().getCooldownManager().getCooldownUntil(id, 5);"""
replacement3 = """        long next = cooldown005.getOrDefault(id, 0L);"""
content = content.replace(target3, replacement3)

target4 = """        plugin.getRelicManager().getCooldownManager().setCooldown(id, 5, 600);"""
replacement4 = """        cooldown005.put(id, System.currentTimeMillis() + 600000L);"""
content = content.replace(target4, replacement4)

# Fix generic max health
target5 = """player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)"""
replacement5 = """player.getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_MAX_HEALTH"))"""
content = content.replace(target5, replacement5)

# Fix duplicate execute005
target6 = """    private void execute005(Player player) {
        player.sendMessage("§c[불멸의 심장] 이 유물은 직접 사용하는 것이 아닌, 소지 시 자동으로 발동되는 최상급 패시브입니다.");
    }"""
# Only keep the first one or just remove it if it's there twice. Wait, maybe it's there twice because of my previous multi_replace?
# I'll just remove the second occurrence.

parts = content.split("private void execute005(Player player) {")
if len(parts) > 2:
    # It appears multiple times! 
    # The first one is from my trigger005ImmortalHeart multi_replace.
    # The second one is from before.
    print("Removing duplicate execute005")
    # Actually, I can just replace the whole duplicate execute005 block with nothing.
    content = content.replace(target6, "")
    # Add one back
    content += "\n" + target6 + "\n"
else:
    print("execute005 is not duplicate in this way, or only 1 exists.")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("Fixed RelicAbilityHandler cooldowns, generic max health, and execute005")
