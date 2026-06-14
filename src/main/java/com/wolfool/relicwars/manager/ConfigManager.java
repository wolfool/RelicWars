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
    private int pickupSeconds;

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
    // 드랍 룰: {드랍개수 → [최소소유, 최대소유]}
    private java.util.Map<Integer, int[]> downedDropRules = new java.util.HashMap<>();
    private String finalDeathDropSelection;
    private boolean keepInventoryOnDeath;
    private boolean friendlyFireEnabled;
    private int combatTagSeconds;
    private int downedAutoExecuteSeconds;

    // --- 정신력(Sanity) ---
    private int sanityMax;
    private int sanityRegenPerMinute;
    private int goldenAppleRestore;

    // --- 팀(Team) ---
    private int teamMaxMembers;
    private int teamMaxTotalRelics;
    private int teamInviteExpireSeconds;

    // --- 이벤트(Event) ---
    private double bloodMoonChance;
    private int bloodMoonBroadcastInterval;
    private boolean bloodMoonBroadcastDimension;
    private int bloodMoonCoordinateBlur;
    private boolean forgottenRelicsEnabled;
    private boolean disableEndCities;
    private boolean disableElytra;

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
        pickupSeconds = config.getInt("relic.pickup-seconds", 2);

        // --- 전투 ---
        downedDropSealSeconds = config.getInt("combat.downed-drop-seal-seconds", 30);
        deathDropSealSeconds = config.getInt("combat.death-drop-seal-seconds", 30);
        bossDropSealSeconds = config.getInt("combat.boss-drop-seal-seconds", 60);
        executeHits = config.getInt("combat.execute-hits", 20);
        downedInvincibilitySeconds = config.getInt("combat.downed-invincibility-seconds", 2);
        downedEnvironmentalImmunity = config.getBoolean("combat.downed-environmental-immunity", true);
        reviveHealth = config.getInt("combat.revive-health", 6);
        reviveSeconds = config.getInt("combat.revive-seconds", 8);
        dropRelicOnDowned = config.getBoolean("combat.drop-relic-on-downed", true);
        downedDropSelection = config.getString("combat.downed-drop-selection", "lowest_number");
        // 강탈(Steal) 드랍 룰 파싱: "드랍개수: 최소소유-최대소유"
        downedDropRules.clear();
        org.bukkit.configuration.ConfigurationSection dropRulesSection = config.getConfigurationSection("combat.steal-drop-rules");
        if (dropRulesSection != null) {
            for (String key : dropRulesSection.getKeys(false)) {
                try {
                    int dropCount = Integer.parseInt(key);
                    String range = dropRulesSection.getString(key, "0-0");
                    String[] parts = range.split("-");
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    downedDropRules.put(dropCount, new int[]{min, max});
                } catch (Exception ignored) {}
            }
        }
        if (downedDropRules.isEmpty()) {
            // 기본값
            downedDropRules.put(0, new int[]{0, 1});
            downedDropRules.put(1, new int[]{2, 3});
            downedDropRules.put(2, new int[]{4, 4});
            downedDropRules.put(3, new int[]{5, 99});
        }
        finalDeathDropSelection = config.getString("combat.final-death-drop-selection", "highest_number");
        keepInventoryOnDeath = config.getBoolean("combat.keep-inventory-on-death", true);
        friendlyFireEnabled = config.getBoolean("combat.friendly-fire-enabled", false);
        combatTagSeconds = config.getInt("combat.combat-tag-seconds", 15);
        downedAutoExecuteSeconds = config.getInt("combat.downed-auto-execute-seconds", 60);

        // --- 정신력 ---
        sanityMax = config.getInt("sanity.max", 100);
        sanityRegenPerMinute = config.getInt("sanity.regen-per-minute", 5);
        goldenAppleRestore = config.getInt("sanity.golden-apple-restore", 20);

        // --- 팀 ---
        teamMaxMembers = config.getInt("team.max-members", 2);
        teamMaxTotalRelics = config.getInt("team.max-total-relics-to-form", 8);
        teamInviteExpireSeconds = config.getInt("team.invite-expire-seconds", 60);

        // --- 이벤트 ---
        bloodMoonChance = config.getDouble("event.blood-moon.chance-per-night", 0.15);
        bloodMoonBroadcastInterval = config.getInt("event.blood-moon.broadcast-interval-minutes", 3);
        bloodMoonBroadcastDimension = config.getBoolean("event.blood-moon.broadcast-dimension", true);
        bloodMoonCoordinateBlur = config.getInt("event.blood-moon.coordinate-blur", 100);
        forgottenRelicsEnabled = config.getBoolean("event.forgotten-relics.enabled", true);
        disableEndCities = config.getBoolean("events.disable-end-cities", true);
        disableElytra = config.getBoolean("events.disable-elytra", true);

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
    public int getPickupSeconds() { return pickupSeconds; }

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
    /**
     * 소유 유물 개수에 따른 드랍 개수를 반환합니다.
     * @param ownCount 현재 소유 유물 개수
     * @return 드랍할 유물 개수
     */
    public int getDownedDropCount(int ownCount) {
        for (java.util.Map.Entry<Integer, int[]> entry : downedDropRules.entrySet()) {
            int[] range = entry.getValue();
            if (ownCount >= range[0] && ownCount <= range[1]) {
                return entry.getKey();
            }
        }
        return 0; // 매칭 없으면 드랍 없음
    }
    public String getFinalDeathDropSelection() { return finalDeathDropSelection; }
    public boolean isKeepInventoryOnDeath() { return keepInventoryOnDeath; }
    public boolean isFriendlyFireEnabled() { return friendlyFireEnabled; }
    public int getCombatTagSeconds() { return combatTagSeconds; }
    public int getDownedAutoExecuteSeconds() { return downedAutoExecuteSeconds; }

    // --- 정신력 ---
    public int getSanityMax() { return sanityMax; }
    public int getSanityRegenPerMinute() { return sanityRegenPerMinute; }
    public int getGoldenAppleRestore() { return goldenAppleRestore; }

    // --- 팀 ---
    public int getTeamMaxMembers() { return teamMaxMembers; }
    public int getTeamMaxTotalRelics() { return teamMaxTotalRelics; }
    public int getTeamInviteExpireSeconds() { return teamInviteExpireSeconds; }

    // --- 이벤트 ---
    public double getBloodMoonChance() { return bloodMoonChance; }
    public int getBloodMoonBroadcastInterval() { return bloodMoonBroadcastInterval; }
    public boolean isBloodMoonBroadcastDimension() { return bloodMoonBroadcastDimension; }
    public int getBloodMoonCoordinateBlur() { return bloodMoonCoordinateBlur; }
    public boolean isForgottenRelicsEnabled() { return forgottenRelicsEnabled; }
    public boolean isDisableEndCities() { return disableEndCities; }
    public boolean isDisableElytra() { return disableElytra; }

    // --- 엔딩 ---
    public boolean isEndingRequiredAllSpawned() { return endingRequiredAllSpawned; }
    public int getEndingRequiredTeamRelics() { return endingRequiredTeamRelics; }
    public int getAltarDefenseMinutes() { return altarDefenseMinutes; }
    public int getMaxRelicStealsPerDowned() { return plugin.getConfig().getInt("combat.max-relic-steals", 3); }
    public int getMaxRelicsPerTeam() { return plugin.getConfig().getInt("team.max-relics", 3); }
}
