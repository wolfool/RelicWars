package com.wolfool.relicwars.relic.ability;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.relic.RelicDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class RelicAbilityHandler implements Listener {

    private final RelicWars plugin;
    
    // ë²„í”„ ?íƒœ ê´€ë¦?ë§?(UUID)
    public final Set<UUID> active029FallImmunity = new HashSet<>();
    public final Set<UUID> active027FireImmunity = new HashSet<>();
    public final Set<UUID> active025FastRevive = new HashSet<>();
    public final Set<UUID> active023Marked = new HashSet<>(); // ?œì‹??ì°íŒ ?€??    public final Map<UUID, UUID> active021Duel = new HashMap<>(); // ê²°íˆ¬ ì¤‘ì¸ ??(?€??-> ?œì „??
    public final Set<UUID> active020ScanMode = new HashSet<>(); // /relic scan ?…ë ¥ ?€ê¸??íƒœ
    public final Map<String, Location> active017Anchor = new HashMap<>(); // ?€ID(?ëŠ” UUID) -> ?œê³¡???„ì¹˜
    public final Set<UUID> active015Casting = new HashSet<>(); // #015 ìºìŠ¤??ì¤‘ì¸ ?Œë ˆ?´ì–´
    public final Set<UUID> active010EMP = new HashSet<>(); // #010 EMP???¹í•œ ?Œë ˆ?´ì–´
    
    // Batch 4 ì¶”ê?
    public final Set<UUID> active008Shadow = new HashSet<>(); // #008 ê·¸ë¦¼??ë§?(?ì? ë©´ì—­)
    public final Map<Location, String> active007Dome = new HashMap<>(); // #007 ???„ì¹˜ -> ?€ID/UUID
    public final Map<UUID, LeapData> active006Leap = new HashMap<>(); // #006 ì°¨ì› ?„ì•½???°ì´??    // Batch 5 ì¶”ê?
    public final Set<UUID> active003TrackerWait = new HashSet<>(); // #003 ì¶”ì  ? ë¬¼ ë²ˆí˜¸ ?…ë ¥ ?€ê¸??íƒœ
    // #006???°ì´???´ë˜??    public static class LeapData {
        public Location origin;
        public double health;
        public org.bukkit.entity.ArmorStand hologram;
        public LeapData(Location o, double h, org.bukkit.entity.ArmorStand s) {
            origin = o; health = h; hologram = s;
        }
    }

    public RelicAbilityHandler(RelicWars plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, RelicDefinition def) {
        int num = def.getNumber();
        
        // EMP ?íƒœ ì²´í¬
        if (active010EMP.contains(player.getUniqueId())) {
            player.sendMessage("Â§c[EMP] ê¸°ëŠ¥??ë§ˆë¹„?˜ì–´ ? ë¬¼???¬ìš©?????†ìŠµ?ˆë‹¤!");
            return;
        }

        // #006 ?¬ì‚¬??ë³µê?) ì²´í¬ (ì¿¨í????•ì‹ ???Œëª¨ ë¬´ì‹œ)
        if (num == 6 && active006Leap.containsKey(player.getUniqueId())) {
            execute006Return(player);
            return;
        }

        // ?•ì‹ ???Œëª¨ ì²´í¬ (3?¨ê³„: 10, 4?¨ê³„: 20, 5?¨ê³„: 30)
        int sanityCost = getSanityCost(num);
        if (sanityCost > 0) {
            if (!plugin.getSanityManager().consumeSanity(player, sanityCost)) {
                return; // ?•ì‹ ??ë¶€ì¡?            }
        }

        switch (num) {
            case 30 -> execute030(player);
            case 29 -> execute029(player);
            case 28 -> execute028(player);
            case 27 -> execute027(player);
            case 26 -> execute026(player);
            case 25 -> execute025(player);
            case 24 -> execute024(player);
            case 23 -> execute023(player);
            case 22 -> execute022(player);
            case 21 -> execute021(player);
            case 20 -> execute020(player);
            case 19 -> execute019(player);
            case 18 -> execute018(player);
            case 17 -> execute017(player);
            case 16 -> execute016(player);
            case 15 -> execute015(player);
            case 14 -> execute014(player);
            case 13 -> execute013(player);
            case 12 -> execute012(player);
            case 11 -> execute011(player);
            case 10 -> execute010(player);
            case 9 -> execute009(player);
            case 8 -> execute008(player);
            case 7 -> execute007(player);
            case 6 -> execute006(player);
            case 5 -> execute005(player);
            case 4 -> execute004(player);
            case 3 -> execute003(player);
            case 2 -> execute002(player);
            case 1 -> execute001(player);
            default -> player.sendMessage("Â§c[RelicWars] ? ë¬¼ #" + num + " ?¤í‚¬?€ ?„ì§ êµ¬í˜„?˜ì? ?Šì•˜?µë‹ˆ??");
        }
    }

    // #030 ?™ë¢°???¬ì?
    private void execute030(Player player) {
        Block targetBlock = player.getTargetBlockExact(50);
        if (targetBlock == null) {
            player.sendMessage("Â§c[RelicWars] ?€ê²?ë¸”ë¡???ˆë¬´ ë©€ê±°ë‚˜ ?†ìŠµ?ˆë‹¤!");
            return;
        }
        
        Location strikeLoc = targetBlock.getLocation();
        player.sendMessage("Â§e[RelicWars] 1.5ì´????´ë‹¹ ?„ì¹˜???™ë¢°ê°€ ?¨ì–´ì§‘ë‹ˆ??");
        
        // ?Œí‹°???±ìœ¼ë¡??„ì¡°ì¦ìƒ (MVP?ì„œ???ëµ)
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            strikeLoc.getWorld().strikeLightningEffect(strikeLoc);
            com.wolfool.relicwars.util.RumorUtil.broadcastRumor(strikeLoc, "Â§b[?Œë¬¸] %sìª½ì—??ë²ˆê°œ ?Œë¦¬ê°€ ?¤ë ¸?µë‹ˆ??");
            
            // ë°˜ê²½ 3ë¸”ë¡ ?°ë?ì§€ + ?‰ë°± + ë°œê´‘
            for (org.bukkit.entity.Entity e : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 3, 3, 3)) {
                if (e instanceof Player target && !target.equals(player)) {
                    if (plugin.getTeamManager().isSameTeam(player, target)) continue;
                    
                    target.damage(10.0, player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false));
                    
                    Vector knockback = target.getLocation().toVector().subtract(strikeLoc.toVector());
                    if (knockback.lengthSquared() == 0) knockback = new Vector(0, 1, 0);
                    else knockback = knockback.normalize().multiply(1.5).setY(0.8);
                    
                    target.setVelocity(knockback);
                }
            }
        }, 30L); // 1.5ì´?(30??
    }

    // #029 ì¶”ë½?•ì˜ ê¹ƒí„¸
    private void execute029(Player player) {
        player.sendMessage("Â§e[RelicWars] 15ì´ˆê°„ ?™í•˜ ?°ë?ì§€ ë©´ì—­ ë°??ˆê³µ?ì„œ ?´ë‹¨ ?í”„ ê°€??");
        
        UUID id = player.getUniqueId();
        active029FallImmunity.add(id);
        
        // ?´ë‹¨ ?í”„ë¥??„í•´ ë¹„í–‰ ?ˆìš©
        player.setAllowFlight(true);

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !active029FallImmunity.contains(id)) {
                    this.cancel();
                    return;
                }
                // ë°œë°‘??ê¹ƒí„¸ ?Œí‹°???¸ë ˆ??                player.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, player.getLocation(), 1, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0L, 5L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active029FallImmunity.remove(id);
            if (player.isOnline()) {
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    player.setAllowFlight(false);
                }
                player.sendMessage("Â§c[RelicWars] ì¶”ë½?•ì˜ ê¹ƒí„¸ ?¨ê³¼ê°€ ì¢…ë£Œ?˜ì—ˆ?µë‹ˆ??");
            }
        }, 300L); // 15ì´?    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        
        if (active029FallImmunity.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.setAllowFlight(false); // 1?Œë§Œ ê°€??            
            Vector dir = player.getLocation().getDirection().normalize().multiply(1.5).setY(0.8);
            player.setVelocity(dir);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
        }
    }

    // #028 ?¬í•´????    private void execute028(Player player) {
        player.sendMessage("Â§b[RelicWars] 3ë¶„ê°„ ?˜ì¤‘ ?¸í¡ ë²„í”„ ë°?ë°œë°‘ ë¬??…ë©???ì„±!");
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 3600, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 3600, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 3600, 0, false, false));

        // ì§€??ë¬??…ë©???ì„± (3x3)
        Block feet = player.getLocation().getBlock();
        java.util.List<Block> changedBlocks = new java.util.ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block b = feet.getRelative(x, 0, z);
                if (b.getType() == Material.AIR || b.getType() == Material.SHORT_GRASS || b.getType() == Material.TALL_GRASS) {
                    b.setType(Material.WATER);
                    changedBlocks.add(b);
                }
            }
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Block b : changedBlocks) {
                if (b.getType() == Material.WATER) b.setType(Material.AIR);
            }
        }, 100L); // 5ì´????Œë©¸

        // ?€??ë³´ë„ˆ??        player.setVelocity(player.getLocation().getDirection().multiply(1.2));

        // ?¸ë¥¸ ê³µëª… ?Œí‹°???Œë¬¸ (ì§€??
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 3600) { // 3ë¶?                    this.cancel();
                    return;
                }
                ticks += 5;
                if (player.getLocation().getBlock().getType() == Material.WATER) {
                    player.getWorld().spawnParticle(org.bukkit.Particle.NAUTILUS, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    // #027 ?©ì•”????    private void execute027(Player player) {
        player.sendMessage("Â§c[RelicWars] 15ì´ˆê°„ ?”ì—¼ ë©´ì—­ ë°??©ì•” ë³´í–‰ ë°œë™!");
        
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "Â§b[?Œë¬¸] %sìª½ì—??ë¶ˆê¸¸??ì¹˜ì†Ÿ???´ê¸°ê°€ ?ê»´ì§‘ë‹ˆ??");
        
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
                
                // ë°œë°‘ ?©ì•”??ë§ˆê·¸ë§ˆë¸”ë¡ìœ¼ë¡??„ì‹œ ë³€ê²?                Block below = player.getLocation().getBlock().getRelative(0, -1, 0);
                if (below.getType() == Material.LAVA) {
                    below.setType(Material.MAGMA_BLOCK);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (below.getType() == Material.MAGMA_BLOCK) below.setType(Material.LAVA);
                    }, 60L); // 3ì´????ë˜ ?©ì•”?¼ë¡œ
                }
                
                // ì§€?˜ê°„ ?ë¦¬??ë¶ˆê¸¸
                Block feet = player.getLocation().getBlock();
                if (feet.getType() == Material.AIR) {
                    feet.setType(Material.FIRE);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (feet.getType() == Material.FIRE) feet.setType(Material.AIR);
                    }, 40L); // 2ì´????Œë©¸
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active027FireImmunity.remove(id);
            if (player.isOnline()) player.sendMessage("Â§c[RelicWars] ?”ì—¼ ë©´ì—­??ì¢…ë£Œ?˜ì—ˆ?µë‹ˆ??");
        }, 300L);
    }

    // #026 ?´ë‘ ë§¤ë“­
    private void execute026(Player player) {
        player.sendMessage("Â§8[RelicWars] 10ì´ˆê°„ ?´ë‘  ?ì— ?¨ì–´??‹ˆ??..");
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0, false, false));
        
        org.bukkit.scoreboard.Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = board.getTeam("relic026_" + player.getName());
        if (team == null) {
            team = board.registerNewTeam("relic026_" + player.getName());
        }
        team.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
        team.addEntry(player.getName());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            org.bukkit.scoreboard.Team t = board.getTeam("relic026_" + player.getName());
            if (t != null) {
                t.removeEntry(player.getName());
                t.unregister();
            }
            if (player.isOnline()) {
                player.getWorld().spawnParticle(org.bukkit.Particle.SQUID_INK, player.getLocation().add(0, 1, 0), 100, 0.5, 1.0, 0.5, 0.1);
                player.sendMessage("Â§c[RelicWars] ?´ë‘ ë§¤ë“­ ?€? ì´ ì¢…ë£Œ?˜ì–´ ?„ì¹˜ê°€ ?¸ì¶œ?˜ì—ˆ?µë‹ˆ??");
            }
        }, 200L); // 10ì´?    }



    // #025 ìµœí›„??ë´‰í•©
    private void execute025(Player player) {
        player.sendMessage("Â§5[RelicWars] 30ì´ˆê°„ êµ¬ì¡° ?œê°„??2ì´ˆë¡œ ?€???¨ì¶•?©ë‹ˆ??");
        
        UUID id = player.getUniqueId();
        active025FastRevive.add(id);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active025FastRevive.remove(id);
            if (player.isOnline()) player.sendMessage("Â§c[RelicWars] êµ¬ì¡° ?¨ì¶• ?¨ê³¼ê°€ ì¢…ë£Œ?˜ì—ˆ?µë‹ˆ??");
        }, 600L); // 30ì´?    }

    // ======================== Batch 2: #024 ~ #020 ========================

    // #024 ë¶‰ì? ë´‰í•© ??30ë¸”ë¡ ë°??¤ìš´ ?€?ì„ ?ê¸° ?„ì¹˜ë¡??”ë ˆ?¬íŠ¸
    private void execute024(Player player) {
        // 30ë¸”ë¡ ?´ë‚´ ?¤ìš´???€???ìƒ‰
        Player target = null;
        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            if (!plugin.getTeamManager().isSameTeam(player, p)) continue;
            if (!plugin.getCombatManager().isDowned(p)) continue;
            if (p.getLocation().distance(player.getLocation()) <= 30.0) {
                target = p;
                break;
            }
        }

        if (target == null) {
            player.sendMessage("Â§c[RelicWars] 30ë¸”ë¡ ?´ë‚´???¤ìš´???€?ì´ ?†ìŠµ?ˆë‹¤!");
            return;
        }

        Location startLoc = target.getLocation().clone();
        Location endLoc = player.getLocation().clone();
        target.teleport(endLoc);
        
        // ë¶‰ì? ???Œí‹°???¸ë ˆ???ì„± (ì§ì„  ë³´ê°„)
        double distance = startLoc.distance(endLoc);
        if (distance > 0) {
            org.bukkit.util.Vector dir = endLoc.toVector().subtract(startLoc.toVector()).normalize();
            for (double d = 0; d <= distance; d += 0.5) {
                Location pLoc = startLoc.clone().add(dir.clone().multiply(d)).add(0, 1, 0); // ?ˆë†’??ë³´ì •
                player.getWorld().spawnParticle(org.bukkit.Particle.DUST, pLoc, 1, new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
            }
        }

        player.sendMessage("Â§d[ë¶‰ì? ë´‰í•©] " + target.getName() + "?˜ì„ ???„ì¹˜ë¡??Œí™˜?ˆìŠµ?ˆë‹¤! êµ¬ì¡°ë¥??œì‘?˜ì„¸??");
        target.sendMessage("Â§d[ë¶‰ì? ë´‰í•©] ?€?ì— ?˜í•´ ?ˆì „ ì§€?€ë¡??´ë™?˜ì—ˆ?µë‹ˆ??");
    }

    // #023 ?¬ëƒ¥ê¾¼ì˜ ?œì‹ ??60ì´ˆê°„ ë°”ë¼ë³´ëŠ” ?ì—ê²?ë°œê´‘ + ê°•íƒˆ ?œê°„ ?¨ì¶•
    private void execute023(Player player) {
        // ë°”ë¼ë³´ê³  ?ˆëŠ” ???ìƒ‰ (50ë¸”ë¡ ?´ë‚´)
        Player target = null;
        for (Entity e : player.getNearbyEntities(50, 50, 50)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;

            // ?œì„  ë°©í–¥ ê²€??(?Œë ˆ?´ì–´ê°€ ë°”ë¼ë³´ëŠ” ë°©í–¥ê³??€?ê¹Œì§€??ê°ë„)
            Vector toTarget = p.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            double dot = player.getLocation().getDirection().normalize().dot(toTarget);
            if (dot > 0.95) { // ??18???´ë‚´
                target = p;
                break;
            }
        }

        if (target == null) {
            player.sendMessage("Â§c[RelicWars] ë°”ë¼ë³´ëŠ” ë°©í–¥?????Œë ˆ?´ì–´ê°€ ?†ìŠµ?ˆë‹¤!");
            return;
        }

        Player marked = target;
        marked.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0, false, false)); // 60ì´?        active023Marked.add(marked.getUniqueId());

        player.sendMessage("Â§6[?¬ëƒ¥ê¾¼ì˜ ?œì‹] " + marked.getName() + "?ê²Œ 60ì´ˆê°„ ?¬ëƒ¥ ?œì‹??ì°ì—ˆ?µë‹ˆ??");
        marked.sendMessage("Â§c[ê²½ê³ ] ?„êµ°ê°€ ?¹ì‹ ?ê²Œ ?¬ëƒ¥ ?œì‹??ì°ì—ˆ?µë‹ˆ?? 60ì´ˆê°„ ë²??ˆë¨¸ë¡œë„ ë³´ì…?ˆë‹¤!");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active023Marked.remove(marked.getUniqueId());
        }, 1200L);
    }

    // #022 ?ìš•???™ì „ ??ê°€ì§?ë´‰ì¸ ? ë¬¼ ?¸ë© ?¤ì¹˜
    private void execute022(Player player) {
        Location trapLoc = player.getLocation().clone();
        player.sendMessage("Â§e[?ìš•???™ì „] ê°€ì§?ë´‰ì¸ ? ë¬¼ ?¸ë©???¤ì¹˜?ˆìŠµ?ˆë‹¤!");

        // ê°€ì§?ë´‰ì¸ ? ë¬¼ (?„ì´?? ?Œí™˜
        ItemStack fakeItem = new ItemStack(Material.GOLD_INGOT);
        org.bukkit.entity.Item fake = player.getWorld().dropItem(trapLoc, fakeItem);
        fake.setPickupDelay(32767);
        fake.setUnlimitedLifetime(true);
        fake.setInvulnerable(true);
        fake.setGlowing(true);
        fake.customName(net.kyori.adventure.text.Component.text("Â§c[ë´‰ì¸ ì¤? Â§e?ìš•???™ì „ Â§7(300ì´?"));
        fake.setCustomNameVisible(true);
        
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(trapLoc, "Â§b[?Œë¬¸] %sìª½ì—???ìš•?¤ëŸ¬??ê¸ˆì†?Œì´ ?¤ë ¸?µë‹ˆ??");

        // 3ë¶?3600?? ???ë™ ?Œë©¸
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!fake.isDead()) fake.remove();
        }, 3600L);

        // ê·¼ì ‘(1.5ë¸”ë¡ ?´ë‚´) ê°ì?ë¥??„í•œ ë°˜ë³µ ?œìŠ¤??        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (fake.isDead()) { this.cancel(); return; }

                for (Player p : fake.getWorld().getPlayers()) {
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;
                    if (p.getLocation().distanceSquared(fake.getLocation()) <= 2.25) { // 1.5ë¸”ë¡
                        // ?¸ë© ë°œë™!
                        fake.remove();
                        p.sendMessage("Â§4[?¨ì •!] ê°€ì§?? ë¬¼?´ì—ˆ?µë‹ˆ??");
                        player.sendMessage("Â§a[?ìš•???™ì „] " + p.getName() + "??ê°€) ?¸ë©??ê±¸ë ¸?µë‹ˆ??");

                        // ??°œ ?°ë?ì§€ (ì§€???Œê´´ X)
                        p.getWorld().createExplosion(fake.getLocation(), 0F, false, false);
                        p.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, fake.getLocation(), 1);
                        p.damage(10.0, player);

                        // ?”ë²„??                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, false, false)); // 5ì´?êµ¬ì†2
                        p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 1, false, false)); // 5ì´?ì±„êµ´?¼ë¡œ2
                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false)); // 5ì´?ë°œê´‘
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 5L);
    }

    // #021 ê²°íˆ¬?ì˜ ?Œí¸ ??15x15 ê²°íˆ¬??20ì´ˆê°„ ê°•ì œ 1?€1
    private void execute021(Player player) {
        // 15ë¸”ë¡ ?´ë‚´ ë°”ë¼ë³´ëŠ” ???ìƒ‰
        Player target = null;
        for (Entity e : player.getNearbyEntities(15, 15, 15)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;

            Vector toTarget = p.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            double dot = player.getLocation().getDirection().normalize().dot(toTarget);
            if (dot > 0.9) {
                target = p;
                break;
            }
        }

        if (target == null) {
            player.sendMessage("Â§c[RelicWars] 15ë¸”ë¡ ?´ë‚´?????Œë ˆ?´ì–´ê°€ ?†ìŠµ?ˆë‹¤!");
            return;
        }

        Player enemy = target;
        Location center = player.getLocation().clone().add(enemy.getLocation()).multiply(0.5);

        active021Duel.put(enemy.getUniqueId(), player.getUniqueId());
        active021Duel.put(player.getUniqueId(), enemy.getUniqueId());

        player.sendMessage("Â§4[ê²°íˆ¬?ì˜ ?Œí¸] " + enemy.getName() + "ê³??€) 20ì´ˆê°„ ê°•ì œ ê²°íˆ¬ê°€ ?œì‘?©ë‹ˆ??");
        enemy.sendMessage("Â§4[ê²°íˆ¬?ì˜ ?Œí¸] " + player.getName() + "??ê°€) ?¹ì‹ ??ê²°íˆ¬??ê°€?€?µë‹ˆ?? 20ì´ˆê°„ ?ˆì¶œ ë¶ˆê?!");
        
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(center, "Â§b[?Œë¬¸] %sìª½ì—???´ê¸°ê°€ ?ê»´ì§€??ê²°íˆ¬ê°€ ?œì‘?˜ì—ˆ?µë‹ˆ??");

        // ê²°íˆ¬??ë²??ì„± (ë°°ë¦¬??ë¸”ë¡?¼ë¡œ 7ë¸”ë¡ ë°˜ê²½ ?ë¸Œ)
        Set<Location> barriers = new HashSet<>();
        int radius = 7;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 5; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius || Math.abs(z) == radius || y == 5) {
                        Location bLoc = center.clone().add(x, y, z);
                        Block b = bLoc.getBlock();
                        if (b.getType() == Material.AIR) {
                            b.setType(Material.BARRIER);
                            barriers.add(bLoc);
                        }
                    }
                }
            }
        }

        // 20ì´???ê²°íˆ¬???´ì œ
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active021Duel.remove(enemy.getUniqueId());
            active021Duel.remove(player.getUniqueId());

            for (Location bLoc : barriers) {
                if (bLoc.getBlock().getType() == Material.BARRIER) {
                    bLoc.getBlock().setType(Material.AIR);
                }
            }

            if (player.isOnline()) player.sendMessage("Â§a[ê²°íˆ¬] ê²°íˆ¬ê°€ ì¢…ë£Œ?˜ì—ˆ?µë‹ˆ??");
            if (enemy.isOnline()) enemy.sendMessage("Â§a[ê²°íˆ¬] ê²°íˆ¬ê°€ ì¢…ë£Œ?˜ì—ˆ?µë‹ˆ??");
        }, 400L); // 20ì´?        
        // ?´íƒˆ ë°©ì? ì²´í¬
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 400 || !active021Duel.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
                }
                ticks += 10;
                
                if (player.isOnline() && player.getWorld().equals(center.getWorld()) && player.getLocation().distanceSquared(center) > 100) {
                    player.teleport(center);
                    player.damage(5.0);
                    player.sendMessage("Â§c[ê²°íˆ¬?ì˜ ?Œí¸] ê²°íˆ¬?¥ì„ ë²—ì–´?????†ìŠµ?ˆë‹¤!");
                }
                if (enemy.isOnline() && enemy.getWorld().equals(center.getWorld()) && enemy.getLocation().distanceSquared(center) > 100) {
                    enemy.teleport(center);
                    enemy.damage(5.0);
                    enemy.sendMessage("Â§c[ê²°íˆ¬?ì˜ ?Œí¸] ê²°íˆ¬?¥ì„ ë²—ì–´?????†ìŠµ?ˆë‹¤!");
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // #020 ?Œë¬¸???±ë¶ˆ ??4ê°€ì§€ ?µì…˜ ì¤??˜ë‚˜ë¥?? íƒ?˜ëŠ” GUI ?¤í”ˆ
    private void execute020(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Â§5?Œë¬¸???±ë¶ˆ"));

        inv.setItem(3, createGuiItem(Material.ENDER_EYE, "Â§5[ë´‰ì¸ ? ë¬¼ ?¤ìº”]", "Â§7?„ì¬ ë°”ë‹¥??ë´‰ì¸??, "Â§7? ë¬¼?¤ì˜ ?„ì¹˜ë¥??Œì•…?©ë‹ˆ??"));
        inv.setItem(5, createGuiItem(Material.NAME_TAG, "Â§e[?Œìœ ??ê²€??ëª¨ë“œ]", "Â§7?¹ì • ë²ˆí˜¸??? ë¬¼???„ê?", "Â§7ê°€ì¡ŒëŠ”ì§€ ?Œì•„?????ˆëŠ” ê²€??ëª¨ë“œë¥?ì¼?‹ˆ??"));

        player.openInventory(inv);
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "Â§b[?Œë¬¸] ?„êµ°ê°€ ?Œë¬¸???±ë¶ˆ??ì¼°ìŠµ?ˆë‹¤.");
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        java.util.List<Component> loreList = new java.util.ArrayList<>();
        for (String l : lore) {
            loreList.add(Component.text(l).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(loreList);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public void execute020Option2(Player player) {
        player.sendMessage("Â§d[?Œë¬¸???±ë¶ˆ] Â§f?„ì¬ ë°”ë‹¥??ë´‰ì¸??? ë¬¼???¤ìº”?©ë‹ˆ??..");
        java.util.List<org.bukkit.entity.Item> sealed = plugin.getSealedRelicManager().getActiveSealedRelics();
        if (sealed.isEmpty()) {
            player.sendMessage("Â§c  [ë´‰ì¸] ?„ì¬ ?œë²„ ?´ì— ë´‰ì¸??? ë¬¼???†ìŠµ?ˆë‹¤.");
        } else {
            for (org.bukkit.entity.Item display : sealed) {
                Location loc = display.getLocation();
                int rx = (int) (Math.round(loc.getBlockX() / 10.0) * 10);
                int rz = (int) (Math.round(loc.getBlockZ() / 10.0) * 10);
                long end = display.getPersistentDataContainer().getOrDefault(
                        RelicItemUtil.KEY_COOLDOWN_UNTIL, org.bukkit.persistence.PersistentDataType.LONG, 0L);
                int leftSec = Math.max(0, (int) ((end - System.currentTimeMillis()) / 1000));
                player.sendMessage("Â§e  [ë´‰ì¸] " + display.getName() + " Â§7(?¨ì? ?œê°„: " + leftSec + "ì´? - ?€???„ì¹˜: X: " + rx + " ë¶€ê·? Z: " + rz + " ë¶€ê·?);
            }
        }
    }

    public void execute020Option3(Player player) {
        player.sendMessage("Â§d[?Œë¬¸???±ë¶ˆ] Â§f? ë¬¼ ?Œìœ ??ê²€??ëª¨ë“œë¥?? íƒ?ˆìŠµ?ˆë‹¤.");
        active020ScanMode.add(player.getUniqueId());
        
        Component scanMsg = Component.text("Â§e  [?•ë³´] 5ë¶??´ì— ì±„íŒ…ì°½ì— ê²€?‰í•  ? ë¬¼ ë²ˆí˜¸(?«ì)ë§??…ë ¥?˜ì„¸??");
        player.sendMessage(scanMsg);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active020ScanMode.remove(player.getUniqueId())) {
                if (player.isOnline()) {
                    player.sendMessage("Â§c[?Œë¬¸???±ë¶ˆ] ê²€???€ê¸??œê°„??ë§Œë£Œ?˜ì—ˆ?µë‹ˆ??");
                }
            }
        }, 6000L); // 5ë¶?    }

    // ======================== Batch 3: #019 ~ #015 ========================

    private org.bukkit.entity.Item pending019Relic;

    // #019 ë´‰ì¸??ë°”ëŠ˜ ??ë´‰ì¸ ? ë¬¼??ë´‰ì¸ ?œê°„???ˆë°˜?¼ë¡œ ?¨ì¶•
    private void execute019(Player player) {
        org.bukkit.entity.Item nearest = plugin.getSealedRelicManager().getNearestSealed(player.getLocation(), 50);
        if (nearest == null) {
            player.sendMessage("Â§c[RelicWars] 50ë¸”ë¡ ?´ë‚´??ë´‰ì¸??? ë¬¼???†ìŠµ?ˆë‹¤!");
            return;
        }
        
        pending019Relic = nearest;
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Â§3ë´‰ì¸??ë°”ëŠ˜"));
        inv.setItem(3, createGuiItem(Material.SUGAR, "Â§a[?œê°„ ?¨ì¶•]", "Â§7?´ë‹¹ ? ë¬¼???¨ì? ë´‰ì¸ ?œê°„??, "Â§7Â§l?ˆë°˜Â§7?¼ë¡œ ?¨ì¶•?©ë‹ˆ??"));
        inv.setItem(5, createGuiItem(Material.CLOCK, "Â§c[?œê°„ ?°ì¥]", "Â§7?´ë‹¹ ? ë¬¼???¨ì? ë´‰ì¸ ?œê°„??, "Â§7Â§l2ë°°Â?ë¡??°ì¥?©ë‹ˆ??"));
        
        player.openInventory(inv);
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "Â§b[?Œë¬¸] %sìª½ì—???œê°„???ë¦„??ë¹„í?ë¦¬ëŠ” ??•œ ?Œë™??ë°œìƒ?ˆìŠµ?ˆë‹¤.");
    }

    public void execute019Option1(Player player) {
        if (pending019Relic == null || !pending019Relic.isValid()) {
            player.sendMessage("Â§c[ë´‰ì¸??ë°”ëŠ˜] ?€??? ë¬¼???¬ë¼ì¡ŒìŠµ?ˆë‹¤.");
            return;
        }
        plugin.getSealedRelicManager().reduceSealTime(pending019Relic, 0.5);
        player.sendMessage("Â§d[ë´‰ì¸??ë°”ëŠ˜] " + pending019Relic.getName() + "??ë´‰ì¸ ?œê°„??Â§l?ˆë°˜Â§d?¼ë¡œ ?¨ì¶•?ˆìŠµ?ˆë‹¤!");
        pending019Relic = null;
    }

    public void execute019Option2(Player player) {
        if (pending019Relic == null || !pending019Relic.isValid()) {
            player.sendMessage("Â§c[ë´‰ì¸??ë°”ëŠ˜] ?€??? ë¬¼???¬ë¼ì¡ŒìŠµ?ˆë‹¤.");
            return;
        }
        // SealedRelicManager???œê°„???˜ë¦¬??ë©”ì†Œ?œê? ?†ìœ¼ë¯€ë¡? ?¤ì‹œ reduceSealTime(relic, 2.0) ?˜ê±°??
        // ì§ì ‘ PDCë¥??˜ì •?´ì•¼ ?˜ì?ë§? reduceSealTime(relic, factor) ê°€ ê³±í•˜ê¸??°ì‚°?´ë¼ë©?ê°€?¥í•©?ˆë‹¤.
        // ?•ì¸ ?„ìš”. ?°ì„  reduceSealTime(relic, 2.0)ë¡??°ì¥
        plugin.getSealedRelicManager().reduceSealTime(pending019Relic, 2.0);
        player.sendMessage("Â§d[ë´‰ì¸??ë°”ëŠ˜] " + pending019Relic.getName() + "??ë´‰ì¸ ?œê°„??Â§l2ë°°Â§dë¡??°ì¥?ˆìŠµ?ˆë‹¤!");
        pending019Relic = null;
    }

    // #018 ?”ì  ?Œì¦ˆ ??200ë¸”ë¡ ??? ë¬¼ ë³´ìœ ?ì˜ ë°œìêµ??Œí‹°??    private void execute018(Player player) {
        player.sendMessage("Â§e[?”ì  ?Œì¦ˆ] ë°˜ê²½ 200ë¸”ë¡ ??ìµœê·¼ 3ë¶„ê°„??? ë¬¼ ë³´ìœ ???”ì ??ì¶”ì ?©ë‹ˆ??");
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "Â§b[?Œë¬¸] ?„êµ°ê°€ ?”ì ???½ê¸° ?œì‘?ˆìŠµ?ˆë‹¤.");

        // ?ì‹ ???„ì¹˜????¶”???”ì  (?ì£¼???Œí‹°??5ì´ˆê°„)
        new BukkitRunnable() {
            int count = 0;
            Location origin = player.getLocation().clone();
            @Override
            public void run() {
                if (count++ > 25) { this.cancel(); return; }
                player.getWorld().spawnParticle(org.bukkit.Particle.SPELL_WITCH, origin.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0);
            }
        }.runTaskTimer(plugin, 0L, 4L);

        Map<UUID, Queue<FootprintTracker.FootprintData>> footprints = plugin.getFootprintTracker().getFootprints();
        for (Queue<FootprintTracker.FootprintData> queue : footprints.values()) {
            for (FootprintTracker.FootprintData data : queue) {
                if (data.getLoc().getWorld().equals(player.getWorld())) {
                    if (data.getLoc().distanceSquared(player.getLocation()) <= 40000) { // 200 blocks
                        // ?±ê¸‰ë³??‰ìƒ ì§€??                        org.bukkit.Color color;
                        int num = data.getBestRelicNum();
                        if (num <= 5) color = org.bukkit.Color.YELLOW; // 5?¨ê³„ (ê¸ˆìƒ‰)
                        else if (num <= 10) color = org.bukkit.Color.PURPLE; // 4?¨ê³„ (ë³´ë¼??
                        else if (num <= 18) color = org.bukkit.Color.AQUA; // 3?¨ê³„ (?˜ëŠ˜??
                        else if (num <= 24) color = org.bukkit.Color.LIME; // 2?¨ê³„ (ì´ˆë¡??
                        else color = org.bukkit.Color.WHITE; // 1?¨ê³„ (?°ìƒ‰)

                        // ë¨¼ì? ?Œí‹°???ì„±
                        org.bukkit.Particle.DustOptions dustOptions = new org.bukkit.Particle.DustOptions(color, 1.5f);
                        player.spawnParticle(org.bukkit.Particle.DUST, data.getLoc().clone().add(0, 0.1, 0), 2, 0.2, 0, 0.2, dustOptions);
                    }
                }
            }
        }
    }

    // #017 ?œê³¡???????¤ìš´ ?????„ì¹˜ë¡??”ë ˆ?¬íŠ¸ ?¸ì´ë¸?    private void execute017(Player player) {
        Location anchorLoc = player.getLocation().clone();
        player.sendMessage("Â§5[?œê³¡???? ?„ì¬ ?„ì¹˜??ê³µê°„ ?œê³¡?¥ì„ ?¤ì¹˜?ˆìŠµ?ˆë‹¤! (60ì´ˆê°„ ?œì„±)");
        player.sendMessage("Â§7  ??ë²”ìœ„(50ë¸”ë¡) ?ˆì—???¤ìš´?˜ë©´ ???„ì¹˜ë¡??œê°„?´ë™?©ë‹ˆ??");

        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "Â§b[?Œë¬¸] %sìª½ì—??ê³µê°„???¼ê·¸?¬ì????Œë¦¬ê°€ ?¤ë ¸?µë‹ˆ??");

        String teamId = plugin.getTeamManager().getTeamId(player);
        String key = teamId != null ? teamId : player.getUniqueId().toString();
        active017Anchor.put(key, anchorLoc);

        // ???„ì¹˜??ë³´ë¼???Œí‹°??        BukkitTask particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active017Anchor.containsKey(key)) { this.cancel(); return; }
                anchorLoc.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, anchorLoc.clone().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }.runTaskTimer(plugin, 0L, 10L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active017Anchor.remove(key) != null) {
                if (player.isOnline()) player.sendMessage("Â§c[?œê³¡???? ?œê³¡?¥ì´ ?Œë©¸?˜ì—ˆ?µë‹ˆ??");
            }
        }, 1200L); // 60ì´?    }

    // #016 ê°ì‹œ??ë°©íŒ¨ ??5ë¶„ê°„ 80ë¸”ë¡ ?ˆì´??    private void execute016(Player player) {
        player.sendMessage("Â§b[ê°ì‹œ??ë°©íŒ¨] ë°˜ê²½ 80ë¸”ë¡ ê°ì‹œ êµ¬ì—­??5ë¶„ê°„ ?„ê°œ?©ë‹ˆ??");
        Location center = player.getLocation().clone();
        
        // ì¤‘ì‹¬ë¶€???¬ë????Œí‹°??ë¦¬ìŠ¤??        BukkitTask particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                center.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTMENT_TABLE, center.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 6000) { 
                    this.cancel(); 
                    particleTask.cancel();
                    if (player.isOnline()) player.sendMessage("Â§c[ê°ì‹œ??ë°©íŒ¨] ê°ì‹œ êµ¬ì—­???´ì œ?˜ì—ˆ?µë‹ˆ??");
                    return; 
                }
                ticks += 40;

                // ??ê°ì? ë°??¤ìš´???„êµ° ê°ì?
                for (Player p : player.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    // #008 ê·¸ë¦¼??ë§?ë©´ì—­ (??ê°ì? ?œì—ë§?ë¬´ì‹œ, ?„êµ° ?¤ìš´?€ ê°ì???
                    boolean isShadowed = active008Shadow.contains(p.getUniqueId());
                    
                    if (p.getLocation().distanceSquared(center) <= 6400) {
                        Vector dir = p.getLocation().toVector().subtract(center.toVector());
                        int dist = (int) dir.length();
                        String direction = getCardinalDirection(dir);
                        
                        boolean isSameTeam = plugin.getTeamManager().isSameTeam(player, p);
                        if (!isSameTeam) {
                            if (isShadowed) continue; // ê·¸ë¦¼??ë§?ë°œë™ ì¤‘ì¸ ?ì? ë¬´ì‹œ
                            player.sendTitle("Â§c[ê²½ê³ ] ??ê°ì?", "Â§e" + direction + " " + dist + "ë¸”ë¡", 0, 40, 10);
                            return; // 1ê°?ë°œê²¬???°ì„  ê²½ê³  ??ë¦¬í„´
                        } else if (plugin.getCombatManager().isDowned(p)) {
                            player.sendTitle("Â§4[ë¹„ìƒ] ?„êµ° ?¤ìš´", "Â§c" + direction + " " + dist + "ë¸”ë¡", 0, 40, 10);
                            return;
                        }
                    }
                }
                
                // ë´‰ì¸ ? ë¬¼ ê°ì?
                org.bukkit.entity.Item nearestSealed = plugin.getSealedRelicManager().getNearestSealed(center, 80);
                if (nearestSealed != null) {
                    Vector dir = nearestSealed.getLocation().toVector().subtract(center.toVector());
                    int dist = (int) dir.length();
                    String direction = getCardinalDirection(dir);
                    player.sendTitle("Â§a[?Œë¦¼] ë´‰ì¸ ? ë¬¼ ê°ì?", "Â§b" + direction + " " + dist + "ë¸”ë¡", 0, 40, 10);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // 2ì´ˆë§ˆ???¤ìº”
    }

    // #015 ?Œìˆ˜?ì˜ ê°ˆê³ ë¦???20ë¸”ë¡ ë°?ë´‰ì¸ ? ë¬¼???Œì–´?¤ê¸°
    private void execute015(Player player) {
        org.bukkit.entity.Item nearest = plugin.getSealedRelicManager().getNearestSealed(player.getLocation(), 20);
        if (nearest == null) {
            player.sendMessage("Â§c[RelicWars] 20ë¸”ë¡ ?´ë‚´??ë´‰ì¸??? ë¬¼???†ìŠµ?ˆë‹¤!");
            return;
        }

        player.sendMessage("Â§6[?Œìˆ˜?ì˜ ê°ˆê³ ë¦? 3ì´ˆê°„ ?•ì‹ ??ì§‘ì¤‘?˜ì—¬ ? ë¬¼???Œì–´?µë‹ˆ?? (?´ë™/?¼ê²© ??ì·¨ì†Œ)");
        Location startLoc = player.getLocation().clone();
        active015Casting.add(player.getUniqueId());

        // ìºìŠ¤?? 3ì´??€ê¸?(60??
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !active015Casting.contains(player.getUniqueId())) {
                    this.cancel();
                    if (player.isOnline()) player.sendMessage("Â§c[?Œìˆ˜?ì˜ ê°ˆê³ ë¦? ìºìŠ¤?…ì´ ì·¨ì†Œ?˜ì—ˆ?µë‹ˆ??");
                    return;
                }
                
                if (player.getLocation().distanceSquared(startLoc) > 0.25) {
                    this.cancel();
                    active015Casting.remove(player.getUniqueId());
                    player.sendMessage("Â§c[?Œìˆ˜?ì˜ ê°ˆê³ ë¦? ?´ë™?˜ì—¬ ìºìŠ¤?…ì´ ì·¨ì†Œ?˜ì—ˆ?µë‹ˆ??");
                    return;
                }

                ticks += 5;
                if (ticks >= 60) {
                    this.cancel();
                    active015Casting.remove(player.getUniqueId());
                    
                    if (!nearest.isValid()) {
                        player.sendMessage("Â§c[?Œìˆ˜?ì˜ ê°ˆê³ ë¦? ?€??? ë¬¼???¬ë¼ì¡ŒìŠµ?ˆë‹¤.");
                        return;
                    }
                    
                    // ?Œì–´?¤ê¸° ?œì‘
                    player.sendMessage("Â§a[?Œìˆ˜?ì˜ ê°ˆê³ ë¦? ? ë¬¼???šì•„ì±˜ìŠµ?ˆë‹¤!");
                    startPullingRelic(player, nearest);
                } else {
                    player.spawnParticle(org.bukkit.Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
    
    private void startPullingRelic(Player player, org.bukkit.entity.Item target) {
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ >= 20 || !target.isValid() || !player.isOnline()) { 
                    this.cancel(); 
                    if (target.isValid()) {
                        plugin.getSealedRelicManager().reduceSealTime(target, 0.5); // ?œê°„ ?ˆë°˜ ?¨ì¶•
                        player.sendMessage("Â§d[?Œìˆ˜?ì˜ ê°ˆê³ ë¦? ? ë¬¼???„ì°©?ˆìœ¼ë©? ë´‰ì¸ ?œê°„???ˆë°˜?¼ë¡œ ?¨ì¶•?˜ì—ˆ?µë‹ˆ??");
                    }
                    return; 
                }

                Location current = target.getLocation();
                Location playerLoc = player.getLocation();
                org.bukkit.util.Vector direction = playerLoc.toVector().subtract(current.toVector()).normalize().multiply(1.0);
                target.teleport(current.add(direction));

                // ë¹?ê¶¤ì  ?Œí‹°??                player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, current, 5, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L); // 1ì´?20??ë§Œì— ?„ì°©
    }

    // ======================== Batch 4: #014 ~ #010 ========================

    // #014 ?„ì¥??ë¿???60ì´ˆê°„ ?€ ?œì•¼ ê³µìœ  + ?´ì† ë²„í”„
    private void execute014(Player player) {
        player.sendMessage("Â§6[?„ì¥??ë¿? ë¿”í”¼ë¦¬ê? ?¸ë ¤ ?¼ì§‘?ˆë‹¤! 60ì´ˆê°„ ?€ ?œì•¼ ê³µìœ  + ?´ì† ì¦ê?!");
        String teamId = plugin.getTeamManager().getTeamId(player);

        if (teamId != null) {
            for (UUID memberUuid : plugin.getTeamManager().getTeamMembers(teamId)) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    member.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0, false, false));
                    member.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 0, false, false));
                    member.sendMessage("Â§6[?„ì¥??ë¿? ?€?ì˜ ?„ì¹˜ê°€ 60ì´ˆê°„ ê³µìœ ?©ë‹ˆ?? ?´ë™ ?ë„ ì¦ê?!");
                }
            }
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 0, false, false));
        }

        // 300ë¸”ë¡ ??? ë¬¼ ë³´ìœ ??ë°©í–¥ ?œì‹œ
        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            if (p.getLocation().distance(player.getLocation()) <= 300) {
                int relics = plugin.getRelicManager().countPlayerRelics(p);
                if (relics > 0) {
                    Vector dir = p.getLocation().toVector().subtract(player.getLocation().toVector());
                    String direction = getCardinalDirection(dir);
                    player.sendMessage("Â§e  [?ì?] " + direction + " ë°©í–¥??? ë¬¼ ë³´ìœ ??(" + relics + "ê°?");
                }
            }
        }
    }

    // #013 ?ìš•??ë¼???ë³´ìŠ¤ë¥??ì§„??ë°°ë‹¬
    private void execute013(Player player) {
        player.sendMessage("Â§4[?ìš•??ë¼? ?¼ì˜ ë§ˆì»¤ë¥??¤ì¹˜?ˆìŠµ?ˆë‹¤! ?ìš•??ì¶”ì ?ê? ???„ì¹˜ë¡?ì§ˆì£¼?©ë‹ˆ??");
        // MVP: ë§ˆì»¤ ë°˜ê²½ 30ë¸”ë¡?????„ì¹˜ 10ì´ˆê°„ ?¸ì¶œ
        Location marker = player.getLocation().clone();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 200) { this.cancel(); return; } // 10ì´?                ticks += 20;

                for (Player p : player.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;
                    if (p.getLocation().distance(marker) <= 30.0) {
                        player.sendMessage("Â§c  [ë§ˆì»¤] " + p.getName() + " ê°ì? ??" + (int) p.getLocation().distance(marker) + "ë¸”ë¡");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // #012 ?½íƒˆ?ì˜ ?¥ê°‘ ?????•ì‹ ??30 ê°•íƒˆ (MVP: ?”ë²„??ë¶€??
    private void execute012(Player player) {
        Player target = null;
        for (Entity e : player.getNearbyEntities(10, 10, 10)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;
            target = p;
            break;
        }

        if (target == null) {
            player.sendMessage("Â§c[RelicWars] 10ë¸”ë¡ ?´ë‚´?????Œë ˆ?´ì–´ê°€ ?†ìŠµ?ˆë‹¤!");
            return;
        }

        Player victim = target;
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false));
        int stolenSanity = Math.min(30, plugin.getSanityManager().getSanity(victim));
        plugin.getSanityManager().setSanity(victim, plugin.getSanityManager().getSanity(victim) - stolenSanity);
        plugin.getSanityManager().restoreSanity(player, stolenSanity);

        victim.sendMessage("§4[ì•½íƒˆìì˜ ì¥ê°‘] ëˆ„êµ°ê°€ ë‹¹ì‹ ì˜ ì •ì‹ ë ¥ì„ " + stolenSanity + " ê°•íƒˆí–ˆìŠµë‹ˆë‹¤!");
        player.sendMessage("§a[ì•½íƒˆìì˜ ì¥ê°‘] " + victim.getName() + "ì˜ ì •ì‹ ë ¥ì„ " + stolenSanity + " ê°•íƒˆí–ˆìŠµë‹ˆë‹¤!");
    }

    // #011 ê³µëª…??ì¢???300ë¸”ë¡ ??? ë¬¼ ë³´ìœ ???„ì› ?„ì¹˜ ?ë°œ
    private void execute011(Player player) {
        player.sendMessage("Â§a[ê³µëª…??ì¢? ë°˜ê²½ 300ë¸”ë¡ ?´ì˜ ëª¨ë“  ? ë¬¼ ?Œìœ ?ë? ?ì??©ë‹ˆ??");
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "Â§b[?Œë¬¸] ?´ë””? ê? ë§‘ì? ì¢…ì†Œë¦¬ê? ?¸ë ¤?¼ì§‘?ˆë‹¤.");

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BELL_RESONATE, 3.0f, 1.0f);

        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            // #008 ê·¸ë¦¼??ë§?ë©´ì—­
            if (active008Shadow.contains(p.getUniqueId())) continue;
            if (p.getLocation().distanceSquared(player.getLocation()) > 90000) continue; // 300 blocks

            int relicCount = plugin.getRelicManager().countPlayerRelics(p);
            if (relicCount > 0) {
                // ë°œê´‘ 3ì´?+ ë¶‰ì? ë²¼ë½ ?Œí‹°??3ì´?                p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
                
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 60 || !p.isOnline()) { this.cancel(); return; }
                        ticks += 5;
                        
                        // ë¨¸ë¦¬ ?„ë¡œ 10ë¸”ë¡ ?’ì´ê¹Œì? ë¶‰ì? ë²ˆê°œ ê¸°ë‘¥
                        org.bukkit.Particle.DustOptions red = new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 2.0f);
                        for (double y = 0; y <= 10; y += 0.5) {
                            p.getWorld().spawnParticle(org.bukkit.Particle.DUST, p.getLocation().add(0, y, 0), 2, 0.2, 0.2, 0.2, red);
                        }
                    }
                }.runTaskTimer(plugin, 0L, 5L);

                player.sendMessage("Â§e  [?ì?] " + p.getName() + " ??? ë¬¼ " + relicCount + "ê°?ë³´ìœ  (" +
                        (int) p.getLocation().distance(player.getLocation()) + "ë¸”ë¡)");
                p.sendMessage("Â§c[ê²½ê³ ] ê³µëª…??ì¢…ì— ?˜í•´ ?¹ì‹ ???„ì¹˜ê°€ ?¸ì¶œ?˜ì—ˆ?µë‹ˆ??");
            }
        }
    }

    // #010 ì¶©ê²© ì½”ì–´ ??ê´‘ì—­ ?‰ë°± + 5ì´?EMP (?í˜¸?‘ìš© ì°¨ë‹¨)
    private void execute010(Player player) {
        player.sendMessage("Â§4[ì¶©ê²© ì½”ì–´] ë°˜ê²½ 15ë¸”ë¡ ?‰ë°± ë°?20ë¸”ë¡ EMP ë°œë™!");
        Bukkit.broadcast(Component.text("Â§4[EMP] ê±°ë?????°œ?Œê³¼ ?¨ê»˜ ì£¼ë???ê¸°ìš´??ì¦ë°œ?©ë‹ˆ??"));
        
        player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_HUGE, player.getLocation(), 1);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);

        // 20ë¸”ë¡ ?????ìƒ‰
        for (Entity e : player.getNearbyEntities(20, 20, 20)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;

            // 15ë¸”ë¡ ??ê°•í•œ ?‰ë°±
            if (p.getLocation().distanceSquared(player.getLocation()) <= 225) {
                Vector knockback = p.getLocation().toVector().subtract(player.getLocation().toVector());
                if (knockback.lengthSquared() == 0) knockback = new Vector(0, 1, 0);
                else knockback = knockback.normalize().multiply(3.0).setY(1.2);
                p.setVelocity(knockback);
            }

            // 20ë¸”ë¡ ??EMP ?”ë²„??            p.sendMessage("Â§c[EMP] ì¶©ê²©?Œì— ?˜í•´ 5ì´ˆê°„ ?í˜¸?‘ìš© ë°?? ë¬¼ ?¬ìš©??ì°¨ë‹¨?©ë‹ˆ??");
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 2, false, false));
            
            UUID id = p.getUniqueId();
            active010EMP.add(id);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                active010EMP.remove(id);
                if (p.isOnline()) p.sendMessage("Â§a[EMP] ?œìŠ¤??ë³µêµ¬ ?„ë£Œ. ? ë¬¼ ?¬ìš©??ê°€?¥í•©?ˆë‹¤.");
            }, 100L); // 5ì´?        }
    }

    // ======================== Batch 5: #009 ~ #005 ========================

    // #009 ?Œê´´?ì˜ ????ë´‰ì¸ ì¦‰ì‹œ ?Œê´´
    private void execute009(Player player) {
        org.bukkit.entity.Item nearest = plugin.getSealedRelicManager().getNearestSealed(player.getLocation(), 50);
        if (nearest == null) {
            player.sendMessage("Â§c[RelicWars] 50ë¸”ë¡ ?´ë‚´??ë´‰ì¸??? ë¬¼???†ìŠµ?ˆë‹¤!");
            return;
        }

        plugin.getSealedRelicManager().forceUnseal(nearest);
        player.sendMessage("Â§5[?Œê´´?ì˜ ?? ë´‰ì¸??ì¦‰ì‹œ ?Œê´´?ˆìŠµ?ˆë‹¤! ? ë¬¼???ë“ ê°€?¥í•©?ˆë‹¤!");
        Bukkit.broadcast(Component.text("Â§5[?Œê´´] ?„êµ°ê°€ ë´‰ì¸ ? ë¬¼??ë´‰ì¸??ê°•ì œë¡??Œê´´?ˆìŠµ?ˆë‹¤!"));
    }

    // #006 ì°¨ì› ?„ì•½????30ë¸”ë¡ ?œê°„?´ë™ + 5ì´???ë³µê?
    private void execute006(Player player) {
        Location origin = player.getLocation().clone();
        
        // 30ë¸”ë¡ ??ì¢Œí‘œ ê³„ì‚° (ë²??µê³¼ ë°©ì?)
        Block targetBlock = player.getTargetBlockExact(30, org.bukkit.FluidCollisionMode.NEVER);
        Location targetLoc;
        if (targetBlock != null) {
            targetLoc = targetBlock.getLocation().add(0, 1, 0);
            targetLoc.setDirection(origin.getDirection());
        } else {
            Vector dir = origin.getDirection().normalize().multiply(30);
            targetLoc = origin.clone().add(dir);
        }

        // ì¶œë°œì§€ ?Œí‹°??        player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, origin, 50, 0.5, 1.0, 0.5, 0.1);
        player.getWorld().playSound(origin, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // ?€ë¡œê·¸???”ìƒ (ê°‘ì˜· ê±°ì¹˜?€)
        org.bukkit.entity.ArmorStand hologram = player.getWorld().spawn(origin, org.bukkit.entity.ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setArms(true);
            stand.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD)); // ë¨¸ë¦¬(?Œë ˆ?´ì–´ ë¨¸ë¦¬ë¡??€ì²?ê°€??
            stand.getEquipment().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
            stand.setCustomName("Â§d" + player.getName() + "???”ìƒ");
            stand.setCustomNameVisible(true);
        });

        // ?°ì´??ê¸°ë¡
        UUID id = player.getUniqueId();
        active006Leap.put(id, new LeapData(origin, player.getHealth(), hologram));

        // ?”ë ˆ?¬íŠ¸
        player.teleport(targetLoc);
        player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, targetLoc, 50, 0.5, 1.0, 0.5, 0.1);
        player.getWorld().playSound(targetLoc, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.sendMessage("Â§d[ì°¨ì› ?„ì•½?? ?„ì•½?ˆìŠµ?ˆë‹¤! 5ì´??´ì— ?¤ì‹œ ?¬ìš©?˜ë©´ ë³µê??©ë‹ˆ??");

        // 5ì´??€?´ë¨¸
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            LeapData data = active006Leap.remove(id);
            if (data != null) {
                if (!data.hologram.isDead()) data.hologram.remove();
                if (player.isOnline()) player.sendMessage("Â§c[ì°¨ì› ?„ì•½?? ë³µê? ?œê°„??ì´ˆê³¼?˜ì—ˆ?µë‹ˆ??");
            }
        }, 100L); // 5ì´?    }
    
    private void execute006Return(Player player) {
        LeapData data = active006Leap.remove(player.getUniqueId());
        if (data == null) return;

        if (!data.hologram.isDead()) data.hologram.remove();

        // ì²´ë ¥ ë³µêµ¬
        player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), Math.max(1.0, data.health)));
        
        // ?íƒœ?´ìƒ ?´ì œ (?”ë²„???œê±°)
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // ë³µê? ?”ë ˆ?¬íŠ¸
        Location returnLoc = data.origin;
        player.getWorld().spawnParticle(org.bukkit.Particle.REVERSE_PORTAL, player.getLocation(), 50, 0.5, 1.0, 0.5, 0.1);
        player.teleport(returnLoc);
        player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, returnLoc, 50, 0.5, 1.0, 0.5, 0.1);
        player.getWorld().playSound(returnLoc, org.bukkit.Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);
        
        player.sendMessage("Â§a[ì°¨ì› ?„ì•½?? ?œê°„???˜ê°???ë˜ ?„ì¹˜ë¡?ë³µê??ˆìŠµ?ˆë‹¤! ?íƒœê°€ ?Œë³µ?©ë‹ˆ??");
    }

    // #008 ê·¸ë¦¼??ë§???3ë¶„ê°„ ëª¨ë“  ?ì? ë¬´íš¨??+ ê°€ì§?? í˜¸
    private void execute008(Player player) {
        player.sendMessage("Â§8[ê·¸ë¦¼??ë§? 3ë¶„ê°„ ?€ ?„ì²´ê°€ ëª¨ë“  ?ì??ì„œ ?¬ë¼ì§‘ë‹ˆ??");
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "Â§b[?Œë¬¸] ì£¼ë???ì§™ì? ê·¸ë¦¼?ê? ?œë¦¬?°ë©° ê¸°ìš´???¬ë¼ì§‘ë‹ˆ??");

        // ?€???ì? ë©´ì—­ 3ë¶?        String teamId = plugin.getTeamManager().getTeamId(player);
        if (teamId != null) {
            for (UUID memberUuid : plugin.getTeamManager().getTeamMembers(teamId)) {
                Player member = Bukkit.getPlayer(memberUuid);
                active008Shadow.add(memberUuid);
                if (member != null && member.isOnline()) {
                    member.sendMessage("Â§8[ê·¸ë¦¼??ë§? 3ë¶„ê°„ ëª¨ë“  ?ì??ì„œ ?„ë²½?˜ê²Œ ?€?ë©?ˆë‹¤!");
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> active008Shadow.remove(memberUuid), 3600L);
            }
        } else {
            active008Shadow.add(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> active008Shadow.remove(player.getUniqueId()), 3600L);
        }

        // ê°€ì§?? í˜¸ 3ê°??ì„± (ArmorStand)
        for (int i = 0; i < 3; i++) {
            double rx = (Math.random() - 0.5) * 200;
            double rz = (Math.random() - 0.5) * 200;
            Location fakeLoc = player.getLocation().clone().add(rx, 0, rz);
            fakeLoc.setY(player.getWorld().getHighestBlockYAt(fakeLoc));

            org.bukkit.entity.ArmorStand decoy = player.getWorld().spawn(fakeLoc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setSmall(true);
                stand.setMarker(true);
                stand.setCustomName("Â§c[ê°€ì§?? í˜¸]");
                stand.setCustomNameVisible(false);
            });
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!decoy.isDead()) decoy.remove(); }, 3600L);
        }
    }

    // #007 ?Œìˆ˜ê¾¼ì˜ ????15ì´??ˆë? ë°©ì–´ë§?(??¥)
    private void execute007(Player player) {
        player.sendMessage("Â§b[?Œìˆ˜ê¾¼ì˜ ?? ë°˜ê²½ 8ë¸”ë¡ ?ˆë? ë°©ì–´ë§???¥)??15ì´ˆê°„ ?„ê°œ?©ë‹ˆ??");
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "Â§b[?Œë¬¸] ê±°ë???ë°©ë²½???¸ì›Œì§€??ì§„ë™???ê»´ì§‘ë‹ˆ??");

        Location center = player.getLocation().clone();
        String teamId = plugin.getTeamManager().getTeamId(player);
        String key = teamId != null ? teamId : player.getUniqueId().toString();
        active007Dome.put(center, key);

        // ?Œí‹°???€?´ë¨¸ (??ê²½ê³„ ?œì‹œ) ë°???¥ ë°€?´ë‚´ê¸?        BukkitTask forcefieldTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks += 2;
                if (ticks % 20 == 0) {
                    for (double t = 0; t <= Math.PI; t += Math.PI / 10) {
                        for (double p = 0; p <= 2 * Math.PI; p += Math.PI / 10) {
                            double x = 8 * Math.sin(t) * Math.cos(p);
                            double y = 8 * Math.cos(t);
                            double z = 8 * Math.sin(t) * Math.sin(p);
                            if (y >= 0) {
                                center.getWorld().spawnParticle(org.bukkit.Particle.SOUL, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
                            }
                        }
                    }
                }

                // ??ë°€?´ë‚´ê¸?ë°??¬ì‚¬ì²?ì°¨ë‹¨
                for (Entity e : center.getWorld().getNearbyEntities(center, 8.5, 8.5, 8.5)) {
                    if (e instanceof org.bukkit.entity.Projectile proj) {
                        if (proj.getShooter() instanceof Player shooter) {
                            String shooterTeam = plugin.getTeamManager().getTeamId(shooter);
                            String sKey = shooterTeam != null ? shooterTeam : shooter.getUniqueId().toString();
                            if (!sKey.equals(key)) {
                                proj.remove(); // ???¬ì‚¬ì²??Œë©¸
                                center.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_LARGE, proj.getLocation(), 2);
                            }
                        }
                    } else if (e instanceof Player p) {
                        String pTeam = plugin.getTeamManager().getTeamId(p);
                        String pKey = pTeam != null ? pTeam : p.getUniqueId().toString();
                        if (!pKey.equals(key) && p.getLocation().distanceSquared(center) <= 64) {
                            Vector push = p.getLocation().toVector().subtract(center.toVector());
                            if (push.lengthSquared() == 0) push = new Vector(0, 1, 0);
                            else push = push.normalize().multiply(1.5).setY(0.2);
                            p.setVelocity(push);
                            p.sendMessage("Â§c[?Œìˆ˜ê¾¼ì˜ ?? ?ì˜ ??¥???•ê²¨?¬ìŠµ?ˆë‹¤!");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // 0.1ì´ˆë§ˆ??
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active007Dome.remove(center);
            forcefieldTask.cancel();
            if (player.isOnline()) player.sendMessage("Â§c[?Œìˆ˜ê¾¼ì˜ ?? ë°©ì–´ë§‰ì´ ?´ì œ?˜ì—ˆ?µë‹ˆ??");
        }, 300L); // 15ì´?    }


    // #005 ë¶ˆë©¸???¬ì¥ ???¨ì‹œë¸? ?¤ìš´ ë¬´ì‹œ 1??(MVP: ?¡í‹°ë¸Œë¡œ ?€ì²?
    private void execute005(Player player) {
        player.sendMessage("Â§6[ë¶ˆë©¸???¬ì¥] ?¤ìŒ ì¹˜ëª…?ì„ 1??ë¬´ì‹œ?©ë‹ˆ?? (90ë¶?ì¿¨í???");
        player.sendMessage("Â§7  ì²´ë ¥??0???˜ì–´?????œê°„ ì²´ë ¥ 100%ë¡?ë¶€?œí•˜ë©?ì£¼ë? ?ì„ ë°€ì³ëƒ…?ˆë‹¤.");

        UUID id = player.getUniqueId();
        active029FallImmunity.add(id); // ?„ì‹œë¡?ê°™ì? Set ?œìš© (ë³„ë„ Set ?„ìš”?˜ì?ë§?MVP)

        // 8ì´ˆê°„ ë°›ëŠ” ?°ë?ì§€ 50% ê°ì†Œ
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 1, false, false));

        // ì£¼ë? ??ë°€ì³ë‚´ê¸?        for (Entity e : player.getNearbyEntities(8, 8, 8)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;
            Vector knockback = p.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(2.5).setY(0.8);
            p.setVelocity(knockback);
        }

        // ?©ê¸ˆ ì¶©ê²©???¨ê³¼
        player.getWorld().strikeLightningEffect(player.getLocation());
    }

    // ======================== Batch 6: #004 ~ #001 ========================

    // #004 ??’???•ê? ??ë°˜ê²½ 30ë¸”ë¡ ê´‘ì—­ ?Œìš° 15ì´?    private void execute004(Player player) {
        Block targetBlock = player.getTargetBlockExact(100);
        Location stormCenter;
        if (targetBlock != null) {
            stormCenter = targetBlock.getLocation();
        } else {
            stormCenter = player.getLocation();
        }

        player.sendMessage("Â§b[??’???•ê?] ?€??ì§€??— ?Œë©¸?ì¸ ?Œìš°ë¥?15ì´ˆê°„ ?Œí™˜?©ë‹ˆ??");
        Bukkit.broadcast(Component.text("Â§4[??’] ?˜ëŠ˜??ì§„ë™?˜ë©° ê´‘ë????Œìš°ê°€ ?Ÿì•„ì§‘ë‹ˆ??"));

        final Location center = stormCenter;
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 300) { this.cancel(); return; } // 15ì´?                ticks += 20;

                // 1ì´ˆë§ˆ??ë²ˆê°œ ?€ê²?ë°??°ë?ì§€
                for (int i = 0; i < 3; i++) {
                    double rx = (Math.random() - 0.5) * 60; // ë°˜ê²½ 30
                    double rz = (Math.random() - 0.5) * 60;
                    Location strike = center.clone().add(rx, 0, rz);
                    strike.setY(center.getWorld().getHighestBlockYAt(strike));
                    center.getWorld().strikeLightningEffect(strike);
                }

                // ë²”ìœ„ ???ì—ê²??¼í•´ ë°??”ë²„??                for (Player p : center.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 900) { // 30ë¸”ë¡
                        p.damage(2.0, player); // 1ì¹??°ë?ì§€
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
                        p.sendTitle("Â§c[ë²¼ë½]", "Â§7?ˆì•??ë²ˆì©?…ë‹ˆ??", 0, 20, 10);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1ì´ˆë§ˆ??    }

    // #003 ?ˆë? ì¢Œí‘œ ?˜ì¹¨ë°????¹ì • ? ë¬¼???¤ì‹œê°?ì¢Œí‘œ 3ë¶„ê°„ ?œì‹œ
    private void execute003(Player player) {
        player.sendMessage("Â§5[?ˆë? ì¢Œí‘œ ?˜ì¹¨ë°? ì¶”ì ??? ë¬¼ ë²ˆí˜¸(?«ì)ë¥?ì±„íŒ…???…ë ¥?˜ì„¸?? (1~30)");
        player.sendMessage("Â§7  (?…ë ¥ ?€ê¸??œê°„: 1ë¶?");
        
        active003TrackerWait.add(player.getUniqueId());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active003TrackerWait.remove(player.getUniqueId())) {
                if (player.isOnline()) {
                    player.sendMessage("Â§c[?ˆë? ì¢Œí‘œ ?˜ì¹¨ë°? ?…ë ¥ ?€ê¸??œê°„??ì´ˆê³¼?˜ì—ˆ?µë‹ˆ??");
                }
            }
        }, 1200L); // 1ë¶?    }

    public void start003Tracker(Player player, int targetNum) {
        com.wolfool.relicwars.relic.RelicDefinition def = com.wolfool.relicwars.relic.RelicDefinition.getByNumber(targetNum);
        player.sendMessage("Â§d[?ˆë? ì¢Œí‘œ ?˜ì¹¨ë°? Â§e" + def.getName() + "Â§d??ì¢Œí‘œ ì¶”ì ???œì‘?©ë‹ˆ?? (3ë¶„ê°„ ? ì?)");

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 3600 || !player.isOnline()) { // 3ë¶?                    this.cancel();
                    if (player.isOnline()) player.sendMessage("Â§c[?ˆë? ì¢Œí‘œ ?˜ì¹¨ë°? ì¶”ì ??ì¢…ë£Œ?˜ì—ˆ?µë‹ˆ??");
                    return;
                }
                ticks += 20;

                String ownerUuid = plugin.getDatabaseManager().getRelicOwner(targetNum);
                Location loc = null;

                if (ownerUuid != null) {
                    Player targetPlayer = Bukkit.getPlayer(UUID.fromString(ownerUuid));
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        if (active008Shadow.contains(targetPlayer.getUniqueId())) {
                            player.sendActionBar(Component.text("Â§8[ì¶”ì ] ?€?ì´ ê·¸ë¦¼???ì— ?¨ì—ˆ?µë‹ˆ?? ì¢Œí‘œ ë¶ˆëª…."));
                            return;
                        }
                        loc = targetPlayer.getLocation();
                        if (ticks == 20) {
                            targetPlayer.sendMessage("Â§4[ê²½ê³ ] ?„êµ°ê°€ ?¹ì‹ ??? ë¬¼???¤ì‹œê°„ìœ¼ë¡?ì¶”ì ?˜ê³  ?ˆìŠµ?ˆë‹¤!");
                        }
                    }
                } else {
                    // ?„ë“œ ?œë ?íƒœ (SealedRelicManager ?œìš©)
                    for (org.bukkit.entity.Item display : plugin.getSealedRelicManager().getSealedRelics()) {
                        if (com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(display.getItemStack()) == targetNum) {
                            loc = display.getLocation();
                            break;
                        }
                    }
                }

                if (loc != null) {
                    player.sendActionBar(Component.text("Â§d[ì¶”ì ] Â§e" + def.getName() + " Â§f- X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ()));
                } else {
                    player.sendActionBar(Component.text("Â§c[ì¶”ì ] ?€??? ë¬¼???„ì¹˜ë¥??•ì¸?????†ìŠµ?ˆë‹¤."));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1ì´ˆë§ˆ???…ë°?´íŠ¸
    }

    // #002 ?ìš•???ì¶œ?????¤ìš´???ì—ê²Œì„œ 0.5ì´?ì¦‰ì‹œ ê°•íƒˆ (CombatListener?ì„œ ì²˜ë¦¬??
    private void execute002(Player player) {
        player.sendMessage("Â§c[?ìš•???ì¶œ?? ??? ë¬¼?€ ?ˆê³µ???¬ìš©?˜ëŠ” ê²ƒì´ ?„ë‹™?ˆë‹¤. ?¤ìš´???ì„ ?°í´ë¦?•˜??ë°œë™?˜ì„¸??");
    }

    // #001 ?¤ë©”ê°€ ?„ë¡œ? ì½œ ??ë°œë™ 10ì´???ë°˜ê²½ 100ë¸”ë¡ ??ëª¨ë“  ?Œë ˆ?´ì–´ ì¦‰ì‚¬ (?¬ìš© ???Œë©¸)
    private void execute001(Player player) {
        // ?¸ë²¤? ë¦¬?ì„œ ? ë¬¼ ?? œ
        org.bukkit.inventory.ItemStack handItem = player.getInventory().getItemInMainHand();
        if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(handItem) && com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(handItem) == 1) {
            handItem.setAmount(handItem.getAmount() - 1);
        } else {
            org.bukkit.inventory.ItemStack offItem = player.getInventory().getItemInOffHand();
            if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(offItem) && com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(offItem) == 1) {
                offItem.setAmount(offItem.getAmount() - 1);
            }
        }

        Bukkit.broadcast(Component.text("Â§4========================================"));
        Bukkit.broadcast(Component.text("Â§c[ê²½ê³ ] ?¤ë©”ê°€ ?„ë¡œ? ì½œ??ê°€?™ë˜?ˆìŠµ?ˆë‹¤. 10ì´???ì¢…ë§???„ë˜?©ë‹ˆ??"));
        Bukkit.broadcast(Component.text("Â§4========================================"));

        // ?œì „??ë¬´ì  ë°??´ë™ ë¶ˆê?
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 255, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, 128, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 255, false, false));
        
        Location origin = player.getLocation().clone();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                ticks += 20; // 1ì´?                int left = 10 - (ticks / 20);

                if (left > 0) {
                    Bukkit.broadcast(Component.text("Â§c[?¤ë©”ê°€ ?„ë¡œ? ì½œ] ì¢…ë§ê¹Œì?... " + left + "ì´?));
                    origin.getWorld().playSound(origin, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                } else {
                    this.cancel();
                    Bukkit.broadcast(Component.text("Â§0========================================"));
                    Bukkit.broadcast(Component.text("Â§4[?¤ë©”ê°€ ?„ë¡œ? ì½œ] ì¢…ë§???„ë˜?ˆìŠµ?ˆë‹¤."));
                    Bukkit.broadcast(Component.text("Â§0========================================"));

                    origin.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, origin, 1);
                    origin.getWorld().playSound(origin, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);

                    for (Player p : origin.getWorld().getPlayers()) {
                        if (p.equals(player)) continue;
                        if (p.getLocation().distanceSquared(origin) <= 10000) { // 100ë¸”ë¡ ë°˜ê²½
                            // ëª¨ë“  ë°©ì–´/ë¬´ì  ë¬´ì‹œ ?ˆë? ì¦‰ì‚¬
                            p.setHealth(0.0);
                            p.sendMessage("Â§4[ì¢…ë§] ?¤ë©”ê°€ ?„ë¡œ? ì½œ???˜í•´ ?Œë©¸?ˆìŠµ?ˆë‹¤.");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ======================== ? í‹¸ë¦¬í‹° ========================

    private String getCardinalDirection(Vector dir) {
        double angle = Math.toDegrees(Math.atan2(dir.getZ(), dir.getX()));
        if (angle < 0) angle += 360;

        if (angle < 22.5 || angle >= 337.5) return "?™ìª½";
        if (angle < 67.5) return "?¨ë™ìª?;
        if (angle < 112.5) return "?¨ìª½";
        if (angle < 157.5) return "?¨ì„œìª?;
        if (angle < 202.5) return "?œìª½";
        if (angle < 247.5) return "ë¶ì„œìª?;
        if (angle < 292.5) return "ë¶ìª½";
        return "ë¶ë™ìª?;
    }

    /**
     * ? ë¬¼ ë²ˆí˜¸???°ë¥¸ ?•ì‹ ???Œëª¨?‰ì„ ë°˜í™˜?©ë‹ˆ??
     * 1~2?¨ê³„(#030~#020): 0 (?Œëª¨ ?†ìŒ)
     * 3?¨ê³„(#019~#011): 10
     * 4?¨ê³„(#010~#006): 20
     * 5?¨ê³„(#005~#001): 30
     */
    private int getSanityCost(int relicNumber) {
        if (relicNumber >= 20) return 0;   // 1~2?¨ê³„
        if (relicNumber >= 11) return 10;  // 3?¨ê³„
        if (relicNumber >= 6) return 20;   // 4?¨ê³„
        if (relicNumber >= 1) return 30;   // 5?¨ê³„
        return 0;
    }
}
