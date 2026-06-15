package com.wolfool.relicwars.event;

import com.wolfool.relicwars.RelicWars;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class EnvironmentListener implements Listener {

    private final RelicWars plugin;

    public EnvironmentListener(RelicWars plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 겉날개(Elytra) 원천 차단
    // ==========================================

    @EventHandler
    public void onGlide(EntityToggleGlideEvent event) {
        if (!plugin.getConfigManager().isDisableElytra()) return;
        if (event.isGliding() && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfigManager().isDisableElytra()) return;
        ItemStack item = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        if ((item != null && item.getType() == Material.ELYTRA) || 
            (cursor != null && cursor.getType() == Material.ELYTRA)) {
            
            // 갑옷 슬롯에 장착하려는 경우 차단
            if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player p) {
                    p.sendMessage("§c[RelicWars] 겉날개 착용 및 사용이 금지되어 있습니다.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isDisableElytra()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item != null && item.getType() == Material.ELYTRA) {
            // 우클릭으로 장착하는 것 차단
            if (event.getAction().name().contains("RIGHT_CLICK")) {
                event.setCancelled(true);
                player.sendMessage("§c[RelicWars] 겉날개 착용 및 사용이 금지되어 있습니다.");
            }
        }
    }

    // ==========================================
    // 엔드 시티(End City) 진입 차단
    // ==========================================

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.getConfigManager().isDisableEndCities()) return;
        
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c[RelicWars] 엔드 시티 외곽 섬으로의 이동이 금지되어 있습니다.");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isDisableEndCities()) return;
        
        Player player = event.getPlayer();
        Location to = event.getTo();
        
        if (to == null || to.getWorld() == null) return;
        
        if (to.getWorld().getEnvironment() == Environment.THE_END) {
            // 중심(0,0)에서 1000 이상 멀어지면 데미지를 주고 튕겨냄
            double distanceSq = (to.getX() * to.getX()) + (to.getZ() * to.getZ());
            if (distanceSq > 1000 * 1000) { // 반경 1000
                player.damage(1.0); // 경고성 데미지
                
                // 중심 방향으로 밀어내기 (넉백)
                org.bukkit.util.Vector pushBack = new org.bukkit.util.Vector(-to.getX(), 0, -to.getZ()).normalize().multiply(0.5);
                pushBack.setY(0.2);
                player.setVelocity(pushBack);
                
                player.sendMessage("§c[RelicWars] 더 이상 나아갈 수 없습니다! (외곽 섬 진입 금지)");
            }
        }
    }
}
