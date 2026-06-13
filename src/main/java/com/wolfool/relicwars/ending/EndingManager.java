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
        
        Bukkit.broadcast(Component.text("§5============================================="));
        Bukkit.broadcast(Component.text("§d[엔딩] §f최종 보스 소환 조건이 충족되었습니다!"));
        Bukkit.broadcast(Component.text("§e엔딩 제단이 모습을 드러냈습니다."));
        Bukkit.broadcast(Component.text("§c" + defenseMinutes + "분 동안 §e제단을 방어하는 자에게 최종 유물이 주어집니다."));
        Bukkit.broadcast(Component.text("§5============================================="));

        // TODO: 제단 좌표 생성 및 방어 30분 타이머 가동 (상세 구현은 Phase 5 심화 또는 보류)
        // 방어 성공 시 #000 유물 지급
    }
}
