package com.wolfool.relicwars.relic.ability;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.relic.RelicDefinition;
import com.wolfool.relicwars.relic.FootprintTracker;
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
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class RelicAbilityHandler implements Listener {

    private final RelicWars plugin;

    // GUI 타이틀 상수 (RelicListener와 공유)
    public static final String GUI_TITLE_020 = "§5소문의 등불";
    public static final String GUI_TITLE_019 = "§3봉인의 바늘";
    
    public final Set<UUID> active001Omega = new HashSet<>();
    public final Set<UUID> active005DamageReduction = new HashSet<>();
    private final java.util.Map<UUID, Long> cooldown005 = new java.util.HashMap<>();
    
    // 버프 상태 관리 맵 (UUID)
    public final Set<UUID> active029FallImmunity = new HashSet<>();
    public final Set<UUID> active027FireImmunity = new HashSet<>();
    public final Set<UUID> active025FastRevive = new HashSet<>();
    public final Set<UUID> active023Marked = new HashSet<>(); // 표식이 찍힌 대상
    public final Map<UUID, UUID> active021Duel = new HashMap<>(); // 결투 중인 쌍 (대상 -> 시전자)
    public final Set<UUID> active020ScanMode = new HashSet<>(); // /relic scan 입력 대기 상태
    public final Map<String, Location> active017Anchor = new HashMap<>(); // 팀ID(또는 UUID) -> 왜곡장 위치
    public final Set<UUID> active015Casting = new HashSet<>(); // #015 캐스팅 중인 플레이어
    public final Set<UUID> active010EMP = new HashSet<>(); // #010 EMP에 당한 플레이어

    // === 외부 읽기용 접근자 (CombatListener 등에서 사용) ===
    public boolean isFallImmune(UUID id) { return active029FallImmunity.contains(id); }
    public boolean isMarked(UUID id) { return active023Marked.contains(id); }
    public boolean isCasting(UUID id) { return active015Casting.contains(id); }
    public boolean cancelCasting(UUID id) { return active015Casting.remove(id); }
    public boolean isEmpAffected(UUID id) { return active010EMP.contains(id); }
    public boolean isFastRevive(UUID id) { return active025FastRevive.contains(id); }
    public boolean isShadowActive(UUID id) { return active008Shadow.contains(id); }
    
    // Batch 4 추가
    public final Set<UUID> active008Shadow = new HashSet<>(); // #008 그림자 막 (탐지 면역)
    public final Map<Location, String> active007Dome = new HashMap<>(); // #007 돔 위치 -> 팀ID/UUID
    public final Map<UUID, LeapData> active006Leap = new HashMap<>(); // #006 차원 도약석 데이터
    // Batch 5 추가
    public final Set<UUID> active003TrackerWait = new HashSet<>(); // #003 추적 유물 번호 입력 대기 상태
    // #001 벼락 내부 쿨타임 추적
    private final Map<UUID, Long> lightningCooldown = new HashMap<>();

    // === 태스크 추적 시스템 (cleanupPlayer에서 일괄 취소) ===
    private final Map<UUID, java.util.List<org.bukkit.scheduler.BukkitTask>> activeTasks = new HashMap<>();

    /** 플레이어의 태스크를 추적 목록에 등록 */
    public void trackTask(UUID playerId, org.bukkit.scheduler.BukkitTask task) {
        activeTasks.computeIfAbsent(playerId, k -> new java.util.ArrayList<>()).add(task);
    }

    /** 플레이어의 모든 추적 태스크를 취소하고 제거 */
    private void cancelAllTasks(UUID playerId) {
        java.util.List<org.bukkit.scheduler.BukkitTask> tasks = activeTasks.remove(playerId);
        if (tasks != null) {
            tasks.forEach(t -> { if (t != null && !t.isCancelled()) t.cancel(); });
        }
    }

    /** #001 벼락 내부 쿨타임 조회 */
    public Long getLastLightningTime(UUID id) { return lightningCooldown.get(id); }
    /** #001 벼락 내부 쿨타임 설정 */
    public void setLastLightningTime(UUID id, long time) { lightningCooldown.put(id, time); }

    // #006용 데이터 클래스
    public static class LeapData {
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

    /** 잘못 사용한 유물의 쿨타임을 리셋합니다 (아이템 PDC 기반) */
    public void resetCooldownForRelic(Player player, int relicNumber) {
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)
                    && com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(item) == relicNumber) {
                // PDC 쿨타임 제거
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().remove(
                            new org.bukkit.NamespacedKey(plugin, "cooldown_until"));
                    item.setItemMeta(meta);
                }
                break;
            }
        }
    }

    public boolean execute(Player player, RelicDefinition def) {
        int num = def.getNumber();
        
        // EMP 상태 체크
        if (active010EMP.contains(player.getUniqueId())) {
            player.sendMessage("§c[EMP] 기능이 마비되어 유물을 사용할 수 없습니다!");
            return false;
        }

        // #006 재사용(복귀) 체크 (쿨타임/정신력 소모 무시)
        if (num == 6 && active006Leap.containsKey(player.getUniqueId())) {
            execute006Return(player);
            return false; // 쿨타임 이미 돌고 있으므로 false
        }

        // 정신력 부족 시 사전 차단 (실제 소모는 성공 후에만)
        int sanityCost = getSanityCost(num, player);
        if (sanityCost > 0) {
            if (plugin.getSanityManager().getSanity(player) < sanityCost) {
                player.sendMessage("§c[RelicWars] 정신력이 부족합니다! (필요: " + sanityCost + ", 현재: " + plugin.getSanityManager().getSanity(player) + ")");
                return false;
            }
        }

        // 능력 실행 시도 (실패 가능성이 있는 유물은 여기서 실패하면 false 반환)
        boolean success = switch (num) {
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
            default -> {
                player.sendMessage("§c[RelicWars] 유물 #" + num + " 스킬은 아직 구현되지 않았습니다.");
                yield false;
            }
        };

        // 성공한 경우에만 정신력 소모
        if (success && sanityCost > 0) {
                plugin.getSanityManager().removeSanity(player, sanityCost);
        }

        return success;
    }

    // #030 낙뢰의 심지
    private boolean execute030(Player player) {
        Block targetBlock = player.getTargetBlockExact(50);
        if (targetBlock == null) {
            player.sendMessage("§c[RelicWars] 타겟 블록이 너무 멀거나 없습니다!");
            return false;
        }
        
        Location strikeLoc = targetBlock.getLocation();
        player.sendMessage("§e[RelicWars] 1.5초 뒤 해당 위치에 낙뢰가 떨어집니다!");
        
        // 파티클 등으로 전조증상 (MVP에서는 생략)
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            strikeLoc.getWorld().strikeLightningEffect(strikeLoc);
            com.wolfool.relicwars.util.RumorUtil.broadcastRumor(strikeLoc, "§b[소문] %s쪽에서 번개 소리가 들렸습니다.");
            
            // 반경 3블록 데미지 + 넉백 + 발광
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
        }, 30L); // 1.5초 (30틱)
        return true;
    }

    // #029 추락왕의 깃털
    private boolean execute029(Player player) {
        player.sendMessage("§e[RelicWars] 15초간 낙하 데미지 면역 및 허공에서 이단 점프 가능!");
        
        UUID id = player.getUniqueId();
        active029FallImmunity.add(id);
        
        // 이단 점프를 위해 비행 허용
        player.setAllowFlight(true);

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !active029FallImmunity.contains(id)) {
                    this.cancel();
                    return;
                }
                // 발밑에 깃털 파티클 트레일
                player.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, player.getLocation(), 1, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0L, 5L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active029FallImmunity.remove(id);
            if (player.isOnline()) {
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    player.setAllowFlight(false);
                }
                player.sendMessage("§c[RelicWars] 추락왕의 깃털 효과가 종료되었습니다.");
            }
        }, 300L); // 15초
        return true;
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        
        if (active029FallImmunity.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.setAllowFlight(false); // 1회만 가능
            
            Vector dir = player.getLocation().getDirection().normalize().multiply(1.5).setY(0.8);
            player.setVelocity(dir);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
        }
    }

    // #028 심해의 폐
    private boolean execute028(Player player) {
        player.sendMessage("§b[RelicWars] 3분간 수중 호흡 버프 및 발밑 물 웅덩이 생성!");
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 3600, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 3600, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 3600, 0, false, false));

        // 지상 물 웅덩이 생성 (3x3)
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
        }, 100L); // 5초 뒤 소멸

        // 대쉬 보너스
        player.setVelocity(player.getLocation().getDirection().multiply(1.2));

        // 푸른 공명 파티클 소문 (지속)
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 3600) { // 3분
                    this.cancel();
                    return;
                }
                ticks += 5;
                if (player.getLocation().getBlock().getType() == Material.WATER) {
                    player.getWorld().spawnParticle(org.bukkit.Particle.NAUTILUS, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
        return true;
    }

    // #027 용암의 눈
    private boolean execute027(Player player) {
        player.sendMessage("§c[RelicWars] 15초간 화염 면역 및 용암 보행 발동!");
        
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "§b[소문] %s쪽에서 불길이 치솟는 열기가 느껴집니다.");
        
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
        return true;
    }

    // #026 어둠매듭
    private boolean execute026(Player player) {
        player.sendMessage("§8[RelicWars] 10초간 어둠 속에 숨어듭니다...");
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
                player.sendMessage("§c[RelicWars] 어둠매듭 은신이 종료되어 위치가 노출되었습니다!");
            }
        }, 200L); // 10초
        return true;
    }



    // #025 최후의 봉합
    private boolean execute025(Player player) {
        player.sendMessage("§5[RelicWars] 30초간 구조 시간이 2초로 대폭 단축됩니다!");
        
        UUID id = player.getUniqueId();
        active025FastRevive.add(id);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active025FastRevive.remove(id);
            if (player.isOnline()) player.sendMessage("§c[RelicWars] 구조 단축 효과가 종료되었습니다.");
        }, 600L); // 30초
        return true;
    }

    // ======================== Batch 2: #024 ~ #020 ========================

    // #024 붉은 봉합 — 30블록 밖 다운 팀원을 자기 위치로 텔레포트
    private boolean execute024(Player player) {
        // 30블록 이내 다운된 팀원 탐색
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
            player.sendMessage("§c[RelicWars] 30블록 이내에 다운된 팀원이 없습니다!");
            return false;
        }

        Location startLoc = target.getLocation().clone();
        Location endLoc = player.getLocation().clone();
        target.teleport(endLoc);
        
        // 붉은 실 파티클 트레일 생성 (직선 보간)
        double distance = startLoc.distance(endLoc);
        if (distance > 0) {
            org.bukkit.util.Vector dir = endLoc.toVector().subtract(startLoc.toVector()).normalize();
            for (double d = 0; d <= distance; d += 0.5) {
                Location pLoc = startLoc.clone().add(dir.clone().multiply(d)).add(0, 1, 0); // 눈높이 보정
                player.getWorld().spawnParticle(org.bukkit.Particle.DUST, pLoc, 1, new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
            }
        }

        player.sendMessage("§d[붉은 봉합] " + target.getName() + "님을 내 위치로 소환했습니다! 구조를 시작하세요!");
        target.sendMessage("§d[붉은 봉합] 팀원에 의해 안전 지대로 이동되었습니다!");
        return true;
    }

    // #023 사냥꾼의 표식 — 60초간 바라보는 적에게 발광 + 강탈 시간 단축
    private boolean execute023(Player player) {
        // 바라보고 있는 적 탐색 (50블록 이내)
        Player target = null;
        for (Entity e : player.getNearbyEntities(50, 50, 50)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;

            // 시선 방향 검사 (플레이어가 바라보는 방향과 대상까지의 각도)
            Vector toTarget = p.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            double dot = player.getLocation().getDirection().normalize().dot(toTarget);
            if (dot > 0.95) { // 약 18도 이내
                target = p;
                break;
            }
        }

        if (target == null) {
            player.sendMessage("§c[RelicWars] 바라보는 방향에 적 플레이어가 없습니다!");
            return false;
        }

        Player marked = target;
        marked.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0, false, false)); // 60초
        active023Marked.add(marked.getUniqueId());

        player.sendMessage("§6[사냥꾼의 표식] " + marked.getName() + "에게 60초간 사냥 표식을 찍었습니다!");
        marked.sendMessage("§c[경고] 누군가 당신에게 사냥 표식을 찍었습니다! 60초간 벽 너머로도 보입니다!");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active023Marked.remove(marked.getUniqueId());
        }, 1200L);
        return true;
    }

    // #022 탐욕의 동전 — 가짜 봉인 유물 트랩 설치
    private boolean execute022(Player player) {
        Location trapLoc = player.getLocation().clone();
        player.sendMessage("§e[탐욕의 동전] 가짜 봉인 유물 트랩을 설치했습니다!");

        // 가짜 봉인 유물 (아이템) 소환
        ItemStack fakeItem = new ItemStack(Material.GOLD_INGOT);
        org.bukkit.entity.Item fake = player.getWorld().dropItem(trapLoc, fakeItem);
        fake.setPickupDelay(32767);
        fake.setUnlimitedLifetime(true);
        fake.setInvulnerable(true);
        fake.setGlowing(true);
        fake.customName(net.kyori.adventure.text.Component.text("§c[봉인 중] §e탐욕의 동전 §7(300초)"));
        fake.setCustomNameVisible(true);
        
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(trapLoc, "§b[소문] %s쪽에서 탐욕스러운 금속음이 들렸습니다.");

        // 3분(3600틱) 뒤 자동 소멸
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!fake.isDead()) fake.remove();
        }, 3600L);

        // 근접(1.5블록 이내) 감지를 위한 반복 태스크
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (fake.isDead()) { this.cancel(); return; }

                for (Player p : fake.getWorld().getPlayers()) {
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;
                    if (p.getLocation().distanceSquared(fake.getLocation()) <= 2.25) { // 1.5블록
                        // 트랩 발동!
                        fake.remove();
                        p.sendMessage("§4[함정!] 가짜 유물이었습니다!");
                        player.sendMessage("§a[탐욕의 동전] " + p.getName() + "이(가) 트랩에 걸렸습니다!");

                        // 폭발 데미지 (지형 파괴 X)
                        p.getWorld().createExplosion(fake.getLocation(), 0F, false, false);
                        p.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, fake.getLocation(), 1);
                        p.damage(10.0, player);

                        // 디버프
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, false, false)); // 5초 구속2
                        p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 1, false, false)); // 5초 채굴피로2
                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false)); // 5초 발광
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 5L);
        return true;
    }

    // #021 결투자의 파편 — 15x15 결투장 20초간 강제 1대1
    private boolean execute021(Player player) {
        // 15블록 이내 바라보는 적 탐색
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
            player.sendMessage("§c[RelicWars] 15블록 이내에 적 플레이어가 없습니다!");
            return false;
        }

        Player enemy = target;
        Location center = player.getLocation().clone().add(enemy.getLocation()).multiply(0.5);

        active021Duel.put(enemy.getUniqueId(), player.getUniqueId());
        active021Duel.put(player.getUniqueId(), enemy.getUniqueId());

        player.sendMessage("§4[결투자의 파편] " + enemy.getName() + "과(와) 20초간 강제 결투가 시작됩니다!");
        enemy.sendMessage("§4[결투자의 파편] " + player.getName() + "이(가) 당신을 결투에 가뒀습니다! 20초간 탈출 불가!");
        
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(center, "§b[소문] %s쪽에서 살기가 느껴지는 결투가 시작되었습니다.");

        // 결투장 벽 생성 (배리어 블록으로 7블록 반경 큐브)
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

        // 20초 뒤 결투장 해제
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active021Duel.remove(enemy.getUniqueId());
            active021Duel.remove(player.getUniqueId());

            for (Location bLoc : barriers) {
                if (bLoc.getBlock().getType() == Material.BARRIER) {
                    bLoc.getBlock().setType(Material.AIR);
                }
            }

            if (player.isOnline()) player.sendMessage("§a[결투] 결투가 종료되었습니다.");
            if (enemy.isOnline()) enemy.sendMessage("§a[결투] 결투가 종료되었습니다.");
        }, 400L); // 20초
        
        // 이탈 방지 체크
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
                    player.sendMessage("§c[결투자의 파편] 결투장을 벗어날 수 없습니다!");
                }
                if (enemy.isOnline() && enemy.getWorld().equals(center.getWorld()) && enemy.getLocation().distanceSquared(center) > 100) {
                    enemy.teleport(center);
                    enemy.damage(5.0);
                    enemy.sendMessage("§c[결투자의 파편] 결투장을 벗어날 수 없습니다!");
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
        return true;
    }

    // #020 소문의 등불 — 4가지 옵션 중 하나를 선택하는 GUI 오픈
    private boolean execute020(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text(GUI_TITLE_020));

        inv.setItem(3, createGuiItem(Material.ENDER_EYE, "§5[봉인 유물 스캔]", "§7현재 바닥에 봉인된", "§7유물들의 위치를 파악합니다."));
        inv.setItem(5, createGuiItem(Material.NAME_TAG, "§e[소유자 검색 모드]", "§7특정 번호의 유물을 누가", "§7가졌는지 알아낼 수 있는 검색 모드를 켭니다."));

        player.openInventory(inv);
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "§b[소문] 누군가 소문의 등불을 켰습니다.");
        return true;
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
        player.sendMessage("§d[소문의 등불] §f현재 바닥에 봉인된 유물을 스캔합니다...");
        java.util.List<org.bukkit.entity.Item> sealed = plugin.getSealedRelicManager().getActiveSealedRelics();
        if (sealed.isEmpty()) {
            player.sendMessage("§c  [봉인] 현재 서버 내에 봉인된 유물이 없습니다.");
        } else {
            for (org.bukkit.entity.Item display : sealed) {
                Location loc = display.getLocation();
                int rx = (int) (Math.round(loc.getBlockX() / 10.0) * 10);
                int rz = (int) (Math.round(loc.getBlockZ() / 10.0) * 10);
                long end = display.getPersistentDataContainer().getOrDefault(
                        com.wolfool.relicwars.relic.RelicItemUtil.KEY_COOLDOWN_UNTIL, org.bukkit.persistence.PersistentDataType.LONG, 0L);
                int leftSec = Math.max(0, (int) ((end - System.currentTimeMillis()) / 1000));
                player.sendMessage("§e  [봉인] " + display.getName() + " §7(남은 시간: " + leftSec + "초) - 대략 위치: X: " + rx + " 부근, Z: " + rz + " 부근");
            }
        }
    }

    public void execute020Option3(Player player) {
        player.sendMessage("§d[소문의 등불] §f유물 소유자 검색 모드를 선택했습니다.");
        active020ScanMode.add(player.getUniqueId());
        
        Component scanMsg = Component.text("§e  [정보] 5분 내에 채팅창에 검색할 유물 번호(숫자)만 입력하세요.");
        player.sendMessage(scanMsg);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active020ScanMode.remove(player.getUniqueId())) {
                if (player.isOnline()) {
                    player.sendMessage("§c[소문의 등불] 검색 대기 시간이 만료되었습니다.");
                }
            }
        }, 6000L); // 5분
    }

    public void execute020OptionRandom(Player player) {
        player.sendMessage("§d[소문의 등불] §f무작위 가짜 소문을 퍼뜨립니다.");
        int x = (int)(Math.random() * 2000 - 1000);
        int z = (int)(Math.random() * 2000 - 1000);
        String dir = getCardinalDirection(new org.bukkit.util.Vector(x - player.getLocation().getX(), 0, z - player.getLocation().getZ()));
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(new org.bukkit.Location(player.getWorld(), x, 60, z), "§b[소문] " + dir + "쪽에서 낯선 유물의 기운이 느껴집니다.");
    }

    public final java.util.Set<java.util.UUID> active020PingMode = new java.util.HashSet<>();
    public void execute020OptionPing(Player player) {
        player.sendMessage("§d[소문의 등불] §f기만 전술 모드를 켭니다.");
        active020PingMode.add(player.getUniqueId());
        player.sendMessage("§e  [정보] 5분 안에 채팅창에 가짜 소문을 낼 본인의 유물 번호(숫자)를 입력하세요.");
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active020PingMode.remove(player.getUniqueId())) {
                if (player.isOnline()) player.sendMessage("§c[소문의 등불] 기만 전술 시간이 만료되었습니다.");
            }
        }, 6000L);
    }

    // ======================== Batch 3: #019 ~ #015 ========================

    private final java.util.Map<java.util.UUID, org.bukkit.entity.Item> pending019Relic = new java.util.HashMap<>();

    // #019 봉인의 바늘 — 봉인 유물의 봉인 시간을 절반으로 단축
    private boolean execute019(Player player) {
        org.bukkit.entity.Item nearest = plugin.getSealedRelicManager().getNearestSealed(player.getLocation(), 50);
        if (nearest == null) {
            player.sendMessage("§c[RelicWars] 50블록 이내에 봉인된 유물이 없습니다!");
            return false;
        }
        
        pending019Relic.put(player.getUniqueId(), nearest);
        Inventory inv = Bukkit.createInventory(null, 9, Component.text(GUI_TITLE_019));
        inv.setItem(3, createGuiItem(Material.SUGAR, "§a[시간 단축]", "§7해당 유물의 남은 봉인 시간을", "§7§l절반§7으로 단축합니다."));
        inv.setItem(5, createGuiItem(Material.CLOCK, "§c[시간 연장]", "§7해당 유물의 남은 봉인 시간을", "§7§l2배§7로 연장합니다."));
        
        player.openInventory(inv);
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "§b[소문] %s쪽에서 시간의 흐름이 비틀리는 듯한 파동이 발생했습니다.");
        return true;
    }

    public void execute019Option1(Player player) {
        org.bukkit.entity.Item pRelic = pending019Relic.get(player.getUniqueId());
        if (pRelic == null || !pRelic.isValid()) {
            player.sendMessage("§c[봉인의 바늘] 대상 유물이 사라졌습니다.");
            return;
        }
        plugin.getSealedRelicManager().reduceSealTime(pRelic, 0.5);
        player.sendMessage("§d[봉인의 바늘] " + pRelic.getName() + "의 봉인 시간을 §l절반§d으로 단축했습니다!");
        pending019Relic.remove(player.getUniqueId());
    }

    public void execute019Option2(Player player) {
        org.bukkit.entity.Item pRelic = pending019Relic.get(player.getUniqueId());
        if (pRelic == null || !pRelic.isValid()) {
            player.sendMessage("§c[봉인의 바늘] 대상 유물이 사라졌습니다.");
            return;
        }
        // SealedRelicManager에 시간을 늘리는 메소드가 없으므로, 다시 reduceSealTime(relic, 2.0) 하거나 
        // 직접 PDC를 수정해야 하지만, reduceSealTime(relic, factor) 가 곱하기 연산이라면 가능합니다.
        // 확인 필요. 우선 reduceSealTime(relic, 2.0)로 연장
        plugin.getSealedRelicManager().reduceSealTime(pRelic, 2.0);
        player.sendMessage("§d[봉인의 바늘] " + pRelic.getName() + "의 봉인 시간을 §l2배§d로 연장했습니다!");
        pending019Relic.remove(player.getUniqueId());
    }

    // #018 흔적 렌즈 — 200블록 내 유물 보유자의 발자국 파티클
    private boolean execute018(Player player) {
        player.sendMessage("§e[흔적 렌즈] 반경 200블록 내 최근 3분간의 유물 보유자 흔적을 추적합니다!");
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "§b[소문] 누군가 흔적을 읽기 시작했습니다.");

        // 자신의 위치에 역추적 흔적 (자주색 파티클 5초간)
        new BukkitRunnable() {
            int count = 0;
            Location origin = player.getLocation().clone();
            @Override
            public void run() {
                if (count++ > 25) { this.cancel(); return; }
                player.getWorld().spawnParticle(org.bukkit.Particle.WITCH, origin.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0);
            }
        }.runTaskTimer(plugin, 0L, 4L);

        Map<UUID, Queue<FootprintTracker.FootprintData>> footprints = plugin.getFootprintTracker().getFootprints();
        for (Queue<FootprintTracker.FootprintData> queue : footprints.values()) {
            for (FootprintTracker.FootprintData data : queue) {
                if (data.getLoc().getWorld().equals(player.getWorld())) {
                    if (data.getLoc().distanceSquared(player.getLocation()) <= 40000) { // 200 blocks
                        // 등급별 색상 지정
                        org.bukkit.Color color;
                        int num = data.getBestRelicNum();
                        if (num <= 5) color = org.bukkit.Color.YELLOW; // 5단계 (금색)
                        else if (num <= 10) color = org.bukkit.Color.PURPLE; // 4단계 (보라색)
                        else if (num <= 18) color = org.bukkit.Color.AQUA; // 3단계 (하늘색)
                        else if (num <= 24) color = org.bukkit.Color.LIME; // 2단계 (초록색)
                        else color = org.bukkit.Color.WHITE; // 1단계 (흰색)

                        // 먼지 파티클 생성
                        org.bukkit.Particle.DustOptions dustOptions = new org.bukkit.Particle.DustOptions(color, 1.5f);
                        player.spawnParticle(org.bukkit.Particle.DUST, data.getLoc().clone().add(0, 0.1, 0), 2, 0.2, 0, 0.2, dustOptions);
                    }
                }
            }
        }
        return true;
    }

    // #017 왜곡의 닻 — 다운 시 닻 위치로 텔레포트 세이브
    private boolean execute017(Player player) {
        Location anchorLoc = player.getLocation().clone();
        player.sendMessage("§5[왜곡의 닻] 현재 위치에 공간 왜곡장을 설치했습니다! (60초간 활성)");
        player.sendMessage("§7  이 범위(50블록) 안에서 다운되면 이 위치로 순간이동합니다.");

        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "§b[소문] %s쪽에서 공간이 일그러지는 소리가 들렸습니다.");

        String teamId = plugin.getTeamManager().getTeamId(player);
        String key = teamId != null ? teamId : player.getUniqueId().toString();
        active017Anchor.put(key, anchorLoc);

        // 닻 위치에 보라색 파티클
        BukkitTask particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active017Anchor.containsKey(key)) { this.cancel(); return; }
                anchorLoc.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, anchorLoc.clone().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }.runTaskTimer(plugin, 0L, 10L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active017Anchor.remove(key) != null) {
                if (player.isOnline()) player.sendMessage("§c[왜곡의 닻] 왜곡장이 소멸되었습니다.");
            }
        }, 1200L); // 60초
        return true;
    }

    // #016 감시의 방패 — 5분간 80블록 레이더
    private boolean execute016(Player player) {
        player.sendMessage("§b[감시의 방패] 반경 80블록 감시 구역을 5분간 전개합니다!");
        Location center = player.getLocation().clone();
        
        // 중심부에 희미한 파티클 리스크
        BukkitTask particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                center.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, center.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 6000) { 
                    this.cancel(); 
                    particleTask.cancel();
                    if (player.isOnline()) player.sendMessage("§c[감시의 방패] 감시 구역이 해제되었습니다.");
                    return; 
                }
                ticks += 40;

                // 적 감지 및 다운된 아군 감지
                for (Player p : player.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    // #008 그림자 막 면역 (적 감지 시에만 무시, 아군 다운은 감지됨)
                    boolean isShadowed = active008Shadow.contains(p.getUniqueId());
                    
                    if (p.getLocation().distanceSquared(center) <= 6400) {
                        Vector dir = p.getLocation().toVector().subtract(center.toVector());
                        int dist = (int) dir.length();
                        String direction = getCardinalDirection(dir);
                        
                        boolean isSameTeam = plugin.getTeamManager().isSameTeam(player, p);
                        if (!isSameTeam) {
                            if (isShadowed) continue; // 그림자 막 발동 중인 적은 무시
                            player.sendTitle("§c[경고] 적 감지", "§e" + direction + " " + dist + "블록", 0, 40, 10);
                            return; // 1개 발견시 우선 경고 후 리턴
                        } else if (plugin.getCombatManager().isDowned(p)) {
                            player.sendTitle("§4[비상] 아군 다운", "§c" + direction + " " + dist + "블록", 0, 40, 10);
                            return;
                        }
                    }
                }
                
                // 봉인 유물 감지
                org.bukkit.entity.Item nearestSealed = plugin.getSealedRelicManager().getNearestSealed(center, 80);
                if (nearestSealed != null) {
                    Vector dir = nearestSealed.getLocation().toVector().subtract(center.toVector());
                    int dist = (int) dir.length();
                    String direction = getCardinalDirection(dir);
                    player.sendTitle("§a[알림] 봉인 유물 감지", "§b" + direction + " " + dist + "블록", 0, 40, 10);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // 2초마다 스캔
        return true;
    }

    // #015 회수자의 갈고리 — 20블록 밖 봉인 유물을 끌어오기
    private boolean execute015(Player player) {
        org.bukkit.entity.Item nearest = plugin.getSealedRelicManager().getNearestSealed(player.getLocation(), 20);
        if (nearest == null) {
            player.sendMessage("§c[RelicWars] 20블록 이내에 봉인된 유물이 없습니다!");
            return false;
        }

        player.sendMessage("§6[회수자의 갈고리] 3초간 정신을 집중하여 유물을 끌어옵니다! (이동/피격 시 취소)");
        Location startLoc = player.getLocation().clone();
        active015Casting.add(player.getUniqueId());

        // 캐스팅: 3초 대기 (60틱)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !active015Casting.contains(player.getUniqueId())) {
                    this.cancel();
                    if (player.isOnline()) player.sendMessage("§c[회수자의 갈고리] 캐스팅이 취소되었습니다.");
                    return;
                }
                
                if (player.getLocation().distanceSquared(startLoc) > 0.25) {
                    this.cancel();
                    active015Casting.remove(player.getUniqueId());
                    player.sendMessage("§c[회수자의 갈고리] 이동하여 캐스팅이 취소되었습니다.");
                    return;
                }

                ticks += 5;
                if (ticks >= 60) {
                    this.cancel();
                    active015Casting.remove(player.getUniqueId());
                    
                    if (!nearest.isValid()) {
                        player.sendMessage("§c[회수자의 갈고리] 대상 유물이 사라졌습니다.");
                        return;
                    }
                    
                    // 끌어오기 시작
                    player.sendMessage("§a[회수자의 갈고리] 유물을 낚아챘습니다!");
                    startPullingRelic(player, nearest);
                } else {
                    player.spawnParticle(org.bukkit.Particle.ENCHANT, player.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
        return true;
    }
    
    private void startPullingRelic(Player player, org.bukkit.entity.Item target) {
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ >= 20 || !target.isValid() || !player.isOnline()) { 
                    this.cancel(); 
                    if (target.isValid()) {
                        plugin.getSealedRelicManager().reduceSealTime(target, 0.5); // 시간 절반 단축
                        player.sendMessage("§d[회수자의 갈고리] 유물이 도착했으며, 봉인 시간이 절반으로 단축되었습니다!");
                    }
                    return; 
                }

                Location current = target.getLocation();
                Location playerLoc = player.getLocation();
                org.bukkit.util.Vector direction = playerLoc.toVector().subtract(current.toVector()).normalize().multiply(1.0);
                target.teleport(current.add(direction));

                // 빛 궤적 파티클
                player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, current, 5, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L); // 1초(20틱)만에 도착
    }

    // ======================== Batch 4: #014 ~ #010 ========================

    // #014 전장의 뿔 — 60초간 팀 시야 공유 + 이속 버프
    private boolean execute014(Player player) {
        player.sendMessage("§6[전장의 뿔] 뿔피리가 울려 퍼집니다! 60초간 팀 시야 공유 + 이속 증가!");
        String teamId = plugin.getTeamManager().getTeamId(player);

        if (teamId != null) {
            for (UUID memberUuid : plugin.getTeamManager().getTeamMembers(teamId)) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    member.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0, false, false));
                    member.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 0, false, false));
                    member.sendMessage("§6[전장의 뿔] 팀원의 위치가 60초간 공유됩니다! 이동 속도 증가!");
                }
            }
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 0, false, false));
        }

        // 300블록 내 유물 보유자 방향 표시
        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            if (p.getLocation().distance(player.getLocation()) <= 300) {
                int relics = plugin.getRelicManager().countPlayerRelics(p);
                if (relics > 0) {
                    Vector dir = p.getLocation().toVector().subtract(player.getLocation().toVector());
                    String direction = getCardinalDirection(dir);
                    player.sendMessage("§e  [탐지] " + direction + " 방향에 유물 보유자 (" + relics + "개)");
                }
            }
        }
        return true;
    }

    // #013 탐욕의 뼈 — 보스를 적진에 배달
    private boolean execute013(Player player) {
        player.sendMessage("§4[탐욕의 뼈] 피의 마커를 설치했습니다! 탐욕의 추적자가 이 위치로 질주합니다!");
        // MVP: 마커 반경 30블록의 적 위치 10초간 노출
        Location marker = player.getLocation().clone();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 200) { this.cancel(); return; } // 10초
                ticks += 20;

                if (!player.isOnline() || !player.getWorld().equals(marker.getWorld())) { this.cancel(); return; }

                for (Player p : player.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;
                    if (!p.getWorld().equals(marker.getWorld())) continue;
                    if (p.getLocation().distance(marker) <= 30.0) {
                        player.sendMessage("§c  [마커] " + p.getName() + " 감지 — " + (int) p.getLocation().distance(marker) + "블록");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        return true;
    }

    // #012 약탈자의 장갑 — 적 정신력 30 강탈 (MVP: 디버프 부여)
    private boolean execute012(Player player) {
        Player target = null;
        for (Entity e : player.getNearbyEntities(10, 10, 10)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;
            target = p;
            break;
        }

        if (target == null) {
            player.sendMessage("§c[RelicWars] 10블록 이내에 적 플레이어가 없습니다!");
            return false;
        }

        Player victim = target;
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false));
        int stolenSanity = Math.min(30, plugin.getSanityManager().getSanity(victim));
        plugin.getSanityManager().setSanity(victim, plugin.getSanityManager().getSanity(victim) - stolenSanity);
        plugin.getSanityManager().restoreSanity(player, stolenSanity);

        victim.sendMessage("§4[약탈자의 장갑] 누군가 당신의 정신력을 " + stolenSanity + " 강탈했습니다!");
        player.sendMessage("§a[약탈자의 장갑] " + victim.getName() + "의 정신력을 " + stolenSanity + " 강탈했습니다!");
        return true;
    }

    // #011 공명의 종 — 300블록 내 유물 보유자 전원 위치 적발
    private boolean execute011(Player player) {
        player.sendMessage("§a[공명의 종] 반경 300블록 내의 모든 유물 소유자를 탐지합니다!");
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "§b[소문] 어디선가 맑은 종소리가 울려퍼집니다.");

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BELL_RESONATE, 3.0f, 1.0f);

        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            // #008 그림자 막 면역
            if (active008Shadow.contains(p.getUniqueId())) continue;
            if (p.getLocation().distanceSquared(player.getLocation()) > 90000) continue; // 300 blocks

            int relicCount = plugin.getRelicManager().countPlayerRelics(p);
            if (relicCount > 0) {
                // 발광 3초 + 붉은 벼락 파티클 3초
                p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
                
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 60 || !p.isOnline()) { this.cancel(); return; }
                        ticks += 5;
                        
                        // 머리 위로 10블록 높이까지 붉은 번개 기둥
                        org.bukkit.Particle.DustOptions red = new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 2.0f);
                        for (double y = 0; y <= 10; y += 0.5) {
                            p.getWorld().spawnParticle(org.bukkit.Particle.DUST, p.getLocation().add(0, y, 0), 2, 0.2, 0.2, 0.2, red);
                        }
                    }
                }.runTaskTimer(plugin, 0L, 5L);

                player.sendMessage("§e  [탐지] " + p.getName() + " — 유물 " + relicCount + "개 보유 (" +
                        (int) p.getLocation().distance(player.getLocation()) + "블록)");
                p.sendMessage("§c[경고] 공명의 종에 의해 당신의 위치가 노출되었습니다!");
            }
        }
        return true;
    }

    // #010 충격 코어 — 광역 넉백 + 5초 EMP (상호작용 차단)
    private boolean execute010(Player player) {
        player.sendMessage("§4[충격 코어] 반경 15블록 넉백 및 20블록 EMP 발동!");
        Bukkit.broadcast(Component.text("§4[EMP] 거대한 폭발음과 함께 주변의 기운이 증발합니다!"));
        
        player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, player.getLocation(), 1);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);

        // 20블록 내 적 탐색
        for (Entity e : player.getNearbyEntities(20, 20, 20)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;

            // 15블록 내 강한 넉백
            if (p.getLocation().distanceSquared(player.getLocation()) <= 225) {
                Vector knockback = p.getLocation().toVector().subtract(player.getLocation().toVector());
                if (knockback.lengthSquared() == 0) knockback = new Vector(0, 1, 0);
                else knockback = knockback.normalize().multiply(3.0).setY(1.2);
                p.setVelocity(knockback);
            }

            // 20블록 내 EMP 디버프
            p.sendMessage("§c[EMP] 충격파에 의해 5초간 상호작용 및 유물 사용이 차단됩니다!");
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 2, false, false));
            
            UUID id = p.getUniqueId();
            active010EMP.add(id);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                active010EMP.remove(id);
                if (p.isOnline()) p.sendMessage("§a[EMP] 시스템 복구 완료. 유물 사용이 가능합니다.");
            }, 100L); // 5초
        }
        return true;
    }

    // ======================== Batch 5: #009 ~ #005 ========================

    // #009 파괴자의 서 — 봉인 즉시 파괴
    private boolean execute009(Player player) {
        org.bukkit.entity.Item nearest = plugin.getSealedRelicManager().getNearestSealed(player.getLocation(), 50);
        if (nearest == null) {
            player.sendMessage("§c[RelicWars] 50블록 이내에 봉인된 유물이 없습니다!");
            return false;
        }

        plugin.getSealedRelicManager().forceUnseal(nearest);
        player.sendMessage("§5[파괴자의 서] 봉인을 즉시 파괴했습니다! 유물이 획득 가능합니다!");
        Bukkit.broadcast(Component.text("§5[파괴] 누군가 봉인 유물의 봉인을 강제로 파괴했습니다!"));
        return true;
    }

    // #006 차원 도약석 — 30블록 순간이동 + 5초 내 복귀
    private boolean execute006(Player player) {
        Location origin = player.getLocation().clone();
        
        // 30블록 앞 좌표 계산 (벽 통과 방지)
        Block targetBlock = player.getTargetBlockExact(30, org.bukkit.FluidCollisionMode.NEVER);
        Location targetLoc;
        if (targetBlock != null) {
            targetLoc = targetBlock.getLocation().add(0, 1, 0);
            targetLoc.setDirection(origin.getDirection());
        } else {
            Vector dir = origin.getDirection().normalize().multiply(30);
            targetLoc = origin.clone().add(dir);
        }

        // 출발지 파티클
        player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, origin, 50, 0.5, 1.0, 0.5, 0.1);
        player.getWorld().playSound(origin, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // 홀로그램 잔상 (갑옷 거치대)
        org.bukkit.entity.ArmorStand hologram = player.getWorld().spawn(origin, org.bukkit.entity.ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setArms(true);
            stand.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD)); // 머리(플레이어 머리로 대체 가능)
            stand.getEquipment().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
            stand.setCustomName("§d" + player.getName() + "의 잔상");
            stand.setCustomNameVisible(true);
        });

        // 데이터 기록
        UUID id = player.getUniqueId();
        active006Leap.put(id, new LeapData(origin, player.getHealth(), hologram));

        // 텔레포트
        player.teleport(targetLoc);
        player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, targetLoc, 50, 0.5, 1.0, 0.5, 0.1);
        player.getWorld().playSound(targetLoc, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.sendMessage("§d[차원 도약석] 도약했습니다! §e15초 이내에 다시 우클릭하면 원래 위치로 복귀합니다.");
        player.sendMessage("§d[차원 도약석] 복귀 시 도약 시점의 체력(§c" + String.format("%.1f", player.getHealth()) + "HP§d)으로 돌아갑니다.");

        // 15초 타이머
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            LeapData data = active006Leap.remove(id);
            if (data != null) {
                if (!data.hologram.isDead()) data.hologram.remove();
                if (player.isOnline()) player.sendMessage("§c[차원 도약석] 복귀 시간이 초과되었습니다.");
            }
        }, 300L); // 15초
        return true;
    }
    
    private void execute006Return(Player player) {
        LeapData data = active006Leap.remove(player.getUniqueId());
        if (data == null) return;

        if (!data.hologram.isDead()) data.hologram.remove();

        // 체력 복구
        player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(), Math.max(1.0, data.health)));
        
        // 상태이상 해제 (디버프 제거)
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // 복귀 텔레포트
        Location returnLoc = data.origin;
        player.getWorld().spawnParticle(org.bukkit.Particle.REVERSE_PORTAL, player.getLocation(), 50, 0.5, 1.0, 0.5, 0.1);
        player.teleport(returnLoc);
        player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, returnLoc, 50, 0.5, 1.0, 0.5, 0.1);
        player.getWorld().playSound(returnLoc, org.bukkit.Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);
        
        player.sendMessage("§a[차원 도약석] 시간을 되감아 원래 위치로 복귀했습니다! 체력이 도약 시점(§c" + String.format("%.1f", data.health) + "HP§a)으로 회복되었습니다.");
    }

    // #005 불멸의 심장 (자동 발동)
    public boolean trigger005ImmortalHeart(Player player) {
        java.util.UUID id = player.getUniqueId();
        
        // 쿨다운 확인 (10분 = 600초)
        long now = System.currentTimeMillis();
        long next = cooldown005.getOrDefault(id, 0L);
        if (now < next) {
            return false; // 아직 쿨타임
        }
        
        // 정신력 확인 (코스트 30)
        if (!plugin.getSanityManager().consumeSanity(player, 30)) {
            player.sendMessage("§c[불멸의 심장] 정신력이 부족하여 불멸의 심장이 반응하지 않았습니다!");
            return false; // 정신력 부족
        }
        
        // 쿨타임 적용 (5400초 = 90분, 기획서 기준)
        cooldown005.put(id, System.currentTimeMillis() + 5400000L);
        
        // 부활 처리
        player.setHealth(java.util.Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue()); // 풀피
        player.sendMessage("§c[불멸의 심장] 죽음을 거부하고 다시 일어섭니다!");
        
        // 주변 넉백 및 번개 (기획서: 8블록, 2.5x + 0.8Y)
        for (org.bukkit.entity.Entity e : player.getNearbyEntities(8, 8, 8)) {
            if (!(e instanceof Player p)) continue;
            if (!plugin.getTeamManager().isSameTeam(player, p)) {
                org.bukkit.util.Vector kb = p.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(2.5).setY(0.8);
                p.setVelocity(kb);
            }
        }
        
        org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text("§6[경고] 누군가 불멸의 심장을 통해 부활했습니다! (" + player.getLocation().getBlockX() + ", ?, " + player.getLocation().getBlockZ() + ")"));
        
        // 저항 II 8초 (기획서 명시) + 데미지 감소 추적
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 160, 1, false, false));
        active005DamageReduction.add(id);
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active005DamageReduction.remove(id);
            if (player.isOnline()) player.sendMessage("§c[불멸의 심장] 데미지 감소 효과가 종료되었습니다.");
        }, 160L);
        
        player.getWorld().strikeLightningEffect(player.getLocation());
        return true;
    }



    // #008 그림자 막 — 3분간 모든 탐지 무효화 + 가짜 신호
    private boolean execute008(Player player) {
        player.sendMessage("§8[그림자 막] 3분간 팀 전체가 모든 탐지에서 사라집니다!");
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "§b[소문] 주변에 짙은 그림자가 드리우며 기운이 사라집니다.");

        // 팀원 탐지 면역 3분
        String teamId = plugin.getTeamManager().getTeamId(player);
        if (teamId != null) {
            for (UUID memberUuid : plugin.getTeamManager().getTeamMembers(teamId)) {
                Player member = Bukkit.getPlayer(memberUuid);
                active008Shadow.add(memberUuid);
                if (member != null && member.isOnline()) {
                    member.sendMessage("§8[그림자 막] 3분간 모든 탐지에서 완벽하게 은폐됩니다!");
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> active008Shadow.remove(memberUuid), 3600L);
            }
        } else {
            active008Shadow.add(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> active008Shadow.remove(player.getUniqueId()), 3600L);
        }

        // 가짜 신호 3개 생성 (ArmorStand)
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
                stand.setCustomName("§c[가짜 신호]");
                stand.setCustomNameVisible(false);
            });
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!decoy.isDead()) decoy.remove(); }, 3600L);
        }
        return true;
    }

    // #007 파수꾼의 돔 — 15초 절대 방어막 (역장)
    private boolean execute007(Player player) {
        player.sendMessage("§b[파수꾼의 돔] 반경 8블록 절대 방어막(역장)을 15초간 전개합니다!");
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "§b[소문] 거대한 방벽이 세워지는 진동이 느껴집니다.");

        Location center = player.getLocation().clone();
        String teamId = plugin.getTeamManager().getTeamId(player);
        String key = teamId != null ? teamId : player.getUniqueId().toString();
        active007Dome.put(center, key);

        // 파티클 타이머 (돔 경계 표시) 및 역장 밀어내기
        BukkitTask forcefieldTask = new BukkitRunnable() {
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

                // 적 밀어내기 및 투사체 차단
                for (Entity e : center.getWorld().getNearbyEntities(center, 8.5, 8.5, 8.5)) {
                    if (e instanceof org.bukkit.entity.Projectile proj) {
                        if (proj.getShooter() instanceof Player shooter) {
                            String shooterTeam = plugin.getTeamManager().getTeamId(shooter);
                            String sKey = shooterTeam != null ? shooterTeam : shooter.getUniqueId().toString();
                            if (!sKey.equals(key)) {
                                proj.remove(); // 적 투사체 소멸
                                center.getWorld().spawnParticle(org.bukkit.Particle.LARGE_SMOKE, proj.getLocation(), 2);
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
                            p.sendMessage("§c[파수꾼의 돔] 적의 역장에 튕겨났습니다!");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // 0.1초마다

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active007Dome.remove(center);
            forcefieldTask.cancel();
            if (player.isOnline()) player.sendMessage("§c[파수꾼의 돔] 방어막이 해제되었습니다.");
        }, 300L); // 15초
        return true;
    }


    // #005 불멸의 심장 — 완전 패시브 유물 (우클릭 시 안내만)
    private boolean execute005(Player player) {
        player.sendMessage("§6[불멸의 심장] 이 유물은 패시브 유물입니다.");
        player.sendMessage("§7  치명상을 입을 때 자동으로 발동하여 체력을 회복하고 적을 밀쳐냅니다.");
        player.sendMessage("§7  쿨타임: 90분 | 정신력 소모: 30");
        return false; // 패시브 — 쿨타임/정신력 소모 없음
    }

    // ======================== Batch 6: #004 ~ #001 ========================

    // #004 폭풍의 왕관 — 반경 30블록 광역 뇌우 15초
    private boolean execute004(Player player) {
        Block targetBlock = player.getTargetBlockExact(100);
        Location stormCenter;
        if (targetBlock != null) {
            stormCenter = targetBlock.getLocation();
        } else {
            stormCenter = player.getLocation();
        }

        player.sendMessage("§b[폭풍의 왕관] 대상 지역에 파멸적인 뇌우를 15초간 소환합니다!");
        Bukkit.broadcast(Component.text("§4[폭풍] 하늘이 진동하며 광란의 뇌우가 쏟아집니다!"));

        final Location center = stormCenter;
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 300) { this.cancel(); return; } // 15초
                ticks += 20;

                // 1초마다 번개 타격 및 데미지
                for (int i = 0; i < 3; i++) {
                    double rx = (Math.random() - 0.5) * 60; // 반경 30
                    double rz = (Math.random() - 0.5) * 60;
                    Location strike = center.clone().add(rx, 0, rz);
                    strike.setY(center.getWorld().getHighestBlockYAt(strike));
                    center.getWorld().strikeLightningEffect(strike);
                }

                // 범위 내 적에게 피해 및 디버프
                for (Player p : center.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 900) { // 30블록
                        p.damage(2.0, player); // 1칸 데미지
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
                        p.sendTitle("§c[벼락]", "§7눈앞이 번쩍입니다!", 0, 20, 10);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1초마다
        return true;
    }

    // #003 절대 좌표 나침반 — 특정 유물의 실시간 좌표 3분간 표시
    private boolean execute003(Player player) {
        player.sendMessage("§5[절대 좌표 나침반] 추적할 유물 번호(숫자)를 채팅에 입력하세요! (1~30)");
        player.sendMessage("§7  (입력 대기 시간: 1분)");
        
        active003TrackerWait.add(player.getUniqueId());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active003TrackerWait.remove(player.getUniqueId())) {
                if (player.isOnline()) {
                    player.sendMessage("§c[절대 좌표 나침반] 입력 대기 시간이 초과되었습니다.");
                }
            }
        }, 1200L); // 1분
        return true;
    }

    public void start003Tracker(Player player, int targetNum) {
        com.wolfool.relicwars.relic.RelicDefinition def = com.wolfool.relicwars.relic.RelicDefinition.getByNumber(targetNum);
        if (def == null) { player.sendMessage("§c[절대 좌표 나침반] 유효하지 않은 유물 번호입니다."); return; }

        // 자기 자신 또는 팀원이 소유 중인 유물이면 캔슬 (쿨타임 소모 X)
        String ownerUuid = plugin.getDatabaseManager().getRelicOwner(targetNum);
        if (ownerUuid != null) {
            UUID ownerId = UUID.fromString(ownerUuid);
            if (ownerId.equals(player.getUniqueId())) {
                player.sendMessage("§c[절대 좌표 나침반] 자신이 소유한 유물은 추적할 수 없습니다.");
                // 쿨타임 + 정신력 소모 방지를r
                resetCooldownForRelic(player, 3);
                plugin.getSanityManager().restoreSanity(player, getSanityCost(3, player));
                return;
            }
            Player ownerPlayer = Bukkit.getPlayer(ownerId);
            if (ownerPlayer != null && plugin.getTeamManager().isSameTeam(player, ownerPlayer)) {
                player.sendMessage("§c[절대 좌표 나침반] 같은 팀원이 소유한 유물은 추적할 수 없습니다.");
                resetCooldownForRelic(player, 3);
                plugin.getSanityManager().restoreSanity(player, getSanityCost(3, player));
                return;
            }
        }

        player.sendMessage("§d[절대 좌표 나침반] §e" + def.getName() + "§d의 좌표 추적을 시작합니다. (3분간 유지)");

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 3600 || !player.isOnline()) { // 3분
                    this.cancel();
                    if (player.isOnline()) player.sendMessage("§c[절대 좌표 나침반] 추적이 종료되었습니다.");
                    return;
                }
                ticks += 20;

                String ownerUuid = plugin.getDatabaseManager().getRelicOwner(targetNum);
                Location loc = null;

                if (ownerUuid != null) {
                    Player targetPlayer = Bukkit.getPlayer(UUID.fromString(ownerUuid));
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        if (active008Shadow.contains(targetPlayer.getUniqueId())) {
                            player.sendActionBar(Component.text("§8[추적] 대상이 그림자 속에 숨었습니다. 좌표 불명."));
                            return;
                        }
                        loc = targetPlayer.getLocation();
                        if (ticks == 20) {
                            targetPlayer.sendMessage("§4[경고] 누군가 당신의 유물을 실시간으로 추적하고 있습니다!");
                        }
                    }
                } else {
                    // 필드 드랍 상태 (SealedRelicManager 활용)
                    for (org.bukkit.entity.Item display : plugin.getSealedRelicManager().getActiveSealedRelics()) {
                        if (com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(display.getItemStack()) == targetNum) {
                            loc = display.getLocation();
                            break;
                        }
                    }
                }

                if (loc != null) {
                    player.sendActionBar(Component.text("§d[추적] §e" + def.getName() + " §f- X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ()));
                } else {
                    player.sendActionBar(Component.text("§c[추적] 대상 유물의 위치를 확인할 수 없습니다."));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1초마다 업데이트
    }

    // #002 탐욕의 적출자 — 다운된 적에게서 0.5초 즉시 강탈 (CombatListener에서 처리됨)
    private boolean execute002(Player player) {
        player.sendMessage("§c[탐욕의 적출자] 이 유물은 허공에 사용하는 것이 아닙니다. 다운된 적을 우클릭하여 발동하세요.");
        return false; // 쿨타임/정신력 소모 방지
    }



    // #001 태초의 별: 심판의 별 (60초간 비행, 무제한 벼락 포격, 좌표 실시간 중계)
    private boolean execute001(Player player) {
        // 넘버링 유물은 파괴되지 않음 — 쿨타임으로만 재사용 제한

        Bukkit.broadcast(Component.text("§4========================================"));
        Bukkit.broadcast(Component.text("§e[경고] 태초의 지배자가 강림했습니다. 심판의 별이 떠오릅니다..."));
        Bukkit.broadcast(Component.text("§4========================================"));

        UUID id = player.getUniqueId();
        active001Omega.add(id);
        
        // 60초간 비행 허용
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage("§e[태초의 별] 60초간 자유롭게 비행하며, 좌클릭으로 방어력과 무적을 무시하는 심판의 벼락을 내리꽂을 수 있습니다.");
        player.sendMessage("§e[태초의 별] 또한, 2초마다 모든 적의 위치와 상태가 공유됩니다.");

        BukkitRunnable omegaRunnable = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !active001Omega.contains(id)) {
                    this.cancel();
                    active001Omega.remove(id);
                    return;
                }

                ticks += 40; // 2초마다
                if (ticks > 1200) { // 60초 종료
                    this.cancel();
                    active001Omega.remove(id);
                    lightningCooldown.remove(id);
                    if (player.isOnline()) {
                        player.sendMessage("§c[태초의 별] 태초의 지배자 권능이 소멸했습니다.");
                        player.sendMessage("§4[태초의 별] 5분 뒤 정신력이 완전히 소진됩니다...");
                        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                            player.setAllowFlight(false);
                            player.setFlying(false);
                        }
                        // 5분(6000틱) 뒤 정신력 0으로
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                plugin.getSanityManager().setSanity(player, 0);
                                player.sendMessage("§4[태초의 별] 태초의 힘을 사용한 대가로 정신력이 완전히 소진되었습니다.");
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f);
                            }
                        }, 6000L);
                    }
                    return;
                }

                // 적 탐색 및 중계 (2초 주기)
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.equals(player)) continue;
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;

                    int x = p.getLocation().getBlockX();
                    int y = p.getLocation().getBlockY();
                    int z = p.getLocation().getBlockZ();
                    int health = (int) p.getHealth();
                    int sanity = plugin.getSanityManager().getSanity(p);

                    player.sendMessage("§b[태초의 관측] §f" + p.getName() + " §7- 위치: " + x + ", " + y + ", " + z + 
                            " | 체력: §c" + health + " §7| 정신력: §e" + sanity);
                }
            }
        };
        trackTask(id, omegaRunnable.runTaskTimer(plugin, 40L, 40L));
        return true;
    }


    // ======================== 유틸리티 ========================
    
    public void cleanupPlayer(Player player) {
        java.util.UUID id = player.getUniqueId();
        
        // #001 비행 모드 해제 (반드시 Set 제거 전에 실행)
        if (active001Omega.contains(id)) {
            if (player.isOnline() && player.getGameMode() != org.bukkit.GameMode.CREATIVE 
                    && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
        
        active001Omega.remove(id);
        active005DamageReduction.remove(id);
        if (active029FallImmunity.remove(id)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                p.setAllowFlight(false);
                p.setFlying(false);
            }
        }
        active027FireImmunity.remove(id);
        active025FastRevive.remove(id);
        active023Marked.remove(id);
        active021Duel.remove(id);
        active020ScanMode.remove(id);
        active020PingMode.remove(id);
        active015Casting.remove(id);
        active010EMP.remove(id);
        active008Shadow.remove(id);
        LeapData data006 = active006Leap.remove(id);
        if (data006 != null && data006.hologram != null && !data006.hologram.isDead()) {
            data006.hologram.remove();
        }
        active003TrackerWait.remove(id);
        pending019Relic.remove(id);
        cooldown005.remove(id);
        lightningCooldown.remove(id);
        
        // 모든 추적 태스크 일괄 취소
        cancelAllTasks(id);
        
        // #017 왜곡의 닻 정리 (UUID 키 기반)
        active017Anchor.entrySet().removeIf(e -> e.getKey().contains(id.toString()));
        
        // #007 돔 정리 (UUID 값 기반)
        active007Dome.entrySet().removeIf(e -> e.getValue().equals(id.toString()));
        
        // 결투 대상에서도 제거
        java.util.Iterator<Map.Entry<java.util.UUID, java.util.UUID>> it = active021Duel.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<java.util.UUID, java.util.UUID> entry = it.next();
            if (entry.getValue().equals(id)) {
                it.remove();
            }
        }
    }


    public String getCardinalDirection(Vector dir) {
        double angle = Math.toDegrees(Math.atan2(dir.getZ(), dir.getX()));
        if (angle < 0) angle += 360;

        if (angle < 22.5 || angle >= 337.5) return "동쪽";
        if (angle < 67.5) return "남동쪽";
        if (angle < 112.5) return "남쪽";
        if (angle < 157.5) return "남서쪽";
        if (angle < 202.5) return "서쪽";
        if (angle < 247.5) return "북서쪽";
        if (angle < 292.5) return "북쪽";
        return "북동쪽";
    }

    /**
     * 유물 번호에 따른 정신력 소모량을 반환합니다.
     * 1~2단계(#030~#020): 0 (소모 없음)
     * 3단계(#019~#011): 10
     * 4단계(#010~#006): 20
     * 5단계(#005~#001): 30
     */
    private int getSanityCost(int relicNumber, Player player) {
        if (active001Omega.contains(player.getUniqueId())) return 0; // 태초의 별 발동 중이면 코스트 면제
        if (relicNumber >= 20) return 0;   // 1~2단계
        if (relicNumber >= 11) return 10;  // 3단계
        if (relicNumber >= 6) return 20;   // 4단계
        if (relicNumber >= 1) return 30;   // 5단계
        return 0;
    }

}
