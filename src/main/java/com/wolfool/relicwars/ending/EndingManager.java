package com.wolfool.relicwars.ending;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * 게임 엔딩 조건(모든 유물 등장 + 특정 팀 유물 10개 보유)을 체크하고
 * 엔딩 제단 방어 이벤트(30분)를 관리하는 매니저입니다.
 */
public class EndingManager implements Manager {

    private final RelicWars plugin;
    private BukkitTask endingCheckTask;
    private boolean isEndingTriggered = false;

    public EndingManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        startEndingCheckTask();
        plugin.getLogger().info("§a[RelicWars] EndingManager 초기화 완료.");
    }

    @Override
    public void shutdown() {
        if (endingCheckTask != null) endingCheckTask.cancel();
        plugin.getLogger().info("§a[RelicWars] EndingManager 종료.");
    }

    private void startEndingCheckTask() {
        endingCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (isEndingTriggered) {
                    this.cancel();
                    return;
                }

                // 조건 1: 30개 유물이 모두 스폰되었는가? (Config에서 끌 수도 있음)
                boolean requiredAllSpawned = plugin.getConfigManager().isEndingRequiredAllSpawned();
                if (requiredAllSpawned && !plugin.getRelicManager().areAllRelicsSpawned()) {
                    return; // 아직 모든 유물이 등장하지 않음
                }

                // 조건 2: 한 팀이 10개(설정값) 이상의 유물을 모았는가?
                int requiredRelics = plugin.getConfigManager().getEndingRequiredTeamRelics();
                boolean teamMetCondition = false;
                
                // 모든 유저를 순회하며 팀의 유물 개수를 검사
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    String teamId = plugin.getTeamManager().getTeamId(p);
                    int teamRelics;
                    if (teamId != null) {
                        teamRelics = plugin.getTeamManager().getTeamRelicCount(teamId);
                    } else {
                        teamRelics = plugin.getRelicManager().countPlayerRelics(p); // 솔로일 경우 개인 유물 수
                    }

                    if (teamRelics >= requiredRelics) {
                        teamMetCondition = true;
                        break;
                    }
                }

                if (teamMetCondition) {
                    triggerEndingAltar();
                }
            }
        }.runTaskTimer(plugin, 600L, 1200L); // 1분마다 체크
    }

    private void triggerEndingAltar() {
        isEndingTriggered = true;
        
        int defenseMinutes = plugin.getConfigManager().getAltarDefenseMinutes();
        
        // 제단 위치: 월드 스폰 포인트 기준
        org.bukkit.Location altarLoc = Bukkit.getWorlds().get(0).getSpawnLocation().clone().add(0, 1, 0);

        Bukkit.broadcast(Component.text("§5============================================="));
        Bukkit.broadcast(Component.text("§d[엔딩] §f최종 보스 소환 조건이 충족되었습니다!"));
        Bukkit.broadcast(Component.text("§e엔딩 제단이 월드 스폰 포인트에 모습을 드러냈습니다!"));
        Bukkit.broadcast(Component.text("§e좌표: X:" + (int) altarLoc.getX() + " Y:" + (int) altarLoc.getY() + " Z:" + (int) altarLoc.getZ()));
        Bukkit.broadcast(Component.text("§c" + defenseMinutes + "분 동안 §e제단을 방어하는 자에게 #000 왕좌의 핵이 주어집니다."));
        Bukkit.broadcast(Component.text("§5============================================="));

        // 제단 시각 표시 (비콘 빔 효과 - 발광 아머스탠드)
        org.bukkit.entity.ArmorStand beacon = altarLoc.getWorld().spawn(altarLoc, org.bukkit.entity.ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCustomName("§6✦ 엔딩 제단 ✦");
            stand.setCustomNameVisible(true);
            stand.setGlowing(true);
            stand.setMarker(true);
        });

        // 30분 방어 타이머
        final int totalSeconds = defenseMinutes * 60;
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                elapsed += 60; // 1분 단위
                int remaining = totalSeconds - elapsed;

                if (remaining <= 0) {
                    // 방어 성공! #000 유물 스폰
                    this.cancel();
                    beacon.remove();
                    onDefenseSuccess(altarLoc);
                    return;
                }

                int minutesLeft = remaining / 60;
                
                // 5분마다 또는 마지막 3분 매분 알림
                if (minutesLeft % 5 == 0 || minutesLeft <= 3) {
                    Bukkit.broadcast(Component.text("§e[엔딩 제단] §f방어 남은 시간: §c" + minutesLeft + "분"));
                }

                // 제단 주변에 파티클 (시각 효과)
                altarLoc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, altarLoc.clone().add(0, 3, 0), 30, 2, 3, 2, 0.05);
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // 1분마다
    }

    private void onDefenseSuccess(org.bukkit.Location altarLoc) {
        Bukkit.broadcast(Component.text("§6============================================="));
        Bukkit.broadcast(Component.text("§6[엔딩] §e방어 성공! §f#000 왕좌의 핵이 모습을 드러냅니다!"));
        Bukkit.broadcast(Component.text("§6============================================="));

        // #000 왕좌의 핵 드랍 (봉인 60초)
        com.wolfool.relicwars.relic.RelicDefinition def000 = com.wolfool.relicwars.relic.RelicDefinition.getByNumber(0);
        if (def000 != null) {
            org.bukkit.inventory.ItemStack relic000 = com.wolfool.relicwars.relic.RelicItemUtil.createRelicItem(def000);
            plugin.getSealedRelicManager().spawnSealedRelic(altarLoc, relic000, 60);
        } else {
            // fallback: RelicDefinition에 #000이 없을 경우 드래곤 에그로 대체
            org.bukkit.inventory.ItemStack dragonEgg = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DRAGON_EGG);
            org.bukkit.inventory.meta.ItemMeta meta = dragonEgg.getItemMeta();
            meta.displayName(Component.text("§6✦ #000 왕좌의 핵 ✦"));
            dragonEgg.setItemMeta(meta);
            altarLoc.getWorld().dropItemNaturally(altarLoc, dragonEgg);
        }

        // 획득한 팀이 30분 방어에 성공하면 시즌 종료
        // (실제 구현에서는 #000을 가진 팀이 추가 30분 방어를 버티면 승리)
        Bukkit.broadcast(Component.text("§e[엔딩] §f이 유물을 주워 30분간 지켜내면 시즌이 종료됩니다!"));
    }
}
