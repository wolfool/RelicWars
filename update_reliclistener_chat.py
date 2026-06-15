import os

filepath = "src/main/java/com/wolfool/relicwars/relic/RelicListener.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """        if (plugin.getRelicAbilityHandler().active020ScanMode.contains(player.getUniqueId())) {"""

replacement = """        if (plugin.getRelicAbilityHandler().active020PingMode.contains(player.getUniqueId())) {
            event.setCancelled(true);
            String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getRelicAbilityHandler().active020PingMode.remove(player.getUniqueId());
                try {
                    int targetNum = Integer.parseInt(msg);
                    if (!plugin.getRelicManager().hasRelic(player, targetNum)) {
                        player.sendMessage("§c[소문의 등불] 기만 전술은 본인이 소지한 유물의 번호만 사용할 수 있습니다!");
                        return;
                    }
                    
                    int x = (int)(Math.random() * 2000 - 1000);
                    int z = (int)(Math.random() * 2000 - 1000);
                    String dir = plugin.getRelicAbilityHandler().getCardinalDirection(new org.bukkit.util.Vector(x - player.getLocation().getX(), 0, z - player.getLocation().getZ()));
                    
                    com.wolfool.relicwars.relic.RelicDefinition def = com.wolfool.relicwars.relic.RelicDefinition.getByNumber(targetNum);
                    com.wolfool.relicwars.util.RumorUtil.broadcastRumor(new org.bukkit.Location(player.getWorld(), x, 60, z), "§b[소문] " + dir + "쪽에서 " + def.name() + "의 기운이 느껴집니다...");
                    player.sendMessage("§d[소문의 등불] 기만 전술 성공! 적들이 가짜 위치로 향할 것입니다.");
                    
                } catch (NumberFormatException e) {
                    player.sendMessage("§c[RelicWars] 숫자로만 입력해주세요.");
                }
            });
            return;
        }

        if (plugin.getRelicAbilityHandler().active020ScanMode.contains(player.getUniqueId())) {"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added active020PingMode to RelicListener")
else:
    print("Failed to find target in RelicListener for ping mode chat")
