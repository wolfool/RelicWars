package com.wolfool.relicwars.manager;

import com.wolfool.relicwars.RelicWars;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * config.yml 설정값을 캐싱하여 빠르게 접근할 수 있도록 관리하는 매니저.
 * 플러그인 로드 시 YAML에서 값을 읽어 필드에 저장하고,
 * /relic reload 시 다시 로드할 수 있습니다.
 */
public class ConfigManager {

    private final RelicWars plugin;

    // --- 유물(Relic) ---
    private int maxRelicsPerPlayer;
    private int teamMaxOnFormation;
    private boolean sealGlow;

    // --- 전투(Combat) ---
    private int downedDropSealSeconds;
    private int deathDropSealSeconds;
    private int bossDropSealSeconds;
    private int executeHits;
    private int downedInvincibilitySeconds;
    private boolean downedEnvironmentalImmunity;
    private int reviveHealth;
    private int reviveSeconds;
    private boolean dropRelicOnDowned;
    private String downedDropSelection;
    private double finalDeathDropPercent;
    private String finalDeathDropSelection;
    private boolean keepInventoryOnDeath;
    private boolean friendlyFireEnabled;
    private int combatTagSeconds;

    // --- 정신력(Sanity) ---
    private int sanityMax;
    private int sanityRegenPerMinute;
    private int goldenAppleRestore;

    // --- 팀(Team) ---
    private int teamMaxMembers;
    private int teamInviteExpireSeconds;

    // --- 이벤트(Event) ---
    private double bloodMoonChance;
    private int bloodMoonBroadcastInterval;
    private boolean bloodMoonBroadcastDimension;
    private int bloodMoonCoordinateBlur;

    // --- 엔딩(Ending) ---
    private boolean endingRequiredAllSpawned;
    private int endingRequiredTeamRelics;
    private int altarDefenseMinutes;

    public ConfigManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    /**
     * config.yml에서 모든 값을 읽어 필드에 캐싱합니다.
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // --- 유물 ---
        maxRelicsPerPlayer = config.getInt("relic.max-per-player", 4);
        teamMaxOnFormation = config.getInt("relic.team-max-on-formation", 6);
        sealGlow = config.getBoolean("relic.seal-glow", true);

        // --- 전투 ---
        downedDropSealSeconds = config.getInt("combat.downed-drop-seal-seconds", 30);
        deathDropSealSeconds = config.getInt("combat.death-drop-seal-seconds", 30);
        bossDropSealSeconds = config.getInt("combat.boss-drop-seal-seconds", 60);
        executeHits = config.getInt("combat.execute-hits", 5);
        downedInvincibilitySeconds = config.getInt("combat.downed-invincibility-seconds", 2);
        downedEnvironmentalImmunity = config.getBoolean("combat.downed-environmental-immunity", true);
        reviveHealth = config.getInt("combat.revive-health", 6);
        reviveSeconds = config.getInt("combat.revive-seconds", 8);
        dropRelicOnDowned = config.getBoolean("combat.drop-relic-on-downed", true);
        downedDropSelection = config.getString("combat.downed-drop-selection", "lowest_number");
        finalDeathDropPercent = config.getDouble("combat.final-death-drop-percent", 0.30);
        finalDeathDropSelection = config.getString("combat.final-death-drop-selection", "highest_number");
        keepInventoryOnDeath = config.getBoolean("combat.keep-inventory-on-death", true);
        friendlyFireEnabled = config.getBoolean("combat.friendly-fire-enabled", false);
        combatTagSeconds = config.getInt("combat.combat-tag-seconds", 15);

        // --- 정신력 ---
        sanityMax = config.getInt("sanity.max", 100);
        sanityRegenPerMinute = config.getInt("sanity.regen-per-minute", 5);
        goldenAppleRestore = config.getInt("sanity.golden-apple-restore", 20);

        // --- 팀 ---
        teamMaxMembers = config.getInt("team.max-members", 2);
        teamInviteExpireSeconds = config.getInt("team.invite-expire-seconds", 60);

        // --- 이벤트 ---
        bloodMoonChance = config.getDouble("event.blood-moon.chance-per-night", 0.15);
        bloodMoonBroadcastInterval = config.getInt("event.blood-moon.broadcast-interval-minutes", 3);
        bloodMoonBroadcastDimension = config.getBoolean("event.blood-moon.broadcast-dimension", true);
        bloodMoonCoordinateBlur = config.getInt("event.blood-moon.coordinate-blur", 100);

        // --- 엔딩 ---
        endingRequiredAllSpawned = config.getBoolean("ending.required-all-spawned", true);
        endingRequiredTeamRelics = config.getInt("ending.required-team-relics-to-summon", 10);
        altarDefenseMinutes = config.getInt("ending.altar-defense-minutes", 30);
    }

    // ===================== Getters =====================

    // --- 유물 ---
    public int getMaxRelicsPerPlayer() { return maxRelicsPerPlayer; }
    public int getTeamMaxOnFormation() { return teamMaxOnFormation; }
    public boolean isSealGlow() { return sealGlow; }

    // --- 전투 ---
    public int getDownedDropSealSeconds() { return downedDropSealSeconds; }
    public int getDeathDropSealSeconds() { return deathDropSealSeconds; }
    public int getBossDropSealSeconds() { return bossDropSealSeconds; }
    public int getExecuteHits() { return executeHits; }
    public int getDownedInvincibilitySeconds() { return downedInvincibilitySeconds; }
    public boolean isDownedEnvironmentalImmunity() { return downedEnvironmentalImmunity; }
    public int getReviveHealth() { return reviveHealth; }
    public int getReviveSeconds() { return reviveSeconds; }
    public boolean isDropRelicOnDowned() { return dropRelicOnDowned; }
    public String getDownedDropSelection() { return downedDropSelection; }
    public double getFinalDeathDropPercent() { return finalDeathDropPercent; }
    public String getFinalDeathDropSelection() { return finalDeathDropSelection; }
    public boolean isKeepInventoryOnDeath() { return keepInventoryOnDeath; }
    public boolean isFriendlyFireEnabled() { return friendlyFireEnabled; }
    public int getCombatTagSeconds() { return combatTagSeconds; }

    // --- 정신력 ---
    public int getSanityMax() { return sanityMax; }
    public int getSanityRegenPerMinute() { return sanityRegenPerMinute; }
    public int getGoldenAppleRestore() { return goldenAppleRestore; }

    // --- 팀 ---
    public int getTeamMaxMembers() { return teamMaxMembers; }
    public int getTeamInviteExpireSeconds() { return teamInviteExpireSeconds; }

    // --- 이벤트 ---
    public double getBloodMoonChance() { return bloodMoonChance; }
    public int getBloodMoonBroadcastInterval() { return bloodMoonBroadcastInterval; }
    public boolean isBloodMoonBroadcastDimension() { return bloodMoonBroadcastDimension; }
    public int getBloodMoonCoordinateBlur() { return bloodMoonCoordinateBlur; }

    // --- 엔딩 ---
    public boolean isEndingRequiredAllSpawned() { return endingRequiredAllSpawned; }
    public int getEndingRequiredTeamRelics() { return endingRequiredTeamRelics; }
    public int getAltarDefenseMinutes() { return altarDefenseMinutes; }
}
