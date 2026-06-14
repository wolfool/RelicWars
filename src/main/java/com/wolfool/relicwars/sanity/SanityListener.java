package com.wolfool.relicwars.sanity;

import com.wolfool.relicwars.RelicWars;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class SanityListener implements Listener {

    private final RelicWars plugin;

    public SanityListener(RelicWars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        
        // 다운 상태면 먹을 수 없음 (CombatListener에서도 막고 있지만 이중 방어)
        if (plugin.getCombatManager().isDowned(player)) return;

        Material type = event.getItem().getType();
        
        if (type == Material.GOLDEN_APPLE) {
            plugin.getSanityManager().restoreSanity(player, 20);
        } else if (type == Material.ENCHANTED_GOLDEN_APPLE) {
            plugin.getSanityManager().restoreSanity(player, 50);
        }
    }
}
