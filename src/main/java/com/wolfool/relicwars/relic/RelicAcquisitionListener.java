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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RelicAcquisitionListener implements Listener {

    private final RelicWars plugin;
    private final Map<UUID, Double> fallStartY = new HashMap<>();
    private BukkitTask fallTrackerTask;

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
                    // 추락 시작 지점 기록 (아직 없으면 기록)
                    fallStartY.putIfAbsent(player.getUniqueId(), player.getLocation().getY());
                } else {
                    // 추락이 끝났거나(착지/물에 빠짐), 비정상적인 체공(비행/겉날개 등)을 한 경우
                    Double startY = fallStartY.remove(player.getUniqueId());
                    if (startY != null) {
                        // 추락이 끝난 시점의 Y 좌표
                        double endY = player.getLocation().getY();
                        
                        // Y=319 이상에서 시작하여 Y=-60 이하로 떨어졌고, 죽지 않고 살았다면 (물 양동이 낙법 등)
                        if (startY >= 319.0 && endY <= -60.0 && !player.isDead()) {
                            // 기믹 달성! (1틱 지연하여 데미지로 죽는지 여부 최종 확인 후 지급)
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (!player.isDead() && !plugin.getCombatManager().isDowned(player)) {
                                    spawnRelicIfUnspawned(29, player.getLocation(), player);
                                }
                            }, 5L);
                        }
                    }
                }
            }
        }, 0L, 2L); // 2틱(0.1초) 주기로 추적
    }

    // ======================== 공통 스폰 유틸리티 ========================

    private void spawnRelicIfUnspawned(int relicNum, Location loc, Player achiever) {
        String state = plugin.getDatabaseManager().getRelicState(relicNum);
        if ("unspawned".equals(state)) {
            RelicDefinition def = RelicDefinition.getByNumber(relicNum);
            if (def != null) {
                // 이미 스폰되었다고 DB에 업데이트 (소지 중 상태로)
                plugin.getDatabaseManager().updateRelicState(relicNum, "held", achiever.getUniqueId().toString(), achiever.getLocation());
                
                ItemStack relic = RelicItemUtil.createRelicItem(def);
                
                // 인벤토리에 지급 (공간이 없으면 바닥에 일반 드랍)
                java.util.HashMap<Integer, ItemStack> leftOver = achiever.getInventory().addItem(relic);
                if (!leftOver.isEmpty()) {
                    achiever.getWorld().dropItem(achiever.getLocation(), leftOver.get(0));
                }
                
                Bukkit.broadcast(net.kyori.adventure.text.Component.text("§e[소문] §f누군가 특별한 조건을 달성하여 " + def.getTierColor() + def.getName() + "§f 유물이 세상에 모습을 드러냈습니다!"));
                achiever.sendMessage("§a[RelicWars] 기믹 달성! 인벤토리에 유물이 지급되었습니다.");
            }
        }
    }
}
