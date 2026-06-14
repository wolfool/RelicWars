package com.wolfool.relicwars.event;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import org.bukkit.Bukkit;

/**
 * 게임 내의 각종 이벤트(블러드문, 소문 등) 스케줄러를 관리합니다.
 */
public class EventManager implements Manager {

    private final RelicWars plugin;
    private BloodMoonTask bloodMoonTask;
    private ForgottenRelicTask forgottenRelicTask;

    public EventManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        // 블러드문: 게임 틱(시간) 감지 스케줄러 (매 1분(1200틱)마다 체크)
        bloodMoonTask = new BloodMoonTask(plugin);
        bloodMoonTask.runTaskTimer(plugin, 20L, 1200L); // 1분 간격

        // 방치된 유물 소문 스케줄러 (매 1분마다 체크)
        if (plugin.getConfigManager().isForgottenRelicsEnabled()) {
            forgottenRelicTask = new ForgottenRelicTask(plugin);
            forgottenRelicTask.runTaskTimerAsynchronously(plugin, 60L, 1200L); // 비동기로 1분 간격 체크
        }

        // 겉날개/엔드시티 차단 리스너
        Bukkit.getPluginManager().registerEvents(new EnvironmentListener(plugin), plugin);

        plugin.getLogger().info("§a[RelicWars] EventManager 초기화 완료.");
    }

    @Override
    public void shutdown() {
        if (bloodMoonTask != null) bloodMoonTask.cancel();
        if (forgottenRelicTask != null) forgottenRelicTask.cancel();
        plugin.getLogger().info("§a[RelicWars] EventManager 종료.");
    }
}
