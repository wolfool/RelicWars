package com.wolfool.relicwars.boss;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * 커스텀 보스들의 스폰을 총괄하는 매니저.
 * 탐욕의 추적자, 붕괴의 핵 등의 스폰 로직을 관리합니다.
 */
public class BossManager implements Manager, Listener {

    private final RelicWars plugin;
    private BukkitTask greedAssassinTask;

    public BossManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startGreedAssassinSpawner();
        plugin.getLogger().info("§a[RelicWars] BossManager 초기화 완료.");
    }

    @Override
    public void shutdown() {
        if (greedAssassinTask != null) greedAssassinTask.cancel();
        plugin.getLogger().info("§a[RelicWars] BossManager 종료.");
    }

    /**
     * 주기적으로(예: 10분마다) 탐욕스러운 유저(유물 3개 이상)를 찾아 탐욕의 추적자를 기습 소환합니다.
     */
    private void startGreedAssassinSpawner() {
        greedAssassinTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 1. 유물을 3개 이상 보유한 유저들 필터링
                Player target = null;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plugin.getRelicManager().countPlayerRelics(p) >= 3) {
                        target = p;
                        break; // 한 명 찾으면 그 대상에게 스폰 (랜덤 셔플로 개선 가능)
                    }
                }

                if (target != null) {
                    // 확률적 스폰 (예: 30% 확률)
                    if (Math.random() <= 0.3) {
                        spawnGreedAssassin(target);
                    }
                }
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // 5분마다 (6000틱) 체크
    }

    private void spawnGreedAssassin(Player target) {
        Location spawnLoc = target.getLocation().add(Math.random() * 10 - 5, 0, Math.random() * 10 - 5);
        
        target.sendMessage("§0=========================================");
        target.sendMessage("§4[위험] 등 뒤에서 차가운 살기가 느껴집니다...");
        target.sendMessage("§0=========================================");

        // 보스 인스턴스 생성 및 스폰
        GreedAssassin assassin = new GreedAssassin(plugin, target);
        assassin.spawn(spawnLoc);
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (event.getEntity().getCustomName() != null && event.getEntity().getCustomName().contains("탐욕의 추적자")) {
            event.getDrops().clear();
            
            int sealSeconds = plugin.getConfigManager().getBossDropSealSeconds();
            
            // #013 탐욕의 뼈 봉인 드랍
            org.bukkit.inventory.ItemStack relic13 = com.wolfool.relicwars.relic.RelicItemUtil.createRelicItem(com.wolfool.relicwars.relic.RelicDefinition.getByNumber(13));
            plugin.getSealedRelicManager().spawnSealedRelic(
                event.getEntity().getLocation(),
                relic13, 
                sealSeconds
            );
            
            Bukkit.broadcast(Component.text("§a[보스] 누군가 탐욕의 추적자를 물리치고 유물의 봉인을 해제하려 합니다!"));
        }
    }
}
