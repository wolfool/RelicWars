package com.wolfool.relicwars.ending;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

/**
 * 엔딩 제단 점령전 시스템.
 * 조건 충족 시 제단이 나타나고, 소환 팀이 반경 안에서 게이지를 채워야 합니다.
 * 적이 진입하면 게이지가 정지/감소합니다.
 * BossBar로 점령 게이지를 전체 서버에 표시합니다.
 */
public class EndingManager implements Manager {

    private final RelicWars plugin;
    private BukkitTask endingCheckTask;
    private BukkitTask captureTickTask;
    
    // 점령전 상태
    private boolean isEndingTriggered = false;
    private boolean isCaptureActive = false;
    private Location altarLocation;
    private ArmorStand altarMarker;
    private String capturingTeamId;
    private UUID capturingSoloId;
    private float captureProgress = 0.0f;
    
    // BossBar
    private BossBar bossBar;
    
    // Config 캐시
    private int altarRadius = 15;
    private float captureRatePerPlayer = 1.0f;
    private float decayRateEmpty = 0.5f;
    private float decayRateEnemy = 2.0f;

    public EndingManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        loadConfig();
        startEndingCheckTask();
        plugin.getLogger().info("\u00a7a[RelicWars] EndingManager \ucd08\uae30\ud654 \uc644\ub8cc.");
    }

    @Override
    public void shutdown() {
        if (endingCheckTask != null) endingCheckTask.cancel();
        stopCapture(true);
        plugin.getLogger().info("\u00a7a[RelicWars] EndingManager \uc885\ub8cc.");
    }
    
    private void loadConfig() {
        var config = plugin.getConfig();
        altarRadius = config.getInt("ending.altar-radius", 15);
        captureRatePerPlayer = (float) config.getDouble("ending.capture-rate-per-player", 1.0);
        decayRateEmpty = (float) config.getDouble("ending.decay-rate-empty", 0.5);
        decayRateEnemy = (float) config.getDouble("ending.decay-rate-enemy", 2.0);
    }

    // ======================== \uc790\ub3d9 \uc870\uac74 \uccb4\ud06c ========================

    private void startEndingCheckTask() {
        endingCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (isEndingTriggered) {
                    this.cancel();
                    return;
                }

                boolean requiredAllSpawned = plugin.getConfigManager().isEndingRequiredAllSpawned();
                if (requiredAllSpawned && !plugin.getRelicManager().areAllRelicsSpawned()) {
                    return;
                }

                int requiredRelics = plugin.getConfigManager().getEndingRequiredTeamRelics();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    String teamId = plugin.getTeamManager().getTeamId(p);
                    int teamRelics;
                    if (teamId != null) {
                        teamRelics = plugin.getTeamManager().getTeamRelicCount(teamId);
                    } else {
                        teamRelics = plugin.getRelicManager().countPlayerRelics(p);
                    }

                    if (teamRelics >= requiredRelics) {
                        startCapture(teamId, teamId == null ? p.getUniqueId() : null);
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 600L, 1200L);
    }

    // ======================== \uc810\ub839\uc804 \uc2dc\uc791/\uc911\ub2e8 ========================

    public void startCapture(String teamId, UUID soloId) {
        if (isCaptureActive) {
            plugin.getLogger().warning("[EndingManager] \uc774\ubbf8 \uc810\ub839\uc804\uc774 \uc9c4\ud589 \uc911\uc785\ub2c8\ub2e4.");
            return;
        }

        isEndingTriggered = true;
        isCaptureActive = true;
        capturingTeamId = teamId;
        capturingSoloId = soloId;
        captureProgress = 0.0f;

        altarLocation = Bukkit.getWorlds().get(0).getSpawnLocation().clone().add(0, 1, 0);

        altarMarker = altarLocation.getWorld().spawn(altarLocation, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCustomName("\u00a76\u2726 \uc5d4\ub529 \uc81c\ub2e8 \u2726");
            stand.setCustomNameVisible(true);
            stand.setGlowing(true);
            stand.setMarker(true);
        });

        bossBar = BossBar.bossBar(
                Component.text("\u2694 \uc5d4\ub529 \uc81c\ub2e8 \uc810\ub839 \u2014 0%", NamedTextColor.GOLD),
                0.0f,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bossBar);
        }

        String capturerName = getCapturerDisplayName();
        Bukkit.broadcast(Component.text("\u00a75============================================="));
        Bukkit.broadcast(Component.text("\u00a7d[\uc5d4\ub529] \u00a7f" + capturerName + "\u00a7f \ud300\uc774 \uc5d4\ub529 \uc870\uac74\uc744 \ucda9\uc871\ud588\uc2b5\ub2c8\ub2e4!"));
        Bukkit.broadcast(Component.text("\u00a7e\uc5d4\ub529 \uc81c\ub2e8\uc774 \uc6d4\ub4dc \uc2a4\ud3f0\uc5d0 \ub098\ud0c0\ub0ac\uc2b5\ub2c8\ub2e4!"));
        Bukkit.broadcast(Component.text("\u00a7e\uc88c\ud45c: X:" + altarLocation.getBlockX() + " Y:" + altarLocation.getBlockY() + " Z:" + altarLocation.getBlockZ()));
        Bukkit.broadcast(Component.text("\u00a7c\uc81c\ub2e8\uc744 \uc810\ub839\ud558\uc5ec \uac8c\uc774\uc9c0\ub97c 100%\uae4c\uc9c0 \ucc44\uc6b0\uba74 \uc2dc\uc98c \uc2b9\ub9ac!"));
        Bukkit.broadcast(Component.text("\u00a7c\uc801\uc774 \uc9c4\uc785\ud558\uba74 \uc810\ub839\uc774 \uc911\ub2e8\ub429\ub2c8\ub2e4! \uc81c\ub2e8\uc744 \uc0ac\uc218\ud558\uc138\uc694!"));
        Bukkit.broadcast(Component.text("\u00a75============================================="));

        captureTickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCapture();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopCapture(boolean silent) {
        isCaptureActive = false;
        captureProgress = 0.0f;

        if (captureTickTask != null) {
            captureTickTask.cancel();
            captureTickTask = null;
        }

        if (altarMarker != null && !altarMarker.isDead()) {
            altarMarker.remove();
            altarMarker = null;
        }

        if (bossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bossBar);
            }
            bossBar = null;
        }

        if (!silent) {
            Bukkit.broadcast(Component.text("\u00a7c[\uc5d4\ub529] \uc5d4\ub529 \uc81c\ub2e8 \uc774\ubca4\ud2b8\uac00 \uc911\ub2e8\ub418\uc5c8\uc2b5\ub2c8\ub2e4."));
        }
    }

    public void forceStart() {
        if (isCaptureActive) return;
        
        String bestTeam = null;
        UUID bestSolo = null;
        int maxRelics = 0;
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            String teamId = plugin.getTeamManager().getTeamId(p);
            int count;
            if (teamId != null) {
                count = plugin.getTeamManager().getTeamRelicCount(teamId);
            } else {
                count = plugin.getRelicManager().countPlayerRelics(p);
            }
            if (count > maxRelics) {
                maxRelics = count;
                bestTeam = teamId;
                bestSolo = (teamId == null) ? p.getUniqueId() : null;
            }
        }
        
        startCapture(bestTeam, bestSolo);
    }

    // ======================== \uc810\ub839 \uac8c\uc774\uc9c0 \ud2f1 ========================

    private void tickCapture() {
        if (!isCaptureActive || altarLocation == null) return;

        int alliesInRange = 0;
        int enemiesInRange = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getCombatManager().isDowned(p)) continue;
            if (!p.getWorld().equals(altarLocation.getWorld())) continue;
            if (p.getLocation().distanceSquared(altarLocation) > (double) altarRadius * altarRadius) continue;

            if (isCapturingMember(p)) {
                alliesInRange++;
            } else {
                enemiesInRange++;
            }
        }

        if (alliesInRange > 0 && enemiesInRange == 0) {
            // 아군만 → 점령 진행 (인원수 비례)
            captureProgress += (captureRatePerPlayer * alliesInRange) / 100.0f;
        } else if (alliesInRange > 0 && enemiesInRange > 0) {
            // 양쪽 다 → CONTESTED! 완전 정지 (오버워치 방식)
            // 아무 변동 없음 — 전투로 해결해야 함
        } else if (alliesInRange == 0 && enemiesInRange > 0) {
            // 적만 → 게이지 감소 (인원수 비례)
            captureProgress -= (decayRateEnemy * enemiesInRange) / 100.0f;
        } else {
            // 아무도 없음 → 느린 감소
            captureProgress -= decayRateEmpty / 100.0f;
        }

        captureProgress = Math.max(0.0f, Math.min(1.0f, captureProgress));

        updateBossBar(alliesInRange, enemiesInRange);

        // 파티클 효과 (3초마다)
        if (Bukkit.getCurrentTick() % 60 == 0) {
            altarLocation.getWorld().spawnParticle(Particle.END_ROD, altarLocation.clone().add(0, 3, 0), 30, 2, 3, 2, 0.05);
            altarLocation.getWorld().playSound(altarLocation, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.0f);
        }

        // CONTESTED 사운드 (양쪽 다 있을 때, 5초마다)
        if (alliesInRange > 0 && enemiesInRange > 0 && Bukkit.getCurrentTick() % 100 == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getWorld().equals(altarLocation.getWorld())) continue;
                if (p.getLocation().distanceSquared(altarLocation) <= (double) altarRadius * altarRadius * 4) {
                    p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.5f);
                }
            }
        }

        if (captureProgress >= 1.0f) {
            onCaptureComplete();
        }
    }

    private boolean isCapturingMember(Player p) {
        if (capturingTeamId != null) {
            String playerTeam = plugin.getTeamManager().getTeamId(p);
            return capturingTeamId.equals(playerTeam);
        } else if (capturingSoloId != null) {
            return capturingSoloId.equals(p.getUniqueId());
        }
        return false;
    }

    private void updateBossBar(int allies, int enemies) {
        if (bossBar == null) return;

        int percent = Math.round(captureProgress * 100);
        
        String statusText;
        BossBar.Color color;
        
        if (allies > 0 && enemies == 0) {
            statusText = "\u00a7a\u25b2 \uc810\ub839 \uc911";
            color = BossBar.Color.GREEN;
        } else if (allies > 0 && enemies > 0) {
            statusText = "§c§l⚡ CONTESTED!";
            color = BossBar.Color.RED;
        } else if (enemies > 0) {
            statusText = "\u00a74\u25bc \uc801 \uce68\ud22c";
            color = BossBar.Color.RED;
        } else {
            statusText = "\u00a77\u2014 \ube44\uc5b4\uc788\uc74c";
            color = BossBar.Color.YELLOW;
        }

        bossBar.name(Component.text("\u2694 \uc5d4\ub529 \uc81c\ub2e8 \uc810\ub839 \u2014 " + percent + "% " + statusText));
        bossBar.progress(captureProgress);
        bossBar.color(color);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bossBar);
        }
    }

    // ======================== \uc810\ub839 \uc644\ub8cc ========================

    private void onCaptureComplete() {
        isCaptureActive = false;

        if (captureTickTask != null) {
            captureTickTask.cancel();
            captureTickTask = null;
        }

        if (bossBar != null) {
            bossBar.name(Component.text("\u00a76\u2726 \uc2dc\uc98c \uc2b9\ub9ac! \u2726", NamedTextColor.GOLD));
            bossBar.progress(1.0f);
            bossBar.color(BossBar.Color.YELLOW);
        }

        if (altarMarker != null && !altarMarker.isDead()) {
            altarMarker.remove();
        }

        String winnerName = getCapturerDisplayName();

        Bukkit.broadcast(Component.text("\u00a76============================================="));
        Bukkit.broadcast(Component.text("\u00a76          \u2726 \uc2dc\uc98c \uc2b9\ub9ac! \u2726"));
        Bukkit.broadcast(Component.text("\u00a7e  " + winnerName + " \u00a7e\ud300\uc774 \uc5d4\ub529 \uc81c\ub2e8 \uc810\ub839\uc5d0 \uc131\uacf5\ud588\uc2b5\ub2c8\ub2e4!"));
        Bukkit.broadcast(Component.text("\u00a76============================================="));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            p.sendTitle("\u00a76\u2726 \uc2dc\uc98c \uc2b9\ub9ac \u2726", "\u00a7e" + winnerName + " \ud300\uc758 \uc2b9\ub9ac!", 20, 100, 40);
        }

        Location fireworkLoc = altarLocation.clone().add(0, 5, 0);
        for (int i = 0; i < 5; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                altarLocation.getWorld().spawnParticle(Particle.FIREWORK, fireworkLoc, 50, 3, 5, 3, 0.1);
                altarLocation.getWorld().strikeLightningEffect(altarLocation);
            }, i * 20L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bossBar != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.hideBossBar(bossBar);
                }
                bossBar = null;
            }
        }, 200L);
    }

    // ======================== \uc720\ud2f8\ub9ac\ud2f0 ========================

    private String getCapturerDisplayName() {
        if (capturingTeamId != null) {
            List<UUID> members = plugin.getTeamManager().getTeamMembers(capturingTeamId);
            if (members != null && !members.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (UUID uuid : members) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        if (sb.length() > 0) sb.append(" & ");
                        sb.append(p.getName());
                    }
                }
                return sb.length() > 0 ? sb.toString() : capturingTeamId;
            }
            return capturingTeamId;
        } else if (capturingSoloId != null) {
            Player p = Bukkit.getPlayer(capturingSoloId);
            return p != null ? p.getName() : "\uc54c \uc218 \uc5c6\uc74c";
        }
        return "\uc54c \uc218 \uc5c6\uc74c";
    }

    // ======================== \uc678\ubd80 \uc811\uadfc\uc790 ========================

    public boolean isCaptureActive() { return isCaptureActive; }
    public boolean isEndingTriggered() { return isEndingTriggered; }
    public float getCaptureProgress() { return captureProgress; }
    public Location getAltarLocation() { return altarLocation; }
    
    public void resetEnding() {
        stopCapture(true);
        isEndingTriggered = false;
        capturingTeamId = null;
        capturingSoloId = null;
        // 기존 체크 태스크가 남아있으면 cancel 후 재시작
        if (endingCheckTask != null) {
            try { endingCheckTask.cancel(); } catch (IllegalStateException ignored) {}
        }
        startEndingCheckTask();
    }
}
