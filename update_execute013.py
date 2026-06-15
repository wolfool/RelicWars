import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if "private void execute013(Player player) {" in line:
        end_idx = i + 21
        new_lines = lines[:i] + [
            "    private void execute013(Player player) {",
            "        player.sendMessage(\"§4[탐욕의 눈] 60초간 서버 내 다른 플레이어들의 유물 보유량과 위치를 스캔합니다...\");",
            "        org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text(\"§c[경고] 누군가 탐욕의 눈을 떴습니다... 모든 비밀이 꿰뚫어집니다.\"));",
            "",
            "        new org.bukkit.scheduler.BukkitRunnable() {",
            "            int ticks = 0;",
            "            @Override",
            "            public void run() {",
            "                if (!player.isOnline() || ticks >= 1200) { this.cancel(); return; } // 60초",
            "                ticks += 100; // 5초마다 핑",
            "",
            "                player.sendMessage(\"§c== [탐욕의 눈 스캔 결과] ==\");",
            "                boolean found = false;",
            "                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {",
            "                    if (p.equals(player)) continue;",
            "                    ",
            "                    int relicCount = plugin.getRelicManager().countPlayerRelics(p);",
            "                    if (relicCount > 0) {",
            "                        found = true;",
            "                        int x = p.getLocation().getBlockX() + (int)(Math.random() * 20 - 10);",
            "                        int z = p.getLocation().getBlockZ() + (int)(Math.random() * 20 - 10);",
            "                        player.sendMessage(\"§c- 대상: §f\" + p.getName() + \" §c| 유물 수: §e\" + relicCount + \"개 §c| 위치: §7(~\" + x + \", ?, ~\" + z + \")\");",
            "                        ",
            "                        p.sendMessage(\"§8[섬뜩함] 탐욕스러운 시선이 당신의 등 뒤를 훑고 지나갑니다...\");",
            "                        ",
            "                        org.bukkit.util.Vector dir = p.getLocation().toVector().subtract(player.getLocation().toVector());",
            "                        if (dir.lengthSquared() > 0) dir = dir.normalize();",
            "                        player.spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1.5, 0).add(dir.multiply(2)), 10, 0.1, 0.1, 0.1, 0.05);",
            "                    }",
            "                }",
            "                if (!found) player.sendMessage(\"§7현재 서버 내에 유물을 소지한 다른 플레이어가 없습니다.\");",
            "            }",
            "        }.runTaskTimer(plugin, 0L, 100L);",
            "    }"
        ] + lines[end_idx+1:]
        with open(filepath, "w", encoding="utf-8") as f:
            f.write('\n'.join(new_lines))
        print("Updated execute013")
        break
else:
    print("Failed to find execute013 target")
