import os

filepath = "src/main/java/com/wolfool/relicwars/relic/RelicListener.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerUseRelic(PlayerInteractEvent event) {"""

replacement = """    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLeftClick001(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            org.bukkit.entity.Player player = event.getPlayer();
            if (plugin.getRelicAbilityHandler().active001Omega.contains(player.getUniqueId())) {
                org.bukkit.inventory.ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem == null || handItem.getType() == org.bukkit.Material.AIR) {
                    // 빈 손 좌클릭 시 심판의 벼락
                    org.bukkit.block.Block targetBlock = player.getTargetBlockExact(100);
                    if (targetBlock != null) {
                        org.bukkit.Location loc = targetBlock.getLocation();
                        loc.getWorld().strikeLightningEffect(loc);
                        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
                            if (e instanceof org.bukkit.entity.Player target && !target.equals(player)) {
                                if (plugin.getTeamManager().isSameTeam(player, target)) continue;
                                target.setHealth(0.0);
                                target.sendMessage("§4[심판] 태초의 지배자가 내린 벼락에 맞아 즉사했습니다.");
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerUseRelic(PlayerInteractEvent event) {"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Successfully added left click listener for #001")
else:
    print("Could not find target in RelicListener")
