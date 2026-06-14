package com.wolfool.relicwars.relic;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import com.wolfool.relicwars.RelicWars;

/**
 * 유물 상호작용 시 임팩트 있는 시각/청각 이펙트를 제공합니다.
 * 유물 줍기 완료, 기믹 달성, 구조 완료, 다운, 확킬 등에서 사용합니다.
 */
public class InteractionEffects {

    // ======================== 유물 획득 (줍기/기믹 달성) ========================

    /**
     * 유물 획득 시 화려한 임팩트 이펙트
     * - 황금 폭발 파티클
     * - 엔드 로드 나선 상승
     * - 인첸트 반짝임
     * - 천둥소리 + 레벨업 사운드
     * - 타이틀 표시
     */
    public static void playRelicAcquireEffect(Player player, RelicDefinition def, RelicWars plugin) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // === 1. 파티클 폭발 ===
        // 황금빛 먼지 폭발 (넓게)
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 80, 0.5, 1.0, 0.5, 0.5);
        // 엔드 로드 상승
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), 40, 0.3, 0.0, 0.3, 0.1);
        // 인첸트 반짝임
        world.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.5, 0), 60, 1.0, 1.0, 1.0, 1.0);

        // === 2. 사운드 ===
        // 레벨업 사운드 (고음, 전체에게)
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.5f, 0.8f);
        // UI 토스트 사운드
        player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.2f);

        // === 3. 나선형 파티클 상승 애니메이션 (1.5초간) ===
        final org.bukkit.scheduler.BukkitTask[] spiralTask = new org.bukkit.scheduler.BukkitTask[1];
        spiralTask[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= 30 || !player.isOnline()) {
                    if (spiralTask[0] != null) spiralTask[0].cancel();
                    return;
                }
                double angle = tick * 0.5;
                double radius = 1.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = tick * 0.1;
                Location spiralLoc = loc.clone().add(x, y, z);
                world.spawnParticle(Particle.END_ROD, spiralLoc, 2, 0, 0, 0, 0);
                tick++;
            }
        }, 0L, 1L);

        // === 4. 타이틀 (획득자에게만) ===
        String tierColor = def != null ? def.getTierColor() : "§e";
        String name = def != null ? def.getName() : "유물";
        player.sendTitle(tierColor + "§l" + name, "§7유물을 획득했습니다!", 5, 40, 15);
    }

    // ======================== 기믹 달성 (최초 발견) ========================

    /**
     * 기믹 달성 시 서버 전체에 임팩트
     * - 천둥 번쩍 효과
     * - 엔더 드래곤 사운드
     * - 거대한 파티클
     */
    public static void playGimmickCompleteEffect(Player achiever, RelicDefinition def, RelicWars plugin) {
        Location loc = achiever.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // === 1. 거대 파티클 ===
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 2, 0), 200, 2.0, 2.0, 2.0, 0.8);
        world.spawnParticle(Particle.FLASH, loc.clone().add(0, 1, 0), 3, 0, 0, 0, 0);
        world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0, 1, 0), 50, 1.5, 1.5, 1.5, 0.05);

        // === 2. 사운드 (전체 서버에 들리도록 높은 볼륨) ===
        world.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 3.0f, 1.0f);
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.MASTER, 0.8f, 1.5f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 2.0f, 0.7f);

        // === 3. 지연된 추가 이펙트 (0.5초 후 빛의 기둥) ===
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!achiever.isOnline()) return;
            for (int i = 0; i < 30; i++) {
                Location beamLoc = loc.clone().add(0, i * 0.5, 0);
                world.spawnParticle(Particle.END_ROD, beamLoc, 5, 0.1, 0, 0.1, 0.02);
            }
            world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 2.0f, 1.2f);
        }, 10L);

        // === 4. 타이틀 ===
        String tierColor = def != null ? def.getTierColor() : "§e";
        String name = def != null ? def.getName() : "유물";
        achiever.sendTitle("§6§l✦ 기믹 달성! ✦", tierColor + name + " §7발견!", 5, 60, 20);
    }

    // ======================== 구조 완료 ========================

    /**
     * 구조 완료 시 치유 이펙트
     * - 하트 파티클
     * - 부활 사운드
     */
    public static void playReviveEffect(Player rescuer, Player revived, RelicWars plugin) {
        Location loc = revived.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // === 하트 + 치유 파티클 ===
        world.spawnParticle(Particle.HEART, loc.clone().add(0, 2, 0), 15, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 1, 0), 30, 0.8, 1.0, 0.8, 0.5);
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), 20, 0.3, 0.5, 0.3, 0.05);

        // === 사운드 ===
        world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.5f);
        world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.8f);

        // === 타이틀 ===
        revived.sendTitle("§a§l구조 완료!", "§7전투에 복귀합니다", 5, 30, 10);
        rescuer.sendTitle("§a§l구조 성공!", "§7" + revived.getName() + " 구조 완료", 5, 30, 10);
    }

    // ======================== 다운 ========================

    /**
     * 다운 시 비극적 이펙트
     * - 피 파티클 + 연기
     * - 위더 사운드
     */
    public static void playDownEffect(Player downed) {
        Location loc = downed.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // === 파티클 ===
        world.spawnParticle(Particle.DAMAGE_INDICATOR, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.3);
        world.spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 0.5, 0), 20, 0.3, 0.2, 0.3, 0.05);
        world.spawnParticle(Particle.SOUL, loc.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);

        // === 사운드 ===
        world.playSound(loc, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.8f, 0.5f);
        world.playSound(loc, Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 0.7f);
        // 심장박동
        downed.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    // ======================== 확킬 (처형) ========================

    /**
     * 확킬 시 강렬한 사망 이펙트
     * - 적색 파티클 폭발
     * - 소울 상승
     * - 위더 사운드
     */
    public static void playExecuteEffect(Player victim, Player attacker) {
        Location loc = victim.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // === 파티클 ===
        world.spawnParticle(Particle.DAMAGE_INDICATOR, loc.clone().add(0, 1, 0), 50, 1.0, 1.0, 1.0, 0.5);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.5, 0), 30, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.SOUL, loc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);

        // === 사운드 ===
        world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.5f, 1.5f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.0f, 0.8f);
        
        if (attacker != null) {
            attacker.sendTitle("§c§l처형!", "§7" + victim.getName() + " 처형 완료", 5, 30, 10);
        }
    }

    // ======================== 봉인 해제 ========================

    /**
     * 봉인 유물이 해제될 때 주변 이펙트
     * - 보라색 파티클
     * - 종소리
     */
    public static void playUnsealEffect(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 0.5, 0), 40, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 1.0);
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.5f, 1.5f);
        world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.BLOCKS, 1.5f, 0.8f);
    }

    // ======================== 줍기 진행 중 틱 이펙트 ========================

    /**
     * 유물 줍기 중 매 틱 이펙트 (점점 강해짐)
     */
    public static void playPickupTickEffect(Player player, Location relicLoc, float progress) {
        World world = relicLoc.getWorld();
        if (world == null) return;

        // progress: 0.0 ~ 1.0
        int particleCount = (int) (2 + progress * 8);
        world.spawnParticle(Particle.END_ROD, relicLoc.clone().add(0, 0.5, 0), particleCount, 0.2, 0.2, 0.2, 0.02);

        // 50% 이상이면 추가 이펙트
        if (progress > 0.5f) {
            world.spawnParticle(Particle.ENCHANT, relicLoc.clone().add(0, 0.8, 0), 3, 0.3, 0.3, 0.3, 0.5);
        }

        // 80% 이상이면 소리 빨라짐
        if (progress > 0.8f) {
            player.playSound(relicLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.3f, 0.8f + progress);
        }
    }

    // ======================== 자동 확킬 (시간 초과 사망) ========================

    /**
     * 자동 확킬 시 쓸쓸한 사망 이펙트
     */
    public static void playAutoExecuteEffect(Player victim) {
        Location loc = victim.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.SOUL, loc.clone().add(0, 1, 0), 30, 0.5, 1.0, 0.5, 0.05);
        world.spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.03);
        world.playSound(loc, Sound.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 0.3f, 1.5f);
    }
}
