import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """    private void execute025(Player player) {
        player.sendMessage("§5[RelicWars] 30초간 구조 시간이 2초로 대폭 단축됩니다!");
        
        UUID id = player.getUniqueId();
        active025FastRevive.add(id);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active025FastRevive.remove(id);
            if (player.isOnline()) player.sendMessage("§c[RelicWars] 구조 단축 효과가 종료되었습니다.");
        }, 600L); // 30초
    }"""

replacement = """    private void execute025(Player player) {
        player.sendMessage("§5[RelicWars] 30초간 구조 시간이 2초로 대폭 단축되며, 주변 15블록의 적에게 실명을 부여합니다!");
        
        UUID id = player.getUniqueId();
        active025FastRevive.add(id);
        
        // 반경 15블록 적에게 30초 실명 부여
        for (org.bukkit.entity.Entity e : player.getNearbyEntities(15, 15, 15)) {
            if (e instanceof Player target && !target.equals(player)) {
                if (!plugin.getTeamManager().isSameTeam(player, target)) {
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 600, 0, false, false));
                    target.sendMessage("§8[RelicWars] 누군가의 최후의 봉합으로 인해 시야가 멀어집니다...");
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active025FastRevive.remove(id);
            if (player.isOnline()) player.sendMessage("§c[RelicWars] 구조 단축 효과가 종료되었습니다.");
        }, 600L); // 30초
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added blindness to #025")
else:
    # Try regex fallback if encoding mismatch
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if "private void execute025(Player player) {" in line:
            end_idx = i + 11
            new_lines = lines[:i+1] + [
                "        player.sendMessage(\"§5[RelicWars] 30초간 구조 시간이 2초로 대폭 단축되며, 주변 15블록의 적에게 실명을 부여합니다!\");",
                "        ",
                "        java.util.UUID id = player.getUniqueId();",
                "        active025FastRevive.add(id);",
                "        ",
                "        // 반경 15블록 적에게 30초 실명 부여",
                "        for (org.bukkit.entity.Entity e : player.getNearbyEntities(15, 15, 15)) {",
                "            if (e instanceof Player target && !target.equals(player)) {",
                "                if (!plugin.getTeamManager().isSameTeam(player, target)) {",
                "                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 600, 0, false, false));",
                "                    target.sendMessage(\"§8[RelicWars] 누군가의 스킬로 인해 시야가 멀어집니다...\");",
                "                }",
                "            }",
                "        }",
                "",
                "        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {",
                "            active025FastRevive.remove(id);",
                "            if (player.isOnline()) player.sendMessage(\"§c[RelicWars] 구조 단축 효과가 종료되었습니다.\");",
                "        }, 600L); // 30초",
                "    }"
            ] + lines[end_idx+1:]
            with open(filepath, "w", encoding="utf-8") as f:
                f.write('\n'.join(new_lines))
            print("Added using fallback regex-like search.")
            break
    else:
        print("Failed to find execute025 target")
