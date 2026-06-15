import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if "public void execute020Option3(Player player) {" in line:
        # We need to insert after execute020Option3 ends
        end_idx = i + 17 # approx
        while "    }" not in lines[end_idx]:
            end_idx += 1
            
        new_lines = lines[:end_idx+1] + [
            "",
            "    public void execute020OptionRandom(Player player) {",
            "        player.sendMessage(\"§d[소문의 등불] §f무작위 가짜 소문을 퍼뜨립니다.\");",
            "        int x = (int)(Math.random() * 2000 - 1000);",
            "        int z = (int)(Math.random() * 2000 - 1000);",
            "        String dir = getCardinalDirection(new org.bukkit.util.Vector(x - player.getLocation().getX(), 0, z - player.getLocation().getZ()));",
            "        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(new org.bukkit.Location(player.getWorld(), x, 60, z), \"§b[소문] \" + dir + \"쪽에서 낯선 유물의 기운이 느껴집니다.\");",
            "    }",
            "",
            "    public final java.util.Set<java.util.UUID> active020PingMode = new java.util.HashSet<>();",
            "    public void execute020OptionPing(Player player) {",
            "        player.sendMessage(\"§d[소문의 등불] §f기만 전술 모드를 켭니다.\");",
            "        active020PingMode.add(player.getUniqueId());",
            "        player.sendMessage(\"§e  [정보] 5분 안에 채팅창에 가짜 소문을 낼 본인의 유물 번호(숫자)를 입력하세요.\");",
            "        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {",
            "            if (active020PingMode.remove(player.getUniqueId())) {",
            "                if (player.isOnline()) player.sendMessage(\"§c[소문의 등불] 기만 전술 시간이 만료되었습니다.\");",
            "            }",
            "        }, 6000L);",
            "    }"
        ] + lines[end_idx+1:]
        with open(filepath, "w", encoding="utf-8") as f:
            f.write('\n'.join(new_lines))
        print("Added OptionRandom and OptionPing")
        break
else:
    print("Failed to find execute020Option3")
