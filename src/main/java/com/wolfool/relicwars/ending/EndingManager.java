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

    // 준비 딜레이 상태
    private boolean isPreparing = false;
    private BukkitTask preparationTask;
    private BukkitTask preparationCountdownTask;

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
        if (isCaptureActive || isPreparing) {
            plugin.getLogger().warning("[EndingManager] 이미 점령전이 진행/준비 중입니다.");
            return;
        }

        isEndingTriggered = true;
        capturingTeamId = teamId;
        capturingSoloId = soloId;
        captureProgress = 0.0f;

        // 제단 위치 결정: config 우선, 없으면 월드 스폰
        altarLocation = resolveAltarLocation();

        String capturerName = getCapturerDisplayName();
        int delayMinutes = plugin.getConfigManager().getPreparationDelayMinutes();

        if (delayMinutes <= 0) {
            // 즉시 시작
            beginCapture(capturerName);
        } else {
            // 준비 시간 시작
            isPreparing = true;
            Bukkit.broadcast(Component.text("§5============================================="));
            Bukkit.broadcast(Component.text("§d[엔딩] §f" + capturerName + "§f 팀이 엔딩 조건을 충족했습니다!"));
            Bukkit.broadcast(Component.text("§e엔딩 제단이 곧 활성화됩니다!"));
            Bukkit.broadcast(Component.text("§e좌표: X:" + altarLocation.getBlockX() + " Y:" + altarLocation.getBlockY() + " Z:" + altarLocation.getBlockZ()));
            Bukkit.broadcast(Component.text("§c§l" + delayMinutes + "분 후 점령전이 시작됩니다! 제단으로 이동하세요!"));
            Bukkit.broadcast(Component.text("§5============================================="));

            // 준비 BossBar 표시
            bossBar = BossBar.bossBar(
                    Component.text("⚔ 엔딩 제단 준비 중 — " + delayMinutes + "분 후 시작", NamedTextColor.GOLD),
                    1.0f,
                    BossBar.Color.YELLOW,
                    BossBar.Overlay.PROGRESS
            );
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showBossBar(bossBar);
            }

            // 제단 마커 미리 설치
            altarMarker = altarLocation.getWorld().spawn(altarLocation, ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setCustomName("§6✦ 엔딩 제단 (준비 중) ✦");
                stand.setCustomNameVisible(true);
                stand.setGlowing(true);
                stand.setMarker(true);
            });

            // 카운트다운 (매 1분마다 알림)
            final int totalSeconds = delayMinutes * 60;
            final long startTick = Bukkit.getCurrentTick();
            preparationCountdownTask = new BukkitRunnable() {
                @Override
                public void run() {
                    long elapsed = (Bukkit.getCurrentTick() - startTick) / 20;
                    int remaining = totalSeconds - (int) elapsed;
                    if (remaining <= 0) {
                        this.cancel();
                        return;
                    }
                    float progress = (float) remaining / totalSeconds;
                    if (bossBar != null) {
                        int mins = remaining / 60;
                        int secs = remaining % 60;
                        String timeStr = mins > 0 ? mins + "분 " + secs + "초" : secs + "초";
                        bossBar.name(Component.text("⚔ 엔딩 제단 준비 중 — " + timeStr + " 후 시작", NamedTextColor.GOLD));
                        bossBar.progress(Math.max(0, Math.min(1.0f, progress)));
                    }
                    // 1분마다 알림
                    if (remaining % 60 == 0 && remaining > 0) {
                        Bukkit.broadcast(Component.text("§e[엔딩] §c점령전 시작까지 " + (remaining / 60) + "분 남았습니다!"));
                    }
                    // 30초, 10초 알림
                    if (remaining == 30) {
                        Bukkit.broadcast(Component.text("§e[엔딩] §c§l점령전 시작까지 30초!"));
                    }
                    if (remaining == 10) {
                        Bukkit.broadcast(Component.text("§e[엔딩] §c§l점령전 시작까지 10초!"));
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L);

            // 딜레이 후 점령전 시작
            preparationTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                isPreparing = false;
                if (preparationCountdownTask != null) {
                    preparationCountdownTask.cancel();
                    preparationCountdownTask = null;
                }
                beginCapture(capturerName);
            }, (long) delayMinutes * 60 * 20);
        }
    }

    /**
     * config에서 제단 위치를 결정합니다.
     * config에 world가 설정되어 있으면 해당 좌표 사용, 아니면 월드 스폰 사용.
     */
    private Location resolveAltarLocation() {
        String worldName = plugin.getConfigManager().getAltarWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return new Location(world,
                        plugin.getConfigManager().getAltarX(),
                        plugin.getConfigManager().getAltarY(),
                        plugin.getConfigManager().getAltarZ());
            }
            plugin.getLogger().warning("[EndingManager] config의 엔딩 제단 월드 '" + worldName + "'를 찾을 수 없습니다. 스폰 지점을 사용합니다.");
        }
        return Bukkit.getWorlds().get(0).getSpawnLocation().clone().add(0, 1, 0);
    }

    /**
     * 실제 점령전을 시작합니다 (준비 딜레이 후 호출).
     */
    private void beginCapture(String capturerName) {
        isCaptureActive = true;

        // 기존 마커 이름 변경
        if (altarMarker != null && !altarMarker.isDead()) {
            altarMarker.setCustomName("§6✦ 엔딩 제단 ✦");
        }

        // BossBar 업데이트
        if (bossBar != null) {
            bossBar.name(Component.text("⚔ 엔딩 제단 점령 — 0%", NamedTextColor.GOLD));
            bossBar.progress(0.0f);
            bossBar.color(BossBar.Color.PURPLE);
        } else {
            bossBar = BossBar.bossBar(
                    Component.text("⚔ 엔딩 제단 점령 — 0%", NamedTextColor.GOLD),
                    0.0f,
                    BossBar.Color.PURPLE,
                    BossBar.Overlay.PROGRESS
            );
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bossBar);
        }

        Bukkit.broadcast(Component.text("§5============================================="));
        Bukkit.broadcast(Component.text("§d§l[엔딩] 점령전이 시작되었습니다!"));
        Bukkit.broadcast(Component.text("§c제단을 점령하여 게이지를 100%까지 채우면 시즌 승리!"));
        Bukkit.broadcast(Component.text("§c적이 진입하면 점령이 중단됩니다! 제단을 사수하세요!"));
        Bukkit.broadcast(Component.text("§5============================================="));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        captureTickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCapture();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopCapture(boolean silent) {
        isCaptureActive = false;

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
        isPreparing = false;
        captureProgress = 0.0f;
        capturingTeamId = null;
        capturingSoloId = null;
        // 준비 딜레이 태스크 정리
        if (preparationTask != null) {
            try { preparationTask.cancel(); } catch (IllegalStateException ignored) {}
            preparationTask = null;
        }
        if (preparationCountdownTask != null) {
            try { preparationCountdownTask.cancel(); } catch (IllegalStateException ignored) {}
            preparationCountdownTask = null;
        }
        // 기존 체크 태스크가 남아있으면 cancel 후 재시작
        if (endingCheckTask != null) {
            try { endingCheckTask.cancel(); } catch (IllegalStateException ignored) {}
        }
        startEndingCheckTask();
    }
}
