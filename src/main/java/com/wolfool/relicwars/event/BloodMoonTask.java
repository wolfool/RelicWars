package com.wolfool.relicwars.event;

import com.wolfool.relicwars.RelicWars;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * 매 밤마다 확률적으로 블러드문을 발생시키고, 타겟 유저 좌표를 방송합니다.
 */
public class BloodMoonTask extends BukkitRunnable {

    private final RelicWars plugin;
    private boolean isBloodMoonActive = false;
    private int broadcastTicks = 0;
    private boolean nightAlreadyTriggered = false;

    public BloodMoonTask(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        World overworld = Bukkit.getWorlds().get(0);
        long time = overworld.getTime();

        // 13000 ~ 23000 이 밤
        boolean isNight = time >= 13000 && time <= 23000;

        if (isNight && !nightAlreadyTriggered) {
            nightAlreadyTriggered = true;
            
            double chance = plugin.getConfigManager().getBloodMoonChance();
            if (Math.random() <= chance) {
                startBloodMoon();
            }
        } else if (!isNight && nightAlreadyTriggered) {
            nightAlreadyTriggered = false;
            if (isBloodMoonActive) {
                endBloodMoon();
            }
        }

        if (isBloodMoonActive) {
            broadcastTicks++;
            int intervalMins = plugin.getConfigManager().getBloodMoonBroadcastInterval();
            if (broadcastTicks >= intervalMins) {
                broadcastTicks = 0;
                broadcastTargetLocation();
            }
        }
    }

    private void startBloodMoon() {
        isBloodMoonActive = true;
        broadcastTicks = 0;
        
        Bukkit.broadcast(Component.text("§4[경고] 블러드문이 떠올랐습니다. 탐욕스러운 자들의 위치가 드러납니다..."));
        
        // 하늘색이나 몹 스폰 로직을 변경하려면 여기서 처리
        // Phase 4에서는 위치 방송에만 집중
        broadcastTargetLocation();
    }

    private void endBloodMoon() {
        isBloodMoonActive = false;
        Bukkit.broadcast(Component.text("§c[RelicWars] 블러드문이 지고 다시 평화로운 아침이 찾아왔습니다."));
    }

    private void broadcastTargetLocation() {
        // 유물을 가장 많이 소지한 플레이어 찾기
        Player target = null;
        int maxRelics = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            int count = plugin.getRelicManager().countPlayerRelics(p);
            if (count > maxRelics) {
                maxRelics = count;
                target = p;
            }
        }

        if (target != null && maxRelics > 0) {
            int blur = Math.max(1, plugin.getConfigManager().getBloodMoonCoordinateBlur());
            int blurredX = (int) (Math.round(target.getLocation().getX() / blur) * blur);
            int blurredZ = (int) (Math.round(target.getLocation().getZ() / blur) * blur);
            
            String env = "알 수 없음";
            if (plugin.getConfigManager().isBloodMoonBroadcastDimension()) {
                env = switch (target.getWorld().getEnvironment()) {
                    case NORMAL -> "오버월드";
                    case NETHER -> "네더";
                    case THE_END -> "엔드";
                    default -> "알 수 없는 차원";
                };
            }

            Bukkit.broadcast(Component.text("§4[블러드문] §c다중 유물 소유자가 " + env + " §cX: " + blurredX + " Z: " + blurredZ + " 부근에 있습니다."));
        } else {
            Bukkit.broadcast(Component.text("§4[블러드문] §7현재 유물을 소지한 타겟이 없습니다."));
        }
    }
}
