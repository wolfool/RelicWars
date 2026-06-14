package com.wolfool.relicwars.relic;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 땅에 떨어진 유물을 '봉인된 아이템 디스플레이(ItemDisplay)' 형태로 관리합니다.
 * 봉인 해제 타이머 관리 및 우클릭 획득 상호작용을 처리합니다.
 */
public class SealedRelicManager implements Manager, Listener {

    private final RelicWars plugin;

    // ItemDisplay UUID -> 해제 스케줄러
    private final Map<UUID, BukkitTask> unsealTasks = new HashMap<>();

    // 플레이어 UUID -> 줍기 캐스팅 태스크
    private final Map<UUID, BukkitTask> pickupTasks = new HashMap<>();

    public SealedRelicManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("§a[RelicWars] SealedRelicManager 초기화 완료.");
    }

    @Override
    public void shutdown() {
        for (BukkitTask task : unsealTasks.values()) {
            task.cancel();
        }
        unsealTasks.clear();
        for (BukkitTask task : pickupTasks.values()) {
            task.cancel();
        }
        pickupTasks.clear();
        plugin.getLogger().info("§a[RelicWars] SealedRelicManager 종료.");
    }

    /**
     * 바닥에 봉인된 유물을 생성합니다. (물리엔진 적용)
     *
     * @param location 드랍할 위치
     * @param relic    유물 아이템
     * @param sealSeconds 봉인 지속 시간 (초)
     */
    public void spawnSealedRelic(Location location, ItemStack relic, int sealSeconds) {
        if (relic == null || !RelicItemUtil.isRelic(relic)) return;

        // 실제 드랍 아이템(Item) 엔티티를 소환하여 중력(물리) 적용
        org.bukkit.entity.Item itemEntity = location.getWorld().dropItem(location, relic);
        itemEntity.setPickupDelay(32767); // 일반 줍기 불가
        itemEntity.setUnlimitedLifetime(true);
        itemEntity.setInvulnerable(true);
        
        RelicDefinition def = RelicDefinition.getByNumber(RelicItemUtil.getRelicNumber(relic));
        if (def != null) {
            itemEntity.customName(Component.text("§c[봉인 중] " + def.getTierColor() + def.getName() + " §7(" + sealSeconds + "초)"));
            itemEntity.setCustomNameVisible(true);
        }
        
        if (plugin.getConfigManager().isSealGlow()) {
            itemEntity.setGlowing(true);
        }

        // PDC 태그 설정
        itemEntity.getPersistentDataContainer().set(RelicItemUtil.KEY_IS_RELIC, PersistentDataType.BYTE, (byte) 1);
        itemEntity.getPersistentDataContainer().set(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG, 
                System.currentTimeMillis() + (sealSeconds * 1000L));

        // 얇은 인터렉션 엔티티 생성 후 아이템에 탑승시킴 (같이 이동/추락함)
        org.bukkit.entity.Interaction interaction = location.getWorld().spawn(location, org.bukkit.entity.Interaction.class, ent -> {
            ent.setInteractionWidth(1.0f);
            ent.setInteractionHeight(0.3f); // 바닥에 얇게
            ent.getPersistentDataContainer().set(RelicItemUtil.KEY_IS_RELIC, PersistentDataType.BYTE, (byte) 1);
        });

        itemEntity.addPassenger(interaction);

        startUnsealTimer(itemEntity, relic, sealSeconds);
    }

    private void startUnsealTimer(org.bukkit.entity.Item itemEntity, ItemStack originalRelic, int seconds) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                if (!itemEntity.isValid()) {
                    cancelTask(itemEntity.getUniqueId());
                    return;
                }

                if (timeLeft <= 0) {
                    // 봉인 해제
                    RelicDefinition def = RelicDefinition.getByNumber(RelicItemUtil.getRelicNumber(originalRelic));
                    if (def != null) {
                        itemEntity.customName(Component.text("§a[우클릭으로 획득] " + def.getTierColor() + def.getName()));
                    }
                    itemEntity.setGlowing(false);
                    // 봉인 완료 태그
                    itemEntity.getPersistentDataContainer().set(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG, 0L);
                    // === 봉인 해제 이펙트 ===
                    InteractionEffects.playUnsealEffect(itemEntity.getLocation());
                    cancelTask(itemEntity.getUniqueId());
                    return;
                }

                // 타이머 업데이트
                RelicDefinition def = RelicDefinition.getByNumber(RelicItemUtil.getRelicNumber(originalRelic));
                if (def != null) {
                    itemEntity.customName(Component.text("§c[봉인 중] " + def.getTierColor() + def.getName() + " §7(" + timeLeft + "초)"));
                }

                timeLeft--;
            }
        }, 0L, 20L);

        unsealTasks.put(itemEntity.getUniqueId(), task);
    }

    private void cancelTask(UUID displayId) {
        BukkitTask task = unsealTasks.remove(displayId);
        if (task != null) task.cancel();
    }

    // 상자 등에 봉인된 유물이 일반 아이템처럼 들어가는 것 방지 (이미 PickupDelay로 막았지만 이중 방지)
    @EventHandler
    public void onEntityPickupItem(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getItem().getPersistentDataContainer().has(RelicItemUtil.KEY_IS_RELIC, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    // ======================== 봉인 유물 획득 ========================

    @EventHandler
    public void onPlayerInteractSealedRelic(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof org.bukkit.entity.Interaction interaction)) return;
        if (!interaction.getPersistentDataContainer().has(RelicItemUtil.KEY_IS_RELIC, PersistentDataType.BYTE)) return;

        Player player = event.getPlayer();

        // 다운된 유저는 주울 수 없음
        if (plugin.getCombatManager().isDowned(player)) return;

        // #010 EMP 상태인 경우 주울 수 없음
        if (plugin.getRelicAbilityHandler().active010EMP.contains(player.getUniqueId())) {
            player.sendMessage("§c[EMP] 기능이 마비되어 유물을 주울 수 없습니다!");
            return;
        }

        org.bukkit.entity.Entity vehicle = interaction.getVehicle();
        org.bukkit.entity.Item tempItem = null;
        if (vehicle instanceof org.bukkit.entity.Item i) {
            tempItem = i;
        } else {
            // 탑승이 풀린 경우 대비
            for (org.bukkit.entity.Entity e : interaction.getNearbyEntities(1, 1, 1)) {
                if (e instanceof org.bukkit.entity.Item i && i.getPersistentDataContainer().has(RelicItemUtil.KEY_IS_RELIC, PersistentDataType.BYTE)) {
                    tempItem = i;
                    break;
                }
            }
        }
        
        if (tempItem == null) return;
        final org.bukkit.entity.Item targetItem = tempItem;
        final org.bukkit.entity.Interaction finalInteraction = interaction;

        Long unsealTime = targetItem.getPersistentDataContainer().get(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG);
        if (unsealTime != null && unsealTime > 0) {
            player.sendMessage("§c[RelicWars] 아직 유물의 봉인이 풀리지 않았습니다!");
            return;
        }

        if (pickupTasks.containsKey(player.getUniqueId())) {
            player.sendMessage("§c[RelicWars] 이미 유물을 줍는 중입니다!");
            return;
        }

        player.sendMessage("§a[RelicWars] 유물 줍기를 시작합니다! (유물에 시선을 유지하세요)");
        Location startLoc = player.getLocation().clone();
        final int pickupSeconds = plugin.getConfigManager().getPickupSeconds();
        final int requiredTicks = pickupSeconds * 20;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !targetItem.isValid() || plugin.getCombatManager().isDowned(player)) {
                    pickupTasks.remove(player.getUniqueId()).cancel();
                    player.sendActionBar(Component.text("§c유물 줍기 취소됨"));
                    return;
                }

                // 이동 체크 (너무 멀어지면 취소)
                if (player.getLocation().distanceSquared(startLoc) > 2.0 || player.getLocation().distanceSquared(targetItem.getLocation()) > 16.0) {
                    pickupTasks.remove(player.getUniqueId()).cancel();
                    player.sendActionBar(Component.text("§c유물 줍기 취소됨 (너무 많이 움직였습니다)"));
                    return;
                }
                
                // 시선 유지 체크
                org.bukkit.entity.Entity targetEnt = player.getTargetEntity(5);
                if (targetEnt == null || !targetEnt.equals(finalInteraction)) {
                    pickupTasks.remove(player.getUniqueId()).cancel();
                    player.sendActionBar(Component.text("§c유물 줍기 취소됨 (시선을 뗐습니다)"));
                    return;
                }

                ticks++;

                // 게이지 바 생성
                int bars = (int) ((double) ticks / requiredTicks * 10);
                StringBuilder gauge = new StringBuilder("§a");
                for (int i = 0; i < 10; i++) {
                    if (i == bars) gauge.append("§7");
                    gauge.append("■");
                }
                player.sendActionBar(Component.text("§e유물 줍는 중... [" + gauge.toString() + "§e] " + String.format("%.1f", ticks / 20.0) + "초 / " + pickupSeconds + ".0초"));

                // === 줍기 진행 중 틱 이펙트 ===
                InteractionEffects.playPickupTickEffect(player, targetItem.getLocation(), (float) ticks / requiredTicks);

                if (ticks >= requiredTicks) {
                    pickupTasks.remove(player.getUniqueId()).cancel();

                    // 줍기 성공!
                    ItemStack pickedUpRelic = targetItem.getItemStack();
                    int relicNum = RelicItemUtil.getRelicNumber(pickedUpRelic);
                    
                    if (plugin.getRelicManager().countPlayerRelics(player) >= plugin.getConfigManager().getMaxRelicsPerPlayer()) {
                        player.sendMessage("§c[RelicWars] 유물 소지 한도를 초과했습니다.");
                        return;
                    }

                    player.getInventory().addItem(pickedUpRelic);
                    RelicDefinition def = RelicDefinition.getByNumber(relicNum);
                    if (def != null) {
                        player.sendMessage("§a[RelicWars] " + def.getDisplayName() + " §a유물을 획득했습니다!");
                        Bukkit.broadcast(Component.text("§e[소문] 누군가 " + def.getTierColor() + def.getName() + " §e유물의 봉인을 풀었습니다!"));
                        // === 획득 이펙트 ===
                        InteractionEffects.playRelicAcquireEffect(player, def, plugin);
                    }

                    targetItem.remove();
                    finalInteraction.remove();

                    // DB 업데이트: 소지 상태로 변경
                    plugin.getDatabaseManager().updateRelicState(relicNum, "held", player.getUniqueId().toString(), player.getLocation());
                }
            }
        }, 0L, 1L);

        pickupTasks.put(player.getUniqueId(), task);
    }

    // ======================== 능력 연동 API ========================

    /**
     * 활성 봉인 유물의 Item 목록을 반환합니다.
     */
    public java.util.List<org.bukkit.entity.Item> getActiveSealedRelics() {
        java.util.List<org.bukkit.entity.Item> result = new java.util.ArrayList<>();
        for (UUID id : unsealTasks.keySet()) {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                org.bukkit.entity.Entity entity = world.getEntity(id);
                if (entity instanceof org.bukkit.entity.Item item && item.isValid()) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    /**
     * 주변에 있는 봉인된 유물(Item엔티티)을 찾는 유틸
     */
    public org.bukkit.entity.Item getNearestSealed(Location loc, double radius) {
        org.bukkit.entity.Item nearest = null;
        double minDist = Double.MAX_VALUE;
        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e instanceof org.bukkit.entity.Item item) {
                if (item.getPersistentDataContainer().has(RelicItemUtil.KEY_IS_RELIC, PersistentDataType.BYTE)) {
                    double dist = e.getLocation().distanceSquared(loc);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = item;
                    }
                }
            }
        }
        return nearest;
    }

    /**
     * 봉인을 즉시 해제하여 획득 가능 상태로 만듭니다. (#009 파괴자의 서)
     */
    public void forceUnseal(org.bukkit.entity.Item targetItem) {
        cancelTask(targetItem.getUniqueId());
        ItemStack relic = targetItem.getItemStack();
        if (relic != null) {
            RelicDefinition def = RelicDefinition.getByNumber(RelicItemUtil.getRelicNumber(relic));
            if (def != null) {
                targetItem.customName(Component.text("§a[우클릭으로 획득] " + def.getTierColor() + def.getName()));
            }
        }
        targetItem.setGlowing(false);
        targetItem.getPersistentDataContainer().set(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG, 0L);
    }

    /**
     * 남은 봉인 시간을 절반으로 줄입니다. (#019 봉인의 바늘)
     */
    public void reduceSealTime(org.bukkit.entity.Item targetItem, double ratio) {
        BukkitTask task = unsealTasks.get(targetItem.getUniqueId());
        if (task == null) return;
        
        Long currentUntil = targetItem.getPersistentDataContainer().get(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG);
        if (currentUntil == null || currentUntil == 0) return;

        long now = System.currentTimeMillis();
        long timeLeftMillis = currentUntil - now;
        if (timeLeftMillis <= 0) return;

        long newTimeLeftMillis = (long) (timeLeftMillis * ratio);
        int newTimeLeftSeconds = (int) (newTimeLeftMillis / 1000L);
        
        targetItem.getPersistentDataContainer().set(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG, now + newTimeLeftMillis);
        
        // 태스크 재시작
        cancelTask(targetItem.getUniqueId());
        startUnsealTimer(targetItem, targetItem.getItemStack(), newTimeLeftSeconds);
    }
}
