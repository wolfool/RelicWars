import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if "private void execute001(Player player) {" in line:
        end_idx = i + 19
        new_lines = lines[:i] + [
            "    private void execute001(Player player) {",
            "        org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text(\"§4========================================\"));",
            "        org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text(\"§e[경고] 태초의 지배자가 강림했습니다. 모든 봉인이 강제 해제되며, 모든 좌표가 실시간으로 노출됩니다.\"));",
            "        org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text(\"§4========================================\"));",
            "",
            "        java.util.UUID id = player.getUniqueId();",
            "        active001Omega.add(id);",
            "",
            "        new org.bukkit.scheduler.BukkitRunnable() {",
            "            int ticks = 0;",
            "            @Override",
            "            public void run() {",
            "                if (!player.isOnline() || ticks >= 1200) { ",
            "                    active001Omega.remove(id);",
            "                    org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text(\"§a[안내] 태초의 지배자 상태가 해제되었습니다.\"));",
            "                    this.cancel(); ",
            "                    return; ",
            "                }",
            "                ticks += 40; // 2초마다 브로드캐스트",
            "",
            "                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {",
            "                    if (p.equals(player)) continue;",
            "                    int x = p.getLocation().getBlockX();",
            "                    int y = p.getLocation().getBlockY();",
            "                    int z = p.getLocation().getBlockZ();",
            "                    int hp = (int) p.getHealth();",
            "                    int sanity = plugin.getSanityManager().getSanity(p);",
            "                    player.sendMessage(\"§e[태초의 눈] §f\" + p.getName() + \" §7| 위치: (\" + x + \", \" + y + \", \" + z + \") | 체력: \" + hp + \" | 정신력: \" + sanity);",
            "                }",
            "            }",
            "        }.runTaskTimer(plugin, 0L, 40L);",
            "    }"
        ] + lines[end_idx+1:]
        with open(filepath, "w", encoding="utf-8") as f:
            f.write('\n'.join(new_lines))
        print("Updated execute001")
        break
else:
    print("Failed to find execute001 target")
