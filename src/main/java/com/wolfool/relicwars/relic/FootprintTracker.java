package com.wolfool.relicwars.relic;

import com.wolfool.relicwars.RelicWars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FootprintTracker {

    private final RelicWars plugin;
    // 플레이어 UUID -> 시간순 발자국 큐
    private final Map<UUID, Queue<FootprintData>> footprints = new HashMap<>();

    public FootprintTracker(RelicWars plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // 3초(60틱)마다 모든 온라인 플레이어의 유물 소지 여부 검사 후 위치 기록
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    // #008 그림자 막 상태라면 발자국 기록 생략
                    if (plugin.getRelicAbilityHandler().active008Shadow.contains(p.getUniqueId())) continue;

                    int relicCount = plugin.getRelicManager().countPlayerRelics(p);
                    if (relicCount > 0) {
                        // 가장 번호가 낮은(가장 강한) 유물 번호 찾기
                        int bestTierNum = 999;
                        for (ItemStack item : p.getInventory().getContents()) {
                            if (item != null && com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)) {
                                int num = com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(item);
                                if (num < bestTierNum) {
                                    bestTierNum = num;
                                }
                            }
                        }

                        if (bestTierNum != 999) {
                            footprints.computeIfAbsent(p.getUniqueId(), k -> new LinkedList<>())
                                    .add(new FootprintData(p.getLocation().clone(), bestTierNum, now));
                        }
                    }
                }

                // 오래된(3분 초과) 데이터 제거
                long threeMinsAgo = now - (180 * 1000);
                for (Queue<FootprintData> queue : footprints.values()) {
                    while (!queue.isEmpty() && queue.peek().getTimestamp() < threeMinsAgo) {
                        queue.poll();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 60L); // 3초마다
    }

    public void shutdown() {
        footprints.clear();
    }

    public Map<UUID, Queue<FootprintData>> getFootprints() {
        return footprints;
    }

    public static class FootprintData {
        private final Location loc;
        private final int bestRelicNum;
        private final long timestamp;

        public FootprintData(Location loc, int bestRelicNum, long timestamp) {
            this.loc = loc;
            this.bestRelicNum = bestRelicNum;
            this.timestamp = timestamp;
        }

        public Location getLoc() { return loc; }
        public int getBestRelicNum() { return bestRelicNum; }
        public long getTimestamp() { return timestamp; }
    }
}
