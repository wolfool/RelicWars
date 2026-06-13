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
 * 플레이어의 전투 상태(다운, 확킬, 2초 무적, 구조 등)를 관리합니다.
 */
public class CombatManager implements Manager {

    private final RelicWars plugin;

    // UUID -> 다운된 시각 (밀리초). 무적 시간(2초) 계산 등에 사용
    private final Map<UUID, Long> downedPlayers = new HashMap<>();

    // UUID -> 확킬 맞은 횟수 (최대 5타)
    private final Map<UUID, Integer> executeHits = new HashMap<>();

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
        downedPlayers.put(player.getUniqueId(), System.currentTimeMillis());
        executeHits.put(player.getUniqueId(), 0);

        player.setHealth(20.0); // 다운 시 체력은 꽉 찬 상태로 유지 (시스템적 체력)
        player.setFoodLevel(6); // 뛰지 못하게
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 4, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 250, false, false, false)); // 점프 불가
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false)); // 시야 제한

        player.sendMessage("§c[RelicWars] 치명상을 입고 쓰러졌습니다! (다운 상태)");
        player.sendTitle("§c§lDOWNED", "§7팀원의 구조를 기다리세요...", 10, 70, 20);

        // --- 유물 1개 자동 드랍 ---
        if (plugin.getConfigManager().isDropRelicOnDowned()) {
            ItemStack droppedRelic = plugin.getRelicManager().extractDownedDrop(player);
            if (droppedRelic != null) {
                int sealTime = plugin.getConfigManager().getDownedDropSealSeconds();
                plugin.getSealedRelicManager().spawnSealedRelic(player.getLocation(), droppedRelic, sealTime);
                player.sendMessage("§c[RelicWars] 다운되며 가장 좋은 유물을 떨어뜨렸습니다!");
            }
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
     * 5타가 누적되면 진짜 사망 처리합니다.
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

    /**
     * 유저를 완전히 죽이고, 비례하여 유물을 바닥에 드랍시킵니다.
     */
    public void executePlayer(Player victim, Player attacker) {
        // 1. 유물 드랍 (비례)
        List<ItemStack> drops = plugin.getRelicManager().extractDeathDrop(victim);
        int sealTime = plugin.getConfigManager().getDeathDropSealSeconds();
        for (ItemStack drop : drops) {
            // 주변에 흩뿌리기
            Location dropLoc = victim.getLocation().clone().add(
                    (Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2);
            plugin.getSealedRelicManager().spawnSealedRelic(dropLoc, drop, sealTime);
        }

        // 2. 바닐라 아이템 드랍 방지 (keepInventory) 및 부활 처리 유도
        if (plugin.getConfigManager().isKeepInventoryOnDeath()) {
            // KeepInventory 룰에 따라 인벤토리를 유지한 채로 리스폰
            // 실제 데스 이벤트를 캔슬할 수는 없으므로, 체력을 0으로 만들어 바닐라 사망을 유도합니다.
            // (CombatListener에서 KeepInventory 이벤트 처리 예정)
            victim.setHealth(0.0);
        } else {
            victim.setHealth(0.0);
        }

        clearDownedState(victim);

        if (attacker != null) {
            Bukkit.broadcast(Component.text("§c[RelicWars] §f" + victim.getName() + "§c님이 §f" + attacker.getName() + "§c님에게 처형당했습니다!"));
        } else {
            Bukkit.broadcast(Component.text("§c[RelicWars] §f" + victim.getName() + "§c님이 처형당했습니다!"));
        }
    }

    /**
     * 자결 처리 (명령어 등에서 호출)
     */
    public void suicide(Player player) {
        if (!isDowned(player)) {
            player.sendMessage("§c[RelicWars] 다운 상태에서만 자결할 수 있습니다.");
            return;
        }
        executePlayer(player, null);
    }

    // ======================== 구조 (부활) ========================

    /**
     * 다운된 유저를 부활시킵니다.
     */
    public void revivePlayer(Player player) {
        if (!isDowned(player)) return;

        clearDownedState(player);

        int reviveHp = plugin.getConfigManager().getReviveHealth();
        player.setHealth(Math.min(reviveHp, player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));

        player.sendMessage("§a[RelicWars] 팀원의 도움으로 구조되었습니다!");
        player.sendTitle("§a§lREVIVED", "§f전투에 복귀합니다.", 10, 40, 10);
    }

    private void clearDownedState(Player player) {
        downedPlayers.remove(player.getUniqueId());
        executeHits.remove(player.getUniqueId());

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }
}
