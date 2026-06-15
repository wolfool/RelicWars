import sys

file_path = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"

with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
    content = f.read()

# Replace the specific block in execute012
old_block = """        victim.sendMessage("§4[약탈자의 장갑] 누군가 당신의 정신력을 강탈했습니다!");
        player.sendMessage("§a[약탈자의 장갑] " + victim.getName() + "의 정신력을 30 강탈했습니다!");
        // TODO: 정신력(Sanity) 시스템 연동"""

new_block = """        int stolenSanity = Math.min(30, plugin.getSanityManager().getSanity(victim));
        plugin.getSanityManager().setSanity(victim, plugin.getSanityManager().getSanity(victim) - stolenSanity);
        plugin.getSanityManager().restoreSanity(player, stolenSanity);

        victim.sendMessage("§4[약탈자의 장갑] 누군가 당신의 정신력을 " + stolenSanity + " 강탈했습니다!");
        player.sendMessage("§a[약탈자의 장갑] " + victim.getName() + "의 정신력을 " + stolenSanity + " 강탈했습니다!");"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("Replaced successfully!")
else:
    print("Old block not found!")
