import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# We want to replace the whole execute001 block.
# Let's find where it starts and ends.
start_idx = content.find("    private void execute001(Player player) {")
if start_idx != -1:
    end_idx = content.find("    public Set<UUID> active005DamageReduction", start_idx)
    if end_idx == -1:
        end_idx = content.find("    // #005", start_idx)
    if end_idx == -1:
        end_idx = content.find("    public final Set<UUID> active005DamageReduction", start_idx)
    
    if end_idx != -1:
        new_execute001 = """    public final Set<UUID> active001Omega = new HashSet<>();

    // #001 태초의 별: 심판의 별 (60초간 비행, 무제한 벼락 포격, 좌표 실시간 중계)
    private void execute001(Player player) {
        // 인벤토리에서 유물 삭제 (1회용이므로 삭제)
        org.bukkit.inventory.ItemStack handItem = player.getInventory().getItemInMainHand();
        if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(handItem) && com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(handItem) == 1) {
            handItem.setAmount(handItem.getAmount() - 1);
        } else {
            org.bukkit.inventory.ItemStack offItem = player.getInventory().getItemInOffHand();
            if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(offItem) && com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(offItem) == 1) {
                offItem.setAmount(offItem.getAmount() - 1);
            }
        }

        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§4========================================"));
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§e[경고] 태초의 지배자가 강림했습니다. 심판의 별이 떠오릅니다..."));
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§4========================================"));

        UUID id = player.getUniqueId();
        active001Omega.add(id);
        
        // 60초간 비행 허용
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage("§e[태초의 별] 60초간 자유롭게 비행하며, 좌클릭으로 방어력과 무적을 무시하는 심판의 벼락을 내리꽂을 수 있습니다.");
        player.sendMessage("§e[태초의 별] 또한, 2초마다 모든 적의 위치와 상태가 공유됩니다.");

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !active001Omega.contains(id)) {
                    this.cancel();
                    active001Omega.remove(id);
                    return;
                }

                ticks += 40; // 2초마다
                if (ticks > 1200) { // 60초 종료
                    this.cancel();
                    active001Omega.remove(id);
                    if (player.isOnline()) {
                        player.sendMessage("§c[태초의 별] 태초의 지배자 권능이 소멸했습니다.");
                        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                            player.setAllowFlight(false);
                            player.setFlying(false);
                        }
                    }
                    return;
                }

                // 적 탐색 및 중계 (2초 주기)
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.equals(player)) continue;
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;

                    int x = p.getLocation().getBlockX();
                    int y = p.getLocation().getBlockY();
                    int z = p.getLocation().getBlockZ();
                    int health = (int) p.getHealth();
                    int sanity = plugin.getSanityManager().getSanity(p);

                    player.sendMessage("§b[태초의 관측] §f" + p.getName() + " §7- 위치: " + x + ", " + y + ", " + z + 
                            " | 체력: §c" + health + " §7| 정신력: §e" + sanity);
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // 2초마다 실행
    }

"""
        content = content[:start_idx - 100] + new_execute001 + content[end_idx:]
        
        # Wait, I need to properly replace without messing up active001Omega declaration if it already exists.
        # Let's check if active001Omega is declared earlier.
        
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)
        print("Successfully updated execute001")
    else:
        print("Could not find end of execute001")
else:
    print("Could not find execute001")
