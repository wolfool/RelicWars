package com.wolfool.relicwars.relic;

import com.wolfool.relicwars.RelicWars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class RelicAcquisitionListener implements Listener {

    private final RelicWars plugin;
    private final Map<UUID, Double> fallStartY = new HashMap<>();
    private BukkitTask fallTrackerTask;

    // #025 최후의 봉합: 10분 안에 3회 구조 추적 (rescuer UUID -> 구조 성공 시간 목록)
    private final Map<UUID, List<Long>> reviveTracker025 = new HashMap<>();

    public RelicAcquisitionListener(RelicWars plugin) {
        this.plugin = plugin;
        startFallTracker();
    }

    // ======================== #030 낙뢰의 심지 ========================

    @EventHandler
    public void onPlayerLightningStrike(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) return;

        // 데미지를 받은 후 살아남았는지 & 남은 체력이 10.0(5칸) 이하인지 확인
        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth > 0 && finalHealth <= 10.0) {
            // 기믹 달성!
            spawnRelicIfUnspawned(30, player.getLocation(), player);
        }
    }

    // ======================== #029 추락왕의 깃털 ========================

    private void startFallTracker() {
        fallTrackerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getCombatManager().isDowned(player)) continue;

                boolean isFalling = !player.isOnGround() && !player.isInWater() && !player.isFlying() 
                        && !player.isGliding() && !player.isSwimming() && !player.isInsideVehicle() 
                        && player.getVelocity().getY() < -0.1
                        && !player.hasPotionEffect(PotionEffectType.SLOW_FALLING)
                        && !player.hasPotionEffect(PotionEffectType.LEVITATION);

                if (isFalling) {
                    fallStartY.putIfAbsent(player.getUniqueId(), player.getLocation().getY());
                } else {
                    Double startY = fallStartY.remove(player.getUniqueId());
                    if (startY != null) {
                        double endY = player.getLocation().getY();
                        
                        // Y=319 이상에서 시작하여 Y=-60 이하로 떨어졌고, 죽지 않고 살았다면
                        if (startY >= 319.0 && endY <= -60.0 && !player.isDead()) {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (!player.isDead() && !plugin.getCombatManager().isDowned(player)) {
                                    spawnRelicIfUnspawned(29, player.getLocation(), player);
                                }
                            }, 5L);
                        }
                    }
                }
            }
        }, 0L, 2L);
    }

    // ======================== #025 최후의 봉합 & #024 붉은 봉합 ========================

    /**
     * CombatListener에서 구조 완료 시 호출합니다.
     * @param rescuer 구조한 사람
     * @param target 구조받은 사람
     */
    public void onReviveSuccess(Player rescuer, Player target) {
        // --- #024 붉은 봉합: 체력 2칸(4.0) 이하에서 팀원 구조 성공 ---
        if (rescuer.getHealth() <= 4.0) {
            spawnRelicIfUnspawned(24, rescuer.getLocation(), rescuer);
        }

        // --- #025 최후의 봉합: 10분 안에 3회 구조 성공 ---
        UUID rescuerId = rescuer.getUniqueId();
        long now = System.currentTimeMillis();
        long tenMinutes = 10 * 60 * 1000L;

        List<Long> times = reviveTracker025.computeIfAbsent(rescuerId, k -> new ArrayList<>());
        times.add(now);

        // 10분이 지난 기록 제거
        times.removeIf(t -> (now - t) > tenMinutes);

        if (times.size() >= 3) {
            spawnRelicIfUnspawned(25, rescuer.getLocation(), rescuer);
            times.clear();
        }
    }

    // ======================== 공통 스폰 유틸리티 ========================

    private void spawnRelicIfUnspawned(int relicNum, Location loc, Player achiever) {
        String state = plugin.getDatabaseManager().getRelicState(relicNum);
        if ("unspawned".equals(state)) {
            RelicDefinition def = RelicDefinition.getByNumber(relicNum);
            if (def != null) {
                plugin.getDatabaseManager().updateRelicState(relicNum, "held", achiever.getUniqueId().toString(), achiever.getLocation());
                
                ItemStack relic = com.wolfool.relicwars.relic.RelicItemUtil.createRelicItem(def);
                
                java.util.HashMap<Integer, ItemStack> leftOver = achiever.getInventory().addItem(relic);
                if (!leftOver.isEmpty()) {
                    achiever.getWorld().dropItem(achiever.getLocation(), leftOver.get(0));
                }
                
                Bukkit.broadcast(net.kyori.adventure.text.Component.text("§e[소문] §f누군가 특별한 조건을 달성하여 " + def.getTierColor() + def.getName() + "§f 유물이 세상에 모습을 드러냈습니다!"));
                achiever.sendMessage("§a[RelicWars] 기믹 달성! 인벤토리에 유물이 지급되었습니다.");

                // === 기믹 달성 이펙트 ===
                InteractionEffects.playGimmickCompleteEffect(achiever, def, plugin);
            }
        }
    }

    /**
     * 플러그인 종료 시 태스크 취소 및 메모리 정리
     */
    public void shutdown() {
        if (fallTrackerTask != null && !fallTrackerTask.isCancelled()) fallTrackerTask.cancel();
        fallStartY.clear();
        reviveTracker025.clear();
    }
}
