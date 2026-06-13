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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
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
                        display.customName(Component.text("§a[획득 가능] " + def.getTierColor() + def.getName()));
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
    public void onInteractSealedRelic(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemDisplay display)) return;
        
        if (!display.getPersistentDataContainer().has(RelicItemUtil.KEY_IS_RELIC, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        // 다운된 유저는 주울 수 없음
        // TODO: CombatManager 체크 (player.isDowned())

        Long unsealTime = display.getPersistentDataContainer().get(RelicItemUtil.KEY_COOLDOWN_UNTIL, PersistentDataType.LONG);
        if (unsealTime != null && unsealTime > 0) {
            player.sendMessage("§c[RelicWars] 아직 유물의 봉인이 풀리지 않았습니다!");
            return;
        }

        ItemStack relic = display.getItemStack();
        if (relic == null) return;

        // 인벤토리에 지급 시도
        int count = plugin.getRelicManager().countPlayerRelics(player);
        int max = plugin.getConfigManager().getMaxRelicsPerPlayer();
        if (count >= max) {
            player.sendMessage("§c[RelicWars] 유물 소지 한도(" + max + "개)를 초과하여 주울 수 없습니다.");
            return;
        }

        player.getInventory().addItem(relic);
        RelicDefinition def = RelicDefinition.getByNumber(RelicItemUtil.getRelicNumber(relic));
        if (def != null) {
            player.sendMessage("§a[RelicWars] " + def.getDisplayName() + " §a유물을 획득했습니다!");
            Bukkit.broadcast(Component.text("§e[소문] 누군가 " + def.getTierColor() + def.getName() + " §e유물의 봉인을 풀었습니다!"));
        }

        display.remove();
    }
}
