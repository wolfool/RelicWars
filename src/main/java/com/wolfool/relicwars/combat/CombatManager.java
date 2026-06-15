package com.wolfool.relicwars.combat;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 플레이어의 전투 상태(다운, 확킬, 강탈, 구조 등)를 관리합니다.
 * - 다운: 가장 좋은 유물 1개 즉시 드랍
 * - 확킬: 다운된 적을 때려서 죽임 (추가 유물 드랍 없음)
 * - 강탈: 다운된 적에게 인터랙션하여 유물을 뺏음 (steal-drop-rules)
 * - 구조: 다운된 팀원을 살림
 */
public class CombatManager implements Manager {

    private final RelicWars plugin;

    // UUID -> 다운된 시각 (밀리초). 무적 시간(2초) 계산 등에 사용
    private final Map<UUID, Long> downedPlayers = new HashMap<>();

    // UUID -> 확킬 맞은 횟수 (최대 5타)
    private final Map<UUID, Integer> executeHits = new HashMap<>();

    // UUID -> 자동 확킬 타이머 태스크
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> autoExecuteTasks = new HashMap<>();

    // UUID -> 전투 태그 만료 시각 (밀리초)
    private final Map<UUID, Long> combatTags = new HashMap<>();

    // UUID -> 강탈 당한 횟수
    private final Map<UUID, Integer> stealCounts = new HashMap<>();

    public CombatManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        Bukkit.getPluginManager().registerEvents(new CombatListener(plugin, this), plugin);
        plugin.getLogger().info("§a[RelicWars] CombatManager 초기화 완료.");
    }

    @Override
    public void shutdown() {
        downedPlayers.clear();
        executeHits.clear();
        for (org.bukkit.scheduler.BukkitTask task : autoExecuteTasks.values()) {
            task.cancel();
        }
        autoExecuteTasks.clear();
        combatTags.clear();
        plugin.getLogger().info("§a[RelicWars] CombatManager 종료.");
    }

    // ======================== 다운 상태 관리 ========================

    /**
     * 플레이어가 다운 상태인지 확인합니다.
     */
    public boolean isDowned(Player player) {
        return downedPlayers.containsKey(player.getUniqueId());
    }

    /**
     * 플레이어를 다운 상태로 만듭니다.
     * 체력이 0이 되는 순간 호출되어야 합니다.
     */
    public void setDowned(Player player) {
        // #005 불멸의 심장 소지 여부 감지 및 발동
        if (plugin.getRelicManager().hasRelic(player, 5)) {
            if (plugin.getRelicAbilityHandler().trigger005ImmortalHeart(player)) {
                return; // 발동 성공 시 다운을 무시하고 빠져나감!
            }
        }

        downedPlayers.put(player.getUniqueId(), System.currentTimeMillis());
        executeHits.put(player.getUniqueId(), 0);
        stealCounts.put(player.getUniqueId(), 0);

        // #017 왜곡의 닻 텔레포트 세이브
        String teamId = plugin.getTeamManager().getTeamId(player);
        String key = teamId != null ? teamId : player.getUniqueId().toString();
        Location anchorLoc = plugin.getRelicAbilityHandler().active017Anchor.get(key);
        if (anchorLoc != null && player.getLocation().distanceSquared(anchorLoc) <= 2500) { // 반경 50블록
            player.teleport(anchorLoc);
            player.sendMessage("§d[왜곡의 닻] 공간 왜곡장에 의해 안전한 위치로 대피했습니다!");
            plugin.getRelicAbilityHandler().active017Anchor.remove(key); // 1회용 소모
        }

        player.setHealth(20.0); // 다운 시 체력은 꽉 찬 상태로 유지 (시스템적 체력)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 4, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 250, false, false, false)); // 점프 불가
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false)); // 시야 제한

        player.sendMessage("§c[RelicWars] 치명상을 입고 쓰러졌습니다! (다운 상태)");
        player.sendTitle("§c§lDOWNED", "§7팀원의 구조를 기다리세요...", 10, 70, 20);

        // 정신력 0으로 초기화
        plugin.getSanityManager().setSanity(player, 0);

        // === 다운 이펙트 ===
        com.wolfool.relicwars.relic.InteractionEffects.playDownEffect(player);

        // --- 다운 시 가장 좋은(번호 낮은) 유물 1개 즉시 드랍 ---
        if (plugin.getConfigManager().isDropRelicOnDowned()) {
            ItemStack bestRelic = plugin.getRelicManager().extractBestRelic(player);
            if (bestRelic != null) {
                int sealTime = plugin.getConfigManager().getDownedDropSealSeconds();
                plugin.getSealedRelicManager().spawnSealedRelic(getSafeDropLocation(player.getLocation()), bestRelic, sealTime);
                int relicNum = com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(bestRelic);
                com.wolfool.relicwars.relic.RelicDefinition def = com.wolfool.relicwars.relic.RelicDefinition.getByNumber(relicNum);
                String name = def != null ? def.getDisplayName() : "유물";
                player.sendMessage("§c[RelicWars] 다운되며 " + name + " §c유물을 떨어뜨렸습니다!");
            }
        }

        // --- 자동 사망 카운트다운 타이머 ---
        int autoExecuteSeconds = plugin.getConfigManager().getDownedAutoExecuteSeconds();
        if (autoExecuteSeconds > 0) {
            org.bukkit.scheduler.BukkitTask autoTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int timeLeft = autoExecuteSeconds;
                @Override
                public void run() {
                    if (!isDowned(player) || !player.isOnline()) {
                        org.bukkit.scheduler.BukkitTask task = autoExecuteTasks.remove(player.getUniqueId());
                        if (task != null) task.cancel();
                        return;
                    }

                    if (timeLeft <= 0) {
                        org.bukkit.scheduler.BukkitTask task = autoExecuteTasks.remove(player.getUniqueId());
                        if (task != null) task.cancel();
                        player.sendMessage("§c[RelicWars] 구조받지 못해 사망했습니다...");
                        Bukkit.broadcast(Component.text("§c[RelicWars] §f" + player.getName() + "§c님이 구조받지 못해 사망했습니다."));
                        com.wolfool.relicwars.relic.InteractionEffects.playAutoExecuteEffect(player);
                        executePlayer(player, null);
                        return;
                    }

                    String color = timeLeft <= 10 ? "§c" : timeLeft <= 30 ? "§e" : "§a";
                    String heartbeat = timeLeft <= 10 ? " §4§l❤" : "";
                    player.sendActionBar(Component.text(color + "§l사망까지 " + timeLeft + "초" + heartbeat));

                    if (timeLeft <= 10) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WARDEN_HEARTBEAT,
                                org.bukkit.SoundCategory.PLAYERS, 0.8f, 1.0f + (10 - timeLeft) * 0.05f);
                    }

                    timeLeft--;
                }
            }, 0L, 20L);
            autoExecuteTasks.put(player.getUniqueId(), autoTask);
        }
    }

    /**
     * 다운 직후 2초 무적 상태인지 확인합니다.
     */
    public boolean isInvincible(Player player) {
        if (!isDowned(player)) return false;
        long downedAt = downedPlayers.get(player.getUniqueId());
        int invincibilityMillis = plugin.getConfigManager().getDownedInvincibilitySeconds() * 1000;
        return System.currentTimeMillis() - downedAt < invincibilityMillis;
    }

    // ======================== 확킬 (처형) ========================

    /**
     * 다운된 유저에게 확킬 타격을 1회 추가합니다.
     * 필요 횟수 누적 시 사망 처리 (유물 추가 드랍 없음).
     */
    public void addExecuteHit(Player victim, Player attacker) {
        if (!isDowned(victim) || isInvincible(victim)) return;

        int requiredHits = plugin.getConfigManager().getExecuteHits();
        int currentHits = executeHits.getOrDefault(victim.getUniqueId(), 0) + 1;
        executeHits.put(victim.getUniqueId(), currentHits);

        victim.sendMessage("§c[RelicWars] 적에게 처형당하고 있습니다! (" + currentHits + "/" + requiredHits + ")");
        attacker.sendMessage("§e[RelicWars] 처형 타격 적중! (" + currentHits + "/" + requiredHits + ")");

        if (currentHits >= requiredHits) {
            executePlayer(victim, attacker);
        }
    }

    // ======================== 강탈 (Steal) ========================

    /**
     * 다운된 적의 유물을 강탈합니다.
     * steal-drop-rules config에 따라 가장 높은 번호(약한) 유물부터 드랍됩니다.
     * @return 강탈된 유물 개수
     */
    public int stealRelics(Player victim, Player stealer) {
        if (!isDowned(victim) || isInvincible(victim)) return 0;

        int maxSteals = plugin.getConfigManager().getMaxRelicStealsPerDowned();
        int currentSteals = stealCounts.getOrDefault(victim.getUniqueId(), 0);
        if (currentSteals >= maxSteals) {
            stealer.sendMessage("§c[RelicWars] 이 대상에게서는 더 이상 강탈할 수 없습니다.");
            return 0;
        }

        List<ItemStack> stealDrops = plugin.getRelicManager().extractStealDrop(victim);
        if (stealDrops.isEmpty()) {
            stealer.sendMessage("§7[RelicWars] 이 플레이어에게는 강탈할 유물이 없습니다.");
            return 0;
        }

        stealCounts.put(victim.getUniqueId(), currentSteals + 1);

        int sealTime = plugin.getConfigManager().getDownedDropSealSeconds();
        for (ItemStack relic : stealDrops) {
            Location dropLoc = getSafeDropLocation(victim.getLocation().clone().add(
                    (Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2));
            plugin.getSealedRelicManager().spawnSealedRelic(dropLoc, relic, sealTime);
        }

        victim.sendMessage("§c[RelicWars] 유물 " + stealDrops.size() + "개를 강탈당했습니다!");
        stealer.sendMessage("§a[RelicWars] 유물 " + stealDrops.size() + "개를 강탈했습니다!");
        Bukkit.broadcast(Component.text("§e[소문] §f" + stealer.getName() + "§e이(가) §f" + victim.getName() + "§e에게서 유물 " + stealDrops.size() + "개를 강탈했습니다!"));

        com.wolfool.relicwars.relic.InteractionEffects.playExecuteEffect(victim, stealer);
        return stealDrops.size();
    }

    // ======================== 사망 처리 ========================

    /**
     * 유저를 사망시키고 스폰으로 보냅니다. (기획서 수정: 사망 시 Steal 규칙에 따라 추가 드랍)
     */
    public void executePlayer(Player victim, Player attacker) {
        clearDownedState(victim);
        victim.setHealth(0.0);
        victim.sendMessage("§c[RelicWars] 처형당했습니다. (추가 유물 드랍 없음)");
        org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c[처형] §f" + victim.getName() + "§c님이 처형당했습니다."));
    }

    public void killPlayer(Player victim, Player attacker) {
        // 사망 전 강탈 규칙 적용하여 바닥에 떨어뜨리기
        List<ItemStack> stealDrops = plugin.getRelicManager().extractStealDrop(victim);
        if (!stealDrops.isEmpty()) {
            int sealTime = plugin.getConfigManager().getDeathDropSealSeconds();
            for (ItemStack relic : stealDrops) {
                Location dropLoc = getSafeDropLocation(victim.getLocation().clone().add(
                        (Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2));
                plugin.getSealedRelicManager().spawnSealedRelic(dropLoc, relic, sealTime);
            }
            victim.sendMessage("§c[RelicWars] 사망으로 인해 유물 " + stealDrops.size() + "개를 땅에 떨어뜨렸습니다!");
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§e[소문] §f" + victim.getName() + "§e이(가) 처형되어 유물 " + stealDrops.size() + "개가 바닥에 흩어졌습니다!"));
        }

        clearDownedState(victim);
        victim.setHealth(0.0);

        if (attacker != null) {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c[RelicWars] §f" + victim.getName() + "§c님이 §f" + attacker.getName() + "§c님에게 처형당했습니다!"));
        } else {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c[RelicWars] §f" + victim.getName() + "§c님이 사망했습니다."));
        }
        com.wolfool.relicwars.relic.InteractionEffects.playExecuteEffect(victim, attacker);
    }

    // ======================== 구조 (부활) ========================

    /**
     * 다운된 유저를 부활시킵니다.
     */
    public void revivePlayer(Player player) {
        if (!isDowned(player)) return;

        clearDownedState(player);

        int reviveHp = plugin.getConfigManager().getReviveHealth();
        player.setHealth(Math.min(reviveHp, java.util.Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue()));

        player.sendMessage("§a[RelicWars] 팀원의 도움으로 구조되었습니다!");
        player.sendTitle("§a§lREVIVED", "§f전투에 복귀합니다.", 10, 40, 10);
    }

    private void clearDownedState(Player player) {
        downedPlayers.remove(player.getUniqueId());
        executeHits.remove(player.getUniqueId());
        stealCounts.remove(player.getUniqueId());

        // 자동 확킬 타이머 취소
        org.bukkit.scheduler.BukkitTask autoTask = autoExecuteTasks.remove(player.getUniqueId());
        if (autoTask != null) autoTask.cancel();

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }

    // ======================== 전투 태그 (Combat Tag) ========================

    public void setCombatTag(Player player) {
        if (isDowned(player)) return; // 다운 중에는 갱신 안 함
        
        long expireTime = System.currentTimeMillis() + (plugin.getConfigManager().getCombatTagSeconds() * 1000L);
        combatTags.put(player.getUniqueId(), expireTime);
    }

    public boolean isInCombat(Player player) {
        Long expireTime = combatTags.get(player.getUniqueId());
        if (expireTime == null) return false;
        if (expireTime > System.currentTimeMillis()) return true;
        combatTags.remove(player.getUniqueId()); // 만료된 엔트리 자동 정리
        return false;
    }

    public void handleCombatLog(Player player) {
        if (isInCombat(player) && !isDowned(player)) {
            // 랜뽑 시 즉시 일반 다운 패널티(1개 드랍) + 최종 사망 패널티(강탈 드랍) 적용
            Bukkit.broadcast(Component.text("§c[RelicWars] §f" + player.getName() + "§c님이 전투 중 강제 종료(랜뽑)하여 처형되었습니다!"));
            
            // 1. 일반 다운 패널티 로직 (최상급 1개)
            if (plugin.getConfigManager().isDropRelicOnDowned()) {
                ItemStack bestRelic = plugin.getRelicManager().extractBestRelic(player);
                if (bestRelic != null) {
                    plugin.getSealedRelicManager().spawnSealedRelic(player.getLocation(), bestRelic, plugin.getConfigManager().getDownedDropSealSeconds());
                }
            }
            
            // 2. 최종 사망 패널티 (나머지 강탈 규칙에 따라 추가 드랍)
            killPlayer(player, null);
            
            combatTags.remove(player.getUniqueId());
        }
    }

    /**
     * 보이드 추락 등으로 인해 유물이 유실되는 것을 막기 위한 안전 드랍 좌표 계산
     * (보이드 추락 시 랜덤 좌표로 이동)
     */
    public Location getSafeDropLocation(Location loc) {
        org.bukkit.World world = loc.getWorld();
        if (world == null) return loc;

        // 고도가 너무 낮으면(보이드)
        if (loc.getY() < world.getMinHeight() + 5) {
            int randomX = (int) (Math.random() * 2000) - 1000;
            int randomZ = (int) (Math.random() * 2000) - 1000;
            loc.setX(randomX);
            loc.setZ(randomZ);
            
            int highest = world.getHighestBlockYAt(loc);
            if (highest <= world.getMinHeight()) {
                loc.setY(100);
            } else {
                loc.setY(highest + 1);
            }
        }
        return loc;
    }
}
