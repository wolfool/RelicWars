import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """    private void execute005(Player player) {
        player.sendMessage("§4[불멸의 심장] 패시브 효과가 발동합니다. (다운 시 체력 회복 후 무적 1초)");
    }"""

replacement = """    // #005 불멸의 심장 (자동 발동)
    public boolean trigger005ImmortalHeart(Player player) {
        java.util.UUID id = player.getUniqueId();
        
        // 쿨다운 확인 (10분 = 600초)
        long now = System.currentTimeMillis();
        long next = plugin.getRelicManager().getCooldownManager().getCooldownUntil(id, 5);
        if (now < next) {
            return false; // 아직 쿨타임
        }
        
        // 정신력 확인 (코스트 30)
        if (!plugin.getSanityManager().consumeSanity(player, 30)) {
            player.sendMessage("§c[불멸의 심장] 정신력이 부족하여 불멸의 심장이 반응하지 않았습니다!");
            return false; // 정신력 부족
        }
        
        // 쿨타임 적용 (600초)
        plugin.getRelicManager().getCooldownManager().setCooldown(id, 5, 600);
        
        // 부활 처리
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()); // 풀피
        player.sendMessage("§c[불멸의 심장] 죽음을 거부하고 다시 일어섭니다!");
        
        // 주변 넉백 및 번개
        for (org.bukkit.entity.Entity e : player.getNearbyEntities(8, 8, 8)) {
            if (!(e instanceof Player p)) continue;
            if (!plugin.getTeamManager().isSameTeam(player, p)) {
                org.bukkit.util.Vector kb = p.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5).setY(0.5);
                p.setVelocity(kb);
            }
        }
        
        org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text("§6[경고] 누군가 불멸의 심장을 통해 부활했습니다! (" + player.getLocation().getBlockX() + ", ?, " + player.getLocation().getBlockZ() + ")"));
        
        // 데미지 50% 감소
        active005DamageReduction.add(id);
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active005DamageReduction.remove(id);
            if (player.isOnline()) player.sendMessage("§c[불멸의 심장] 데미지 감소 효과가 종료되었습니다.");
        }, 160L);
        
        player.getWorld().strikeLightningEffect(player.getLocation());
        return true;
    }

    private void execute005(Player player) {
        player.sendMessage("§c[불멸의 심장] 이 유물은 직접 사용하는 것이 아닌, 소지 시 자동으로 발동되는 최상급 패시브입니다.");
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added trigger005ImmortalHeart")
else:
    print("Failed to find target for execute005")
