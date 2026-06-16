package com.wolfool.relicwars.integration;

import com.wolfool.relicwars.RelicWars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * BetterHud + BetterModel + CraftEngine 연동 관리자.
 * 각 플러그인이 서버에 없어도 에러 없이 동작합니다 (soft-depend).
 */
public class IntegrationManager {

    private final RelicWars plugin;
    private boolean betterHudEnabled = false;
    private boolean betterModelEnabled = false;
    private boolean craftEngineEnabled = false;

    private RelicHudPlaceholders hudPlaceholders;

    public IntegrationManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    /**
     * 연동 초기화 — onEnable 후반부에서 호출.
     * 각 플러그인의 존재 여부를 확인하고 훅을 등록합니다.
     */
    public void initialize() {
        // === BetterHud 연동 ===
        if (Bukkit.getPluginManager().isPluginEnabled("BetterHud")) {
            try {
                hudPlaceholders = new RelicHudPlaceholders(plugin);
                hudPlaceholders.register();
                betterHudEnabled = true;
                plugin.getLogger().info("§a[RelicWars] BetterHud 연동 완료! 커스텀 placeholder 등록됨.");
            } catch (Exception e) {
                plugin.getLogger().warning("[RelicWars] BetterHud 연동 실패: " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("§7[RelicWars] BetterHud 미감지 — HUD 연동 비활성화.");
        }

        // === BetterModel 연동 ===
        if (Bukkit.getPluginManager().isPluginEnabled("BetterModel")) {
            betterModelEnabled = true;
            plugin.getLogger().info("§a[RelicWars] BetterModel 연동 완료! 커스텀 모델 사용 가능.");
        } else {
            plugin.getLogger().info("§7[RelicWars] BetterModel 미감지 — 바닐라 모델 사용.");
        }

        // === CraftEngine 연동 ===
        if (Bukkit.getPluginManager().isPluginEnabled("CraftEngine")) {
            craftEngineEnabled = true;
            plugin.getLogger().info("§a[RelicWars] CraftEngine 연동 완료! 커스텀 아이템 텍스처 사용 가능.");
        } else {
            plugin.getLogger().info("§7[RelicWars] CraftEngine 미감지 — 바닐라 텍스처 사용.");
        }
    }

    public void shutdown() {
        if (hudPlaceholders != null) {
            hudPlaceholders.unregister();
        }
    }

    // === Getters ===
    public boolean isBetterHudEnabled() { return betterHudEnabled; }
    public boolean isBetterModelEnabled() { return betterModelEnabled; }
    public boolean isCraftEngineEnabled() { return craftEngineEnabled; }
}
