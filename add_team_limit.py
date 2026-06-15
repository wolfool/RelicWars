import os

filepath = "src/main/java/com/wolfool/relicwars/relic/SealedRelicManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """                    if (plugin.getRelicManager().countPlayerRelics(player) >= plugin.getConfigManager().getMaxRelicsPerPlayer()) {
                        player.sendMessage("§c[RelicWars] 유물 소지 한도를 초과했습니다.");
                        return;
                    }"""

replacement = """                    if (plugin.getRelicManager().countPlayerRelics(player) >= plugin.getConfigManager().getMaxRelicsPerPlayer()) {
                        player.sendMessage("§c[RelicWars] 유물 소지 한도를 초과했습니다.");
                        return;
                    }
                    if (plugin.getTeamManager().hasTeam(player)) {
                        String teamId = plugin.getTeamManager().getTeamId(player);
                        if (plugin.getTeamManager().getTeamRelicCount(teamId) >= plugin.getConfigManager().getMaxRelicsPerTeam()) {
                            player.sendMessage("§c[RelicWars] 팀 유물 소지 한도를 초과했습니다.");
                            return;
                        }
                    }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added team limit check.")
else:
    print("Target not found.")
