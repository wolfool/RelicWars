package com.wolfool.relicwars.combat;

import com.wolfool.relicwars.RelicWars;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {

    private final RelicWars plugin;
    private final CombatManager combatManager;

    // 구조(부활)를 시도 중인 유저의 타이머 (Rescuer UUID -> Task)
    private final Map<UUID, BukkitTask> reviveTasks = new HashMap<>();

    public CombatListener(RelicWars plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    // ======================== 체력 0 도달 (다운 처리) ========================

    /**
     * 팀원 간의 공격(Friendly Fire)을 설정에 따라 차단합니다.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!plugin.getConfigManager().isFriendlyFireEnabled()) {
            Player victim = null;
            if (event.getEntity() instanceof Player p) victim = p;
            
            Player attacker = null;
            if (event.getDamager() instanceof Player p) attacker = p;
            else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) attacker = p;

            if (victim != null && attacker != null && !victim.equals(attacker)) {
                if (plugin.getTeamManager().isSameTeam(victim, attacker)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // #029 낙하 데미지 면역 체크
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (plugin.getRelicAbilityHandler().active029FallImmunity.contains(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        // 이미 다운된 상태라면
        if (combatManager.isDowned(player)) {
            handleDownedDamage(event, player);
            return;
        }

        // #023 사냥꾼의 표식 10% 추가 피해
        if (plugin.getRelicAbilityHandler().active023Marked.contains(player.getUniqueId())) {
            event.setDamage(event.getDamage() * 1.1);
        }

        // #015 캐스팅 중 피격 시 취소
        if (plugin.getRelicAbilityHandler().active015Casting.remove(player.getUniqueId())) {
            player.sendMessage("§c[회수자의 갈고리] 피격당해 캐스팅이 취소되었습니다.");
        }

        // 다운되지 않은 상태에서 체력이 0 이하로 내려갈 데미지를 받았다면
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            // #005 불멸의 심장 패시브 발동 체크
            org.bukkit.inventory.ItemStack heartRelic = null;
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item) && com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(item) == 5) {
                    heartRelic = item;
                    break;
                }
            }

            if (heartRelic != null && !plugin.getRelicManager().isOnCooldown(player, 5)) {
                event.setCancelled(true);
                
                // 쿨타임 및 정신력 소모
                plugin.getRelicManager().setCooldown(player, 5); // 90분
                plugin.getSanityManager().removeSanity(player, 30);
                
                // 체력 100% 회복
                player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 160, 1, false, false));
                
                player.sendMessage("§6[불멸의 심장] 죽음을 극복하고 부활했습니다!");
                org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text("§6[경고] X:" + player.getLocation().getBlockX() + " 부근에서 강력한 불멸의 기운이 감지되었습니다!"));
                
                player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM, player.getLocation(), 100, 1, 1, 1, 0.5);
                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                
                // 반경 8블록 적 넉백
                for (org.bukkit.entity.Entity e : player.getNearbyEntities(8, 8, 8)) {
                    if (e instanceof Player p && !plugin.getTeamManager().isSameTeam(player, p)) {
                        org.bukkit.util.Vector knockback = p.getLocation().toVector().subtract(player.getLocation().toVector());
                        if (knockback.lengthSquared() == 0) knockback = new org.bukkit.util.Vector(0, 1, 0);
                        else knockback = knockback.normalize().multiply(2.5).setY(0.8);
                        p.setVelocity(knockback);
                        p.sendMessage("§e[불멸의 심장] 거대한 황금빛 충격파에 밀려났습니다!");
                    }
                }
                return;
            }

            // 바닐라 사망 방지 및 다운
            event.setCancelled(true);
            combatManager.setDowned(player);
        }
    }

    private void handleDownedDamage(EntityDamageEvent event, Player victim) {
        // 무적 시간 중이면 무조건 캔슬
        if (combatManager.isInvincible(victim)) {
            event.setCancelled(true);
            return;
        }

        // 환경 데미지 무시 여부 (용암, 낙하 등)
        if (plugin.getConfigManager().isDownedEnvironmentalImmunity()) {
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
                event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                event.setCancelled(true);
                return;
            }
        }

        // 확킬 판정: 오직 플레이어의 근접 타격만 허용
        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            if (byEntityEvent.getDamager() instanceof Player attacker) {
                event.setCancelled(true); // 데미지는 입지 않고 횟수만 차감
                combatManager.addExecuteHit(victim, attacker);
            } else {
                event.setCancelled(true); // 몬스터 공격 무시
            }
        } else {
            event.setCancelled(true); // 다른 데미지 무시
        }
    }

    // ======================== 강탈 인터랙션 ========================

    /**
     * 다운된 적을 웅크린+우클릭하면 강탈 시도
     */
    @EventHandler
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player stealer = event.getPlayer();

        if (!combatManager.isDowned(target)) return;

        // #010 EMP 체크
        if (plugin.getRelicAbilityHandler().active010EMP.contains(stealer.getUniqueId())) {
            stealer.sendMessage("§c[EMP] 기능이 마비되어 상호작용할 수 없습니다!");
            return;
        }
        
        // 같은 팀이면 상호작용 불가
        if (plugin.getTeamManager().isSameTeam(stealer, target)) return;

        // #002 탐욕의 적출자 발동 (우클릭만으로 가능, 웅크리기 불필요)
        org.bukkit.inventory.ItemStack handItem = stealer.getInventory().getItemInMainHand();
        if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(handItem) && com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(handItem) == 2) {
            if (!plugin.getRelicManager().isOnCooldown(stealer, 2)) {
                if (plugin.getSanityManager().consumeSanity(stealer, 30)) {
                    event.setCancelled(true);
                    start002Extract(stealer, target);
                    return;
                }
            } else {
                stealer.sendMessage("§c[탐욕의 적출자] 아직 쿨타임 중입니다!");
            }
        }

        // 일반 강탈은 웅크리기 필수
        if (!stealer.isSneaking()) return;

        event.setCancelled(true);
        combatManager.stealRelics(target, stealer);
    }
    
    private void start002Extract(Player stealer, Player target) {
        stealer.sendMessage("§4[탐욕의 적출자] 대상의 숨통을 끊어 유물을 적출합니다...");
        
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!stealer.isOnline() || !target.isOnline() || !combatManager.isDowned(target) || combatManager.isDowned(stealer)) {
                    this.cancel();
                    stealer.sendMessage("§c[탐욕의 적출자] 적출이 취소되었습니다.");
                    return;
                }

                // 거리 체크
                if (stealer.getLocation().distanceSquared(target.getLocation()) > 16.0) {
                    this.cancel();
                    stealer.sendMessage("§c[탐욕의 적출자] 대상과 너무 멀어졌습니다.");
                    return;
                }

                // 시선 체크
                org.bukkit.entity.Entity targetEnt = stealer.getTargetEntity(5);
                if (targetEnt == null || !targetEnt.equals(target)) {
                    this.cancel();
                    stealer.sendMessage("§c[탐욕의 적출자] 대상에게서 시선을 뗐습니다.");
                    return;
                }

                ticks++;
                if (ticks >= 10) { // 0.5초
                    this.cancel();
                    
                    // 적출 성공 (쿨타임 적용)
                    plugin.getRelicManager().setCooldown(stealer, 2); // 120분

                    // 대상 사망 처리 및 모든 유물 드랍
                    java.util.List<org.bukkit.inventory.ItemStack> allRelics = plugin.getRelicManager().getPlayerRelics(target);
                    for (org.bukkit.inventory.ItemStack relic : allRelics) {
                        target.getInventory().remove(relic);
                        target.getWorld().dropItemNaturally(target.getLocation(), relic);
                    }
                    
                    stealer.sendMessage("§4[탐욕의 적출자] 적출 성공! 대상의 모든 유물이 바닥에 떨어졌습니다.");
                    target.setHealth(0.0); // 즉사
                    
                    stealer.getWorld().spawnParticle(org.bukkit.Particle.CRIT_MAGIC, target.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
                    stealer.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ======================== 다운 상태 제약 ========================

    /**
     * 다운 상태에서는 포션 섭취 등 아이템 사용을 막습니다.
     */
    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (combatManager.isDowned(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c[RelicWars] 다운 상태에서는 아이템을 사용할 수 없습니다.");
        }
    }

    // ======================== 구조 (부활) ========================

    /**
     * 다운된 팀원을 Shift(웅크리기) + 우클릭 시 구조를 시작합니다.
     */
    @EventHandler
    public void onInteractDowned(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player rescuer = event.getPlayer();

        if (combatManager.isDowned(target) && !combatManager.isDowned(rescuer)) {
            // #010 EMP 체크
            if (plugin.getRelicAbilityHandler().active010EMP.contains(rescuer.getUniqueId())) {
                rescuer.sendMessage("§c[EMP] 기능이 마비되어 상호작용할 수 없습니다!");
                return;
            }

            // 이미 구조 중인지 확인
            if (reviveTasks.containsKey(rescuer.getUniqueId())) {
                rescuer.sendMessage("§c[RelicWars] 이미 구조 중입니다!");
                return;
            }

            // TODO: TeamManager 검증 (Phase 3 에서는 누구나 구조 가능하게 임시 설정)
            
            startRevive(rescuer, target);
        }
    }

    private void startRevive(Player rescuer, Player target) {
        rescuer.sendMessage("§a[RelicWars] 구조를 시작합니다! (우클릭을 꾹 누르며 시선을 고정하세요)");
        target.sendMessage("§a[RelicWars] 팀원이 당신을 구조 중입니다...");

        Location startLoc = rescuer.getLocation().clone();
        
        // #025 최후의 봉합 구조 시간 단축 체크
        int reviveSeconds = plugin.getConfigManager().getReviveSeconds();
        if (plugin.getRelicAbilityHandler().active025FastRevive.contains(rescuer.getUniqueId()) ||
            plugin.getRelicAbilityHandler().active025FastRevive.contains(target.getUniqueId())) {
            reviveSeconds = 2; // 8초 -> 2초로 대폭 단축
            rescuer.sendMessage("§d[최후의 봉합] §5구조 시간이 대폭 단축됩니다!");
        }
        
        // #007 파수꾼의 돔 구조 시간 3초 단축 체크
        String rescuerTeam = plugin.getTeamManager().getTeamId(rescuer);
        String rescuerKey = rescuerTeam != null ? rescuerTeam : rescuer.getUniqueId().toString();
        for (Map.Entry<Location, String> entry : plugin.getRelicAbilityHandler().active007Dome.entrySet()) {
            if (entry.getValue().equals(rescuerKey) && rescuer.getLocation().distanceSquared(entry.getKey()) <= 64) {
                if (reviveSeconds > 3) reviveSeconds = 3;
                rescuer.sendMessage("§b[파수꾼의 돔] §f돔 내부의 보호를 받아 구조 시간이 단축됩니다!");
                break;
            }
        }

        final int finalReviveSeconds = reviveSeconds;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            final int totalTicks = finalReviveSeconds * 20;

            @Override
            public void run() {
                // 구조자나 타겟이 접속 종료하거나 타겟이 죽은 경우
                if (!rescuer.isOnline() || !target.isOnline() || !combatManager.isDowned(target) || combatManager.isDowned(rescuer)) {
                    cancelRevive(rescuer.getUniqueId(), "구조 대상이나 구조자의 상태가 변경되었습니다.");
                    return;
                }

                // 거리가 멀어졌는지 확인
                if (rescuer.getLocation().distanceSquared(startLoc) > 4.0 || rescuer.getLocation().distanceSquared(target.getLocation()) > 16.0) {
                    cancelRevive(rescuer.getUniqueId(), "구조 대상과 너무 멀어졌거나 많이 움직였습니다.");
                    return;
                }

                // 시선 유지 확인 (십자선이 타겟을 향하고 있는지)
                org.bukkit.entity.Entity targetEnt = rescuer.getTargetEntity(5);
                if (targetEnt == null || !targetEnt.equals(target)) {
                    cancelRevive(rescuer.getUniqueId(), "구조 대상에서 시선을 뗐습니다.");
                    return;
                }

                // 웅크리기 해제 조건 제거 (이제 움직이지만 않으면 됨)
                
                ticks += 5; // 0.25초마다 체크
                
                // 게이지 바 생성
                int bars = (int) ((double) ticks / totalTicks * 10);
                StringBuilder gauge = new StringBuilder("§a");
                for (int i = 0; i < 10; i++) {
                    if (i == bars) gauge.append("§7");
                    gauge.append("■");
                }
                rescuer.sendActionBar(Component.text("§e구조 중... [" + gauge.toString() + "§e] " + String.format("%.1f", ticks / 20.0) + "초 / " + finalReviveSeconds + ".0초"));

                if (ticks >= totalTicks) {
                    // 구조 완료
                    combatManager.revivePlayer(target);
                    rescuer.sendMessage("§a[RelicWars] 구조를 성공적으로 마쳤습니다!");

                    // === 구조 완료 이펙트 ===
                    com.wolfool.relicwars.relic.InteractionEffects.playReviveEffect(rescuer, target, plugin);

                    // 기믹 체크: #025 최후의 봉합, #024 붉은 봉합
                    if (plugin.getAcquisitionListener() != null) {
                        plugin.getAcquisitionListener().onReviveSuccess(rescuer, target);
                    }
                    
                    // #025 최후의 봉합 도주 버프
                    if (plugin.getRelicAbilityHandler().active025FastRevive.contains(rescuer.getUniqueId()) ||
                        plugin.getRelicAbilityHandler().active025FastRevive.contains(target.getUniqueId())) {
                        
                        target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 200, 1, false, false));
                        target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 100, 0, false, false));
                        rescuer.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 200, 1, false, false));
                        rescuer.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 100, 0, false, false));
                        
                        target.sendMessage("§d[최후의 봉합] §5강력한 도주 버프가 적용되었습니다!");
                        rescuer.sendMessage("§d[최후의 봉합] §5강력한 도주 버프가 적용되었습니다!");
                        
                        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(rescuer.getLocation(), "§b[소문] %s쪽에서 다급한 심박음이 들렸습니다.");
                    }
                    
                    reviveTasks.remove(rescuer.getUniqueId()).cancel();
                }
            }
        }, 0L, 5L); // 5틱(0.25초)마다 체크

        reviveTasks.put(rescuer.getUniqueId(), task);
    }

    private void cancelRevive(UUID rescuerId, String reason) {
        BukkitTask task = reviveTasks.remove(rescuerId);
        if (task != null) {
            task.cancel();
            Player rescuer = Bukkit.getPlayer(rescuerId);
            if (rescuer != null) rescuer.sendMessage("§c[RelicWars] 구조 취소: " + reason);
        }
    }

    // ======================== 랜뽑 방지 ========================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (combatManager.isDowned(player)) {
            // 다운 상태에서 나가면 즉시 사망 처리
            combatManager.killPlayer(player, null);
        }
    }

    // ======================== KeepInventory 처리 ========================

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.getConfigManager().isKeepInventoryOnDeath()) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
    }
}
