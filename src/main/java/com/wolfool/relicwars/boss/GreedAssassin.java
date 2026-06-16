package com.wolfool.relicwars.boss;

import com.wolfool.relicwars.RelicWars;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 3단계 유물 #013 탐욕의 뼈를 드랍하는 이벤트 보스
 * '탐욕의 추적자' 구현 클래스
 */
public class GreedAssassin {

    private final RelicWars plugin;
    private final Player target;
    private WitherSkeleton entity;

    public GreedAssassin(RelicWars plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
    }

    public void spawn(Location loc) {
        entity = (WitherSkeleton) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
        
        // 보스 기본 세팅
        entity.setCustomName("§4[보스] 탐욕의 추적자");
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);
        
        // 타겟이 가진 유물 개수에 비례하여 강해짐
        int relicCount = plugin.getRelicManager().countPlayerRelics(target);
        double maxHp = 100 + (relicCount * 50);
        
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHp);
        entity.setHealth(maxHp);
        entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.35); // 빠른 이속
        entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(15.0);  // 강력한 딜

        // 장비
        entity.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
        entity.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        entity.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
        
        // 스폰 시 투명화 걸기 (기습용)
        entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
        
        // 타겟 고정
        entity.setTarget(target);

        // 보스 AI 패턴 스케줄러
        startPatterns();
    }

    private void startPatterns() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || entity.isDead() || !entity.isValid()) {
                    this.cancel();
                    // 사망 시 드랍 이벤트는 일반 EntityDeathEvent 에서 처리하거나 여기서 처리 가능
                    // (MVP 에서는 EntityDeathEvent로 일괄 처리 권장, 혹은 여기서 #013 뼈 드랍을 강제로 진행)
                    return;
                }

                // 타겟이 바뀌었으면 다시 원래 타겟(탐욕스러운 자)을 물게 함
                if (entity.getTarget() != target) {
                    entity.setTarget(target);
                }

                // 특수 패턴: 간헐적 투명화 & 텔레포트 기습
                if (Math.random() < 0.2) {
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, false, false));
                    Location backLoc = target.getLocation().clone().subtract(target.getLocation().getDirection().multiply(2));
                    entity.teleport(backLoc);
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // 2초마다 AI 점검
    }
}
