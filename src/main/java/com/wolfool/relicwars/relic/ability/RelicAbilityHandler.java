package com.wolfool.relicwars.relic.ability;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.relic.RelicDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RelicAbilityHandler {

    private final RelicWars plugin;
    
    // 버프 상태 관리 맵 (UUID)
    public final Set<UUID> active029FallImmunity = new HashSet<>();
    public final Set<UUID> active027FireImmunity = new HashSet<>();
    public final Set<UUID> active025FastRevive = new HashSet<>();

    public RelicAbilityHandler(RelicWars plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, RelicDefinition def) {
        int num = def.getNumber();
        switch (num) {
            case 30 -> execute030(player);
            case 29 -> execute029(player);
            case 28 -> execute028(player);
            case 27 -> execute027(player);
            case 26 -> execute026(player);
            case 25 -> execute025(player);
            default -> player.sendMessage("§c[RelicWars] 유물 #" + num + " 스킬은 아직 구현되지 않았습니다.");
        }
    }

    // #030 낙뢰의 심지
    private void execute030(Player player) {
        Block targetBlock = player.getTargetBlockExact(50);
        if (targetBlock == null) {
            player.sendMessage("§c[RelicWars] 타겟 블록이 너무 멀거나 없습니다!");
            return;
        }
        
        Location strikeLoc = targetBlock.getLocation();
        player.sendMessage("§e[RelicWars] 1.5초 뒤 해당 위치에 낙뢰가 떨어집니다!");
        
        // 파티클 등으로 전조증상 (MVP에서는 생략)
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            strikeLoc.getWorld().strikeLightning(strikeLoc);
        }, 30L); // 1.5초 (30틱)
    }

    // #029 추락왕의 깃털
    private void execute029(Player player) {
        player.sendMessage("§e[RelicWars] 15초간 낙하 데미지 면역 및 전방 도약!");
        
        // 이단 점프 (대쉬) 발동
        Vector dir = player.getLocation().getDirection().normalize().multiply(1.5).setY(0.8);
        player.setVelocity(dir);

        UUID id = player.getUniqueId();
        active029FallImmunity.add(id);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active029FallImmunity.remove(id);
            if (player.isOnline()) player.sendMessage("§c[RelicWars] 낙하 데미지 면역이 종료되었습니다.");
        }, 300L); // 15초
    }

    // #028 심해의 폐
    private void execute028(Player player) {
        player.sendMessage("§b[RelicWars] 3분간 수중 호흡 버프 및 발밑 물 웅덩이 생성!");
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 3600, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 3600, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 3600, 0, false, false));

        // 지상 물 웅덩이 생성
        Block feet = player.getLocation().getBlock();
        if (feet.getType() == Material.AIR || feet.getType() == Material.SHORT_GRASS) {
            feet.setType(Material.WATER);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (feet.getType() == Material.WATER) feet.setType(Material.AIR);
            }, 100L); // 5초 뒤 소멸
            
            // 대쉬 보너스
            player.setVelocity(player.getLocation().getDirection().multiply(1.2));
        }
    }

    // #027 용암의 눈
    private void execute027(Player player) {
        player.sendMessage("§c[RelicWars] 15초간 화염 면역 및 용암 보행 발동!");
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 300, 0, false, false));
        
        UUID id = player.getUniqueId();
        active027FireImmunity.add(id);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !active027FireImmunity.contains(id)) {
                    this.cancel();
                    return;
                }
                
                // 발밑 용암을 마그마블록으로 임시 변경
                Block below = player.getLocation().getBlock().getRelative(0, -1, 0);
                if (below.getType() == Material.LAVA) {
                    below.setType(Material.MAGMA_BLOCK);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (below.getType() == Material.MAGMA_BLOCK) below.setType(Material.LAVA);
                    }, 60L); // 3초 뒤 원래 용암으로
                }
                
                // 지나간 자리에 불길
                Block feet = player.getLocation().getBlock();
                if (feet.getType() == Material.AIR) {
                    feet.setType(Material.FIRE);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (feet.getType() == Material.FIRE) feet.setType(Material.AIR);
                    }, 40L); // 2초 뒤 소멸
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active027FireImmunity.remove(id);
            if (player.isOnline()) player.sendMessage("§c[RelicWars] 화염 면역이 종료되었습니다.");
        }, 300L);
    }

    // #026 어둠매듭
    private void execute026(Player player) {
        player.sendMessage("§8[RelicWars] 10초간 어둠 속에 숨어듭니다...");
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0, false, false));
        // 스코어보드 닉네임 감추기 등은 MVP에서 투명화 포션으로 대체
    }

    // #025 최후의 봉합
    private void execute025(Player player) {
        player.sendMessage("§5[RelicWars] 30초간 안개(시야 차단) 전개 및 구조 시간 단축!");
        
        // 주변 적에게 어둠(Darkness) 부여
        for (Player p : player.getWorld().getPlayers()) {
            if (p.getLocation().distance(player.getLocation()) <= 30.0) {
                if (!plugin.getTeamManager().isSameTeam(player, p)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 600, 0, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 600, 0, false, false));
                }
            }
        }

        UUID id = player.getUniqueId();
        active025FastRevive.add(id);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active025FastRevive.remove(id);
            if (player.isOnline()) player.sendMessage("§c[RelicWars] 구조 단축 효과가 종료되었습니다.");
        }, 600L); // 30초
    }
}
