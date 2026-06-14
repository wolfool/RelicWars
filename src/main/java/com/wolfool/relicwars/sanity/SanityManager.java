package com.wolfool.relicwars.sanity;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 정신력(Sanity) 시스템 매니저.
 * - 기본 100, 유물 사용 시 10~30 소모
 * - 분당 자연 회복 (config)
 * - 낮으면 디버프 (환각/구속/독)
 * - 금사과로 회복 가능
 */
public class SanityManager implements Manager {

    private final RelicWars plugin;
    private final Map<UUID, Integer> sanityMap = new HashMap<>();
    private BukkitTask regenTask;
    private BukkitTask debuffTask;

    public SanityManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        loadAllFromDB();
        startRegenTask();
        startDebuffTask();
        org.bukkit.Bukkit.getPluginManager().registerEvents(new SanityListener(plugin), plugin);
        plugin.getLogger().info("§a[RelicWars] SanityManager 초기화 완료.");
    }

    @Override
    public void shutdown() {
        saveAllToDB();
        if (regenTask != null) regenTask.cancel();
        if (debuffTask != null) debuffTask.cancel();
        plugin.getLogger().info("§a[RelicWars] SanityManager 종료.");
    }

    // ======================== 핵심 API ========================

    public int getSanity(Player player) {
        return sanityMap.getOrDefault(player.getUniqueId(), plugin.getConfigManager().getSanityMax());
    }

    public void setSanity(Player player, int value) {
        int max = plugin.getConfigManager().getSanityMax();
        int clamped = Math.max(0, Math.min(value, max));
        sanityMap.put(player.getUniqueId(), clamped);
    }

    /**
     * 정신력을 소모합니다. 부족하면 false를 반환합니다.
     */
    public boolean consumeSanity(Player player, int amount) {
        int current = getSanity(player);
        if (current < amount) {
            player.sendMessage("§c[RelicWars] 정신력이 부족합니다! (필요: " + amount + ", 현재: " + current + ")");
            return false;
        }
        setSanity(player, current - amount);
        player.sendActionBar(Component.text("§5정신력: " + getSanity(player) + " / " + plugin.getConfigManager().getSanityMax() +
                " (§c-" + amount + "§5)"));
        return true;
    }

    /**
     * 정신력을 회복합니다.
     */
    public void restoreSanity(Player player, int amount) {
        setSanity(player, getSanity(player) + amount);
        player.sendActionBar(Component.text("§d정신력 회복: " + getSanity(player) + " / " + plugin.getConfigManager().getSanityMax() +
                " (§a+" + amount + "§d)"));
    }

    // ======================== 자연 회복 ========================

    private void startRegenTask() {
        int regenAmount = plugin.getConfigManager().getSanityRegenPerMinute();
        regenTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plugin.getCombatManager().isDowned(p)) continue; // 다운 상태에선 회복 안됨
                    
                    int current = getSanity(p);
                    int max = plugin.getConfigManager().getSanityMax();
                    if (current < max) {
                        setSanity(p, current + regenAmount);
                    }
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // 1분마다
    }

    // ======================== 디버프 시스템 ========================

    private void startDebuffTask() {
        debuffTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    int sanity = getSanity(p);
                    int max = plugin.getConfigManager().getSanityMax();

                    if (sanity <= max * 0.2) {
                        // 20% 이하: 심각 — 어둠 + 구속 + 독
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, false));
                        p.sendActionBar(Component.text("§4⚠ 정신력 위험! (" + sanity + "/" + max + ")"));
                    } else if (sanity <= max * 0.4) {
                        // 40% 이하: 경고 — 어둠만
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false));
                        p.sendActionBar(Component.text("§c⚠ 정신력 부족 (" + sanity + "/" + max + ")"));
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 100L); // 5초마다 체크
    }

    // ======================== DB 연동 ========================

    private void loadAllFromDB() {
        Connection conn = plugin.getDatabaseManager().getConnection();
        if (conn == null) return;
        try {
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT uuid, sanity FROM players WHERE sanity IS NOT NULL");
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    int sanity = rs.getInt("sanity");
                    sanityMap.put(uuid, sanity);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("정신력 데이터 로드 실패: " + e.getMessage());
        }
    }

    private void saveAllToDB() {
        Connection conn = plugin.getDatabaseManager().getConnection();
        if (conn == null) return;
        
        for (Map.Entry<UUID, Integer> entry : sanityMap.entrySet()) {
            try {
                String query = """
                    INSERT INTO players (uuid, name, sanity) VALUES (?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET sanity = excluded.sanity
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, entry.getKey().toString());
                    Player p = Bukkit.getPlayer(entry.getKey());
                    pstmt.setString(2, p != null ? p.getName() : "unknown");
                    pstmt.setInt(3, entry.getValue());
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("정신력 저장 실패: " + e.getMessage());
            }
        }
    }
}
