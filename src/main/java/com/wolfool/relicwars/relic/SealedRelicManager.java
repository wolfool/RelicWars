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

    // 플레이어 UUID -> 줍기 태스크
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
     * 바닥에 봉인된 유물을 생성합니다.
     *
     * @param location 드랍할 위치
     * @param relic    유물 아이템
     * @param sealSeconds 봉인 지속 시간 (초)
     */
    public void spawnSealedRelic(Location location, ItemStack relic, int sealSeconds) {
        if (relic == null || !RelicItemUtil.isRelic(relic)) return;

        Location spawnLoc = location.clone().add(0, 0.5, 0);
        
        ItemDisplay display = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(relic);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            
            // 시각적 효과 (회전 및 크기)
            Transformation transform = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1.5f, 1.5f, 1.5f),
                    new AxisAngle4f(0, 0, 1, 0)
            );
            entity.setTransformation(transform);
            
            // 이름표 표시
            RelicDefinition def = RelicDefinition.getByNumber(RelicItemUtil.getRelicNumber(relic));
            if (def != null) {
                entity.customName(Component.text("§c[봉인 중] " + def.getTierColor() + def.getName() + " §7(" + sealSeconds + "초)"));
                entity.setCustomNameVisible(true);
            }
            
            if (plugin.getConfigManager().isSealGlow()) {
                entity.setGlowing(true);
            }

            // PDC 태그: 봉인 상태
            entity.getPersistentDataContainer().set(RelicItemUtil.KEY_IS_RELIC, PersistentDataType.BYTE, (byte) 1);
            entity.getPersistentDataContainer().set(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG, 
                    System.currentTimeMillis() + (sealSeconds * 1000L)); // 봉인 해제 시간을 쿨타임 태그로 임시 사용
        });

        startUnsealTimer(display, relic, sealSeconds);
    }

    private void startUnsealTimer(ItemDisplay display, ItemStack originalRelic, int seconds) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                if (!display.isValid()) {
                    cancelTask(display.getUniqueId());
                    return;
                }

                if (timeLeft <= 0) {
                    // 봉인 해제
                    RelicDefinition def = RelicDefinition.getByNumber(RelicItemUtil.getRelicNumber(originalRelic));
                    if (def != null) {
                        display.customName(Component.text("§a[Shift로 획득] " + def.getTierColor() + def.getName()));
                    }
                    display.setGlowing(false);
                    // 봉인 완료 태그
                    display.getPersistentDataContainer().set(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG, 0L);
                    cancelTask(display.getUniqueId());
                    return;
                }

                // 타이머 업데이트
                RelicDefinition def = RelicDefinition.getByNumber(RelicItemUtil.getRelicNumber(originalRelic));
                if (def != null) {
                    display.customName(Component.text("§c[봉인 중] " + def.getTierColor() + def.getName() + " §7(" + timeLeft + "초)"));
                }
                
                // 디스플레이 회전 애니메이션
                Transformation t = display.getTransformation();
                t.getRightRotation().rotateY(0.1f);
                display.setTransformation(t);

                timeLeft--;
            }
        }, 0L, 20L);

        unsealTasks.put(display.getUniqueId(), task);
    }

    private void cancelTask(UUID displayId) {
        BukkitTask task = unsealTasks.remove(displayId);
        if (task != null) task.cancel();
    }

    // ======================== 봉인 유물 획득 ========================

    @EventHandler
    public void onPlayerSneakForRelic(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!event.isSneaking()) {
            // 웅크리기 해제 시 줍기 취소
            BukkitTask task = pickupTasks.remove(player.getUniqueId());
            if (task != null) {
                task.cancel();
                player.sendActionBar(Component.text("§c유물 줍기 취소됨"));
            }
            return;
        }

        // 다운된 유저는 주울 수 없음
        if (plugin.getCombatManager().isDowned(player)) return;

        // 반경 3.0 블록 내의 봉인 유물 찾기
        ItemDisplay display = getNearestSealed(player.getLocation(), 3.0);
        if (display == null) return;
        
        if (!display.getPersistentDataContainer().has(RelicItemUtil.KEY_IS_RELIC, PersistentDataType.BYTE)) {
            return;
        }

        Long unsealTime = display.getPersistentDataContainer().get(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG);
        if (unsealTime != null && unsealTime > 0) {
            player.sendMessage("§c[RelicWars] 아직 유물의 봉인이 풀리지 않았습니다!");
            return;
        }

        // 인벤토리에 여유가 있는지 (유물 소지 한도) 미리 체크
        int count = plugin.getRelicManager().countPlayerRelics(player);
        int max = plugin.getConfigManager().getMaxRelicsPerPlayer();
        if (count >= max) {
            player.sendMessage("§c[RelicWars] 유물 소지 한도(" + max + "개)를 초과하여 주울 수 없습니다.");
            return;
        }

        // 기존 줍기 태스크가 있다면 무시
        if (pickupTasks.containsKey(player.getUniqueId())) return;

        // 2초(40틱) 동안 웅크리기 유지 시 줍기
        final int requiredTicks = 40;
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !player.isSneaking() || !display.isValid() || player.getLocation().distance(display.getLocation()) > 4.0 || plugin.getCombatManager().isDowned(player)) {
                    pickupTasks.remove(player.getUniqueId()).cancel();
                    player.sendActionBar(Component.text("§c유물 줍기 취소됨"));
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
                player.sendActionBar(Component.text("§e유물 줍는 중... [" + gauge.toString() + "§e] " + String.format("%.1f", ticks / 20.0) + "초 / 2.0초"));

                if (ticks >= requiredTicks) {
                    pickupTasks.remove(player.getUniqueId()).cancel();

                    ItemStack relic = display.getItemStack();
                    if (relic == null) return;

                    // 다시 한번 한도 체크
                    if (plugin.getRelicManager().countPlayerRelics(player) >= plugin.getConfigManager().getMaxRelicsPerPlayer()) {
                        player.sendMessage("§c[RelicWars] 유물 소지 한도를 초과했습니다.");
                        return;
                    }

                    player.getInventory().addItem(relic);
                    RelicDefinition def = RelicDefinition.getByNumber(RelicItemUtil.getRelicNumber(relic));
                    if (def != null) {
                        player.sendMessage("§a[RelicWars] " + def.getDisplayName() + " §a유물을 획득했습니다!");
                        player.sendActionBar(Component.text("§a유물 획득 완료!"));
                        Bukkit.broadcast(Component.text("§e[소문] 누군가 " + def.getTierColor() + def.getName() + " §e유물의 봉인을 풀었습니다!"));
                    }

                    cancelTask(display.getUniqueId());
                    display.remove();
                }
            }
        }, 0L, 1L);

        pickupTasks.put(player.getUniqueId(), task);
    }

    // ======================== 능력 연동 API ========================

    /**
     * 활성 봉인 유물의 ItemDisplay 목록을 반환합니다.
     */
    public List<ItemDisplay> getActiveSealedRelics() {
        List<ItemDisplay> result = new ArrayList<>();
        for (UUID id : unsealTasks.keySet()) {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                org.bukkit.entity.Entity entity = world.getEntity(id);
                if (entity instanceof ItemDisplay display && display.isValid()) {
                    result.add(display);
                }
            }
        }
        return result;
    }

    /**
     * 특정 위치에서 가장 가까운 봉인 유물을 반환합니다.
     * @param loc 기준 위치
     * @param maxRange 최대 검색 범위 (블록)
     * @return 가장 가까운 봉인 ItemDisplay, 없으면 null
     */
    public ItemDisplay getNearestSealed(Location loc, double maxRange) {
        ItemDisplay nearest = null;
        double minDist = Double.MAX_VALUE;
        for (ItemDisplay display : getActiveSealedRelics()) {
            if (!display.getWorld().equals(loc.getWorld())) continue;
            double dist = display.getLocation().distance(loc);
            if (dist <= maxRange && dist < minDist) {
                minDist = dist;
                nearest = display;
            }
        }
        return nearest;
    }

    /**
     * 봉인을 즉시 해제하여 획득 가능 상태로 만듭니다. (#009 파괴자의 서)
     */
    public void forceUnseal(ItemDisplay display) {
        cancelTask(display.getUniqueId());
        ItemStack relic = display.getItemStack();
        if (relic != null) {
            RelicDefinition def = RelicDefinition.getByNumber(RelicItemUtil.getRelicNumber(relic));
            if (def != null) {
                display.customName(Component.text("§a[Shift로 획득] " + def.getTierColor() + def.getName()));
            }
        }
        display.setGlowing(false);
        display.getPersistentDataContainer().set(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG, 0L);
    }

    /**
     * 봉인 시간을 절반으로 단축합니다. (#019 봉인의 바늘)
     */
    public void halveSealTime(ItemDisplay display) {
        Long unsealTime = display.getPersistentDataContainer().get(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG);
        if (unsealTime != null && unsealTime > 0) {
            long now = System.currentTimeMillis();
            long remaining = unsealTime - now;
            if (remaining > 0) {
                long halved = now + (remaining / 2);
                display.getPersistentDataContainer().set(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG, halved);
            }
        }
    }
}
