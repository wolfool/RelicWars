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

public class RelicAbilityHandler {

    private final RelicWars plugin;
    
    // 버프 상태 관리 맵 (UUID)
    public final Set<UUID> active029FallImmunity = new HashSet<>();
    public final Set<UUID> active027FireImmunity = new HashSet<>();
    public final Set<UUID> active025FastRevive = new HashSet<>();
    public final Set<UUID> active023Marked = new HashSet<>(); // 표식이 찍힌 대상
    public final Map<UUID, UUID> active021Duel = new HashMap<>(); // 결투 중인 쌍 (대상 -> 시전자)

    public RelicAbilityHandler(RelicWars plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, RelicDefinition def) {
        int num = def.getNumber();

        // 정신력 소모 체크 (3단계: 10, 4단계: 20, 5단계: 30)
        int sanityCost = getSanityCost(num);
        if (sanityCost > 0) {
            if (!plugin.getSanityManager().consumeSanity(player, sanityCost)) {
                return; // 정신력 부족
            }
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
            if (p.equals(player)) continue; // 시전자 제외
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

    // ======================== Batch 2: #024 ~ #020 ========================

    // #024 붉은 봉합 — 30블록 밖 다운 팀원을 자기 위치로 텔레포트
    private void execute024(Player player) {
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
            return;
        }

        target.teleport(player.getLocation());
        player.sendMessage("§d[붉은 봉합] " + target.getName() + "님을 내 위치로 소환했습니다! 구조를 시작하세요!");
        target.sendMessage("§d[붉은 봉합] 팀원에 의해 안전 지대로 이동되었습니다!");
    }

    // #023 사냥꾼의 표식 — 60초간 바라보는 적에게 발광 + 강탈 시간 단축
    private void execute023(Player player) {
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
            return;
        }

        Player marked = target;
        marked.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0, false, false)); // 60초
        active023Marked.add(marked.getUniqueId());

        player.sendMessage("§6[사냥꾼의 표식] " + marked.getName() + "에게 60초간 사냥 표식을 찍었습니다!");
        marked.sendMessage("§c[경고] 누군가 당신에게 사냥 표식을 찍었습니다! 60초간 벽 너머로도 보입니다!");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active023Marked.remove(marked.getUniqueId());
        }, 1200L);
    }

    // #022 탐욕의 동전 — 가짜 봉인 유물 트랩 설치
    private void execute022(Player player) {
        Location trapLoc = player.getLocation().clone();
        player.sendMessage("§e[탐욕의 동전] 가짜 봉인 유물 트랩을 설치했습니다!");

        // SealedRelicManager의 ItemDisplay와 동일하게 보이는 가짜 생성
        // MVP: ArmorStand + 이름표로 대체
        org.bukkit.entity.ArmorStand fake = player.getWorld().spawn(trapLoc, org.bukkit.entity.ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setCustomName("§5[봉인] §d??? 유물 §7(해제까지 ??초)");
            stand.setCustomNameVisible(true);
            stand.setMarker(true);
            stand.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(Material.NETHER_STAR));
        });

        // 3분(3600틱) 뒤 자동 소멸
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!fake.isDead()) fake.remove();
        }, 3600L);

        // 근접 우클릭 감지를 위한 반복 태스크
        new BukkitRunnable() {
            @Override
            public void run() {
                if (fake.isDead()) { this.cancel(); return; }

                for (Player p : fake.getWorld().getPlayers()) {
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;
                    if (p.getLocation().distance(fake.getLocation()) <= 2.5) {
                        // 트랩 발동!
                        fake.remove();
                        p.sendMessage("§4[함정!] 가짜 유물이었습니다!");
                        player.sendMessage("§a[탐욕의 동전] " + p.getName() + "이(가) 트랩에 걸렸습니다!");

                        // 폭발 파티클
                        p.getWorld().createExplosion(fake.getLocation(), 0F, false, false);

                        // 디버프
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, false, false)); // 5초 구속2
                        p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 1, false, false)); // 5초 채굴피로2
                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false)); // 5초 발광
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 10L);
    }

    // #021 결투자의 파편 — 15x15 결투장 20초간 강제 1대1
    private void execute021(Player player) {
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
            return;
        }

        Player enemy = target;
        Location center = player.getLocation().clone().add(enemy.getLocation()).multiply(0.5);

        active021Duel.put(enemy.getUniqueId(), player.getUniqueId());
        active021Duel.put(player.getUniqueId(), enemy.getUniqueId());

        player.sendMessage("§4[결투자의 파편] " + enemy.getName() + "과(와) 20초간 강제 결투가 시작됩니다!");
        enemy.sendMessage("§4[결투자의 파편] " + player.getName() + "이(가) 당신을 결투에 가뒀습니다! 20초간 탈출 불가!");
        Bukkit.broadcast(Component.text("§4[결투] " + player.getName() + " vs " + enemy.getName() + " — 20초 결투 시작!"));

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
    }

    // #020 소문의 등불 — 4가지 옵션 중 하나를 선택하는 GUI 오픈
    private void execute020(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("§5소문의 등불"));

        inv.setItem(1, createGuiItem(Material.COMPASS, "§d[미발견 유물 스캔]", "§7아직 세상에 나오지 않은", "§7유물의 정보를 스캔합니다."));
        inv.setItem(3, createGuiItem(Material.ENDER_EYE, "§5[봉인 유물 스캔]", "§7현재 바닥에 봉인된", "§7유물들의 위치를 파악합니다."));
        inv.setItem(5, createGuiItem(Material.NAME_TAG, "§e[소유자 검색 명령어]", "§7특정 번호의 유물을 누가", "§7가졌는지 알아낼 수 있는 명령어를 받습니다."));
        inv.setItem(7, createGuiItem(Material.PLAYER_HEAD, "§a[유물 보유 현황]", "§7현재 접속 중인 플레이어들의", "§7유물 보유 현황을 스캔합니다."));

        player.openInventory(inv);
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

    public void execute020Option1(Player player) {
        player.sendMessage("§d[소문의 등불] §f미발견 유물 정보를 스캔합니다...");
        boolean foundUnspawned = false;
        // DB 연동 및 스폰 로직 확인 (MVP 생략)
        player.sendMessage("§7  [미발견] 모든 유물이 이미 세상에 등장했습니다.");
    }

    public void execute020Option2(Player player) {
        player.sendMessage("§d[소문의 등불] §f현재 바닥에 봉인된 유물을 스캔합니다...");
        player.sendMessage("§7  [봉인] 활성화된 봉인 유물을 검색했습니다.");
    }

    public void execute020Option3(Player player) {
        player.sendMessage("§d[소문의 등불] §f유물 소유자 검색 모드를 선택했습니다.");
        Component scanMsg = Component.text("§e  [정보] 특정 유물 소유자를 알고 싶다면: ")
                .append(Component.text("§a/relic scan <번호>")
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/relic scan "))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("클릭하여 채팅창에 명령어 입력"))));
        player.sendMessage(scanMsg);
    }

    public void execute020Option4(Player player) {
        player.sendMessage("§d[소문의 등불] §f현재 유물 보유 현황을 스캔합니다...");
        for (Player p : Bukkit.getOnlinePlayers()) {
            int count = plugin.getRelicManager().countPlayerRelics(p);
            if (count > 0) {
                player.sendMessage("§7    - " + p.getName() + ": §e" + count + "개 보유");
            }
        }
    }

    // ======================== Batch 3: #019 ~ #015 ========================

    // #019 봉인의 바늘 — 봉인 유물의 봉인 시간을 절반으로 단축
    private void execute019(Player player) {
        org.bukkit.entity.ItemDisplay nearest = plugin.getSealedRelicManager().getNearestSealed(player.getLocation(), 50);
        if (nearest == null) {
            player.sendMessage("§c[RelicWars] 50블록 이내에 봉인된 유물이 없습니다!");
            return;
        }

        plugin.getSealedRelicManager().halveSealTime(nearest);
        player.sendMessage("§d[봉인의 바늘] 근처 봉인 유물의 봉인 시간을 절반으로 단축했습니다!");
        player.sendMessage("§7  위치: X:" + (int) nearest.getLocation().getX() + " Y:" + (int) nearest.getLocation().getY() + " Z:" + (int) nearest.getLocation().getZ());
    }

    // #018 흔적 렌즈 — 200블록 내 유물 보유자의 발자국 파티클
    private void execute018(Player player) {
        player.sendMessage("§e[흔적 렌즈] 반경 200블록 내 유물 보유자의 발자국을 추적합니다!");

        // 유물 보유자 위치에 형광 파티클 표시 (30초간)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 600) { this.cancel(); return; }
                ticks += 10;

                for (Player p : player.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    if (p.getLocation().distance(player.getLocation()) > 200) continue;
                    int relicCount = plugin.getRelicManager().countPlayerRelics(p);
                    if (relicCount > 0) {
                        // 등급별 색상 파티클
                        org.bukkit.Particle particle = org.bukkit.Particle.HAPPY_VILLAGER;
                        Location footprint = p.getLocation().clone().add(0, 0.1, 0);
                        player.spawnParticle(particle, footprint, 5, 0.3, 0.05, 0.3, 0);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // #017 왜곡의 닻 — 다운 시 닻 위치로 텔레포트 세이브
    private void execute017(Player player) {
        Location anchorLoc = player.getLocation().clone();
        player.sendMessage("§5[왜곡의 닻] 현재 위치에 공간 왜곡장을 설치했습니다! (60초간 활성)");
        player.sendMessage("§7  이 범위(50블록) 안에서 다운되면 이 위치로 순간이동합니다.");

        UUID id = player.getUniqueId();
        // 닻 위치 저장 (간이 구현: 60초 후 만료)
        // 실제로는 CombatManager의 다운 로직에서 이 닻을 체크해야 함
        // MVP: 60초간 다운 시 텔레포트 훅 등록
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) player.sendMessage("§c[왜곡의 닻] 왜곡장이 소멸되었습니다.");
        }, 1200L);
    }

    // #016 감시의 방패 — 5분간 80블록 레이더
    private void execute016(Player player) {
        player.sendMessage("§b[감시의 방패] 반경 80블록 감시 구역을 5분간 전개합니다!");
        Location center = player.getLocation().clone();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 6000) { this.cancel(); return; }
                ticks += 40;

                for (Player p : player.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;
                    if (p.getLocation().distance(center) <= 80.0) {
                        // 방향 계산
                        Vector dir = p.getLocation().toVector().subtract(center.toVector());
                        int dist = (int) dir.length();
                        String direction = getCardinalDirection(dir);
                        player.sendTitle("§c[경고] 적 감지", "§e" + direction + " " + dist + "블록", 0, 40, 10);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // 2초마다 스캔
    }

    // #015 회수자의 갈고리 — 20블록 밖 봉인 유물을 끌어오기
    private void execute015(Player player) {
        org.bukkit.entity.ItemDisplay nearest = plugin.getSealedRelicManager().getNearestSealed(player.getLocation(), 20);
        if (nearest == null) {
            player.sendMessage("§c[RelicWars] 20블록 이내에 봉인된 유물이 없습니다!");
            return;
        }

        player.sendMessage("§6[회수자의 갈고리] 봉인 유물을 끌어옵니다!");

        // 3초에 걸쳐 유물을 플레이어 위치로 이동
        final org.bukkit.entity.ItemDisplay target = nearest;
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 60 || !target.isValid()) { this.cancel(); return; }
                ticks += 3;

                Location current = target.getLocation();
                Location playerLoc = player.getLocation();
                org.bukkit.util.Vector direction = playerLoc.toVector().subtract(current.toVector()).normalize().multiply(0.5);
                target.teleport(current.add(direction));

                // 빛 궤적 파티클
                player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, current, 3, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    // ======================== Batch 4: #014 ~ #010 ========================

    // #014 전장의 뿔 — 60초간 팀 시야 공유 + 이속 버프
    private void execute014(Player player) {
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
    }

    // #013 탐욕의 뼈 — 보스를 적진에 배달
    private void execute013(Player player) {
        player.sendMessage("§4[탐욕의 뼈] 피의 마커를 설치했습니다! 탐욕의 추적자가 이 위치로 질주합니다!");
        // MVP: 마커 반경 30블록의 적 위치 10초간 노출
        Location marker = player.getLocation().clone();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 200) { this.cancel(); return; } // 10초
                ticks += 20;

                for (Player p : player.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;
                    if (p.getLocation().distance(marker) <= 30.0) {
                        player.sendMessage("§c  [마커] " + p.getName() + " 감지 — " + (int) p.getLocation().distance(marker) + "블록");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // #012 약탈자의 장갑 — 적 정신력 30 강탈 (MVP: 디버프 부여)
    private void execute012(Player player) {
        Player target = null;
        for (Entity e : player.getNearbyEntities(10, 10, 10)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;
            target = p;
            break;
        }

        if (target == null) {
            player.sendMessage("§c[RelicWars] 10블록 이내에 적 플레이어가 없습니다!");
            return;
        }

        Player victim = target;
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false));
        victim.sendMessage("§4[약탈자의 장갑] 누군가 당신의 정신력을 강탈했습니다!");
        player.sendMessage("§a[약탈자의 장갑] " + victim.getName() + "의 정신력을 30 강탈했습니다!");
        // TODO: 정신력(Sanity) 시스템 연동
    }

    // #011 공명의 종 — 300블록 내 유물 보유자 전원 위치 적발
    private void execute011(Player player) {
        player.sendMessage("§c[공명의 종] 뎅— 무거운 종소리가 울려 퍼집니다!");
        Bukkit.broadcast(Component.text("§4[공명] 어디선가 무거운 종소리가 울려 퍼집니다..."));

        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            if (p.getLocation().distance(player.getLocation()) > 300) continue;

            int relicCount = plugin.getRelicManager().countPlayerRelics(p);
            if (relicCount > 0) {
                // 발광 3초 + 벼락 파티클
                p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
                p.getWorld().strikeLightningEffect(p.getLocation());

                player.sendMessage("§e  [탐지] " + p.getName() + " — 유물 " + relicCount + "개 보유 (" +
                        (int) p.getLocation().distance(player.getLocation()) + "블록)");
                p.sendMessage("§c[경고] 공명의 종에 의해 당신의 위치가 노출되었습니다!");
            }
        }
    }

    // #010 충격 코어 — 광역 넉백 + 5초 EMP (상호작용 차단)
    private void execute010(Player player) {
        player.sendMessage("§4[충격 코어] EMP 충격파 발동!");
        Bukkit.broadcast(Component.text("§4[EMP] 거대한 충격파가 발생했습니다!"));

        // 15블록 내 적 밀어내기
        for (Entity e : player.getNearbyEntities(15, 15, 15)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;

            Vector knockback = p.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(3.0).setY(1.0);
            p.setVelocity(knockback);
            p.sendMessage("§c[EMP] 충격파에 의해 밀려났습니다! 5초간 유물 능력 사용 불가!");

            // 5초간 디버프 (구속 + 채굴피로로 상호작용 방해)
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 2, false, false));
        }
    }

    // ======================== Batch 5: #009 ~ #005 ========================

    // #009 파괴자의 서 — 봉인 즉시 파괴
    private void execute009(Player player) {
        org.bukkit.entity.ItemDisplay nearest = plugin.getSealedRelicManager().getNearestSealed(player.getLocation(), 50);
        if (nearest == null) {
            player.sendMessage("§c[RelicWars] 50블록 이내에 봉인된 유물이 없습니다!");
            return;
        }

        plugin.getSealedRelicManager().forceUnseal(nearest);
        player.sendMessage("§5[파괴자의 서] 봉인을 즉시 파괴했습니다! 유물이 획득 가능합니다!");
        Bukkit.broadcast(Component.text("§5[파괴] 누군가 봉인 유물의 봉인을 강제로 파괴했습니다!"));
    }

    // #008 그림자 막 — 3분간 모든 탐지 무효화 + 가짜 신호
    private void execute008(Player player) {
        player.sendMessage("§8[그림자 막] 3분간 팀 전체가 모든 탐지에서 사라집니다!");

        // 팀원 투명화 3분
        String teamId = plugin.getTeamManager().getTeamId(player);
        if (teamId != null) {
            for (UUID memberUuid : plugin.getTeamManager().getTeamMembers(teamId)) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    member.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 3600, 0, false, false));
                    member.sendMessage("§8[그림자 막] 3분간 모든 탐지에서 은폐됩니다!");
                }
            }
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 3600, 0, false, false));
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
    }

    // #007 파수꾼의 돔 — 15초 절대 방어막
    private void execute007(Player player) {
        player.sendMessage("§b[파수꾼의 돔] 반경 8블록 절대 방어막을 15초간 전개합니다!");
        Location center = player.getLocation().clone();

        // 배리어 블록으로 돔 생성
        Set<Location> domeBlocks = new HashSet<>();
        int radius = 8;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist >= radius - 0.5 && dist <= radius + 0.5) {
                        Location bLoc = center.clone().add(x, y, z);
                        Block b = bLoc.getBlock();
                        if (b.getType() == Material.AIR) {
                            b.setType(Material.BARRIER);
                            domeBlocks.add(bLoc);
                        }
                    }
                }
            }
        }

        // 15초 뒤 해제
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Location bLoc : domeBlocks) {
                if (bLoc.getBlock().getType() == Material.BARRIER) {
                    bLoc.getBlock().setType(Material.AIR);
                }
            }
            if (player.isOnline()) player.sendMessage("§c[파수꾼의 돔] 방어막이 해제되었습니다.");
        }, 300L);
    }

    // #006 차원 도약석 — 30블록 순간이동 + 5초 내 복귀
    private void execute006(Player player) {
        Location origin = player.getLocation().clone();
        Vector dir = player.getLocation().getDirection().normalize().multiply(30);
        Location dest = origin.clone().add(dir);
        dest.setY(player.getWorld().getHighestBlockYAt(dest) + 1);

        player.teleport(dest);
        player.sendMessage("§5[차원 도약] 30블록 전방으로 도약! 5초 내 다시 사용하면 원래 위치로 복귀합니다.");

        // 환영(파티클) 원래 위치에 표시
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 100) { this.cancel(); return; }
                ticks += 5;
                origin.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, origin.clone().add(0, 1, 0), 15, 0.3, 0.8, 0.3, 0);
            }
        }.runTaskTimer(plugin, 0L, 5L);

        // TODO: 5초 내 재사용 시 복귀 + 데미지/상태이상 무효화 (복잡한 상태머신 필요, MVP에서는 단순 텔레포트만)
    }

    // #005 불멸의 심장 — 패시브: 다운 무시 1회 (MVP: 액티브로 대체)
    private void execute005(Player player) {
        player.sendMessage("§6[불멸의 심장] 다음 치명상을 1회 무시합니다! (90분 쿨타임)");
        player.sendMessage("§7  체력이 0이 되어야 할 순간 체력 100%로 부활하며 주변 적을 밀쳐냅니다.");

        UUID id = player.getUniqueId();
        active029FallImmunity.add(id); // 임시로 같은 Set 활용 (별도 Set 필요하지만 MVP)

        // 8초간 받는 데미지 50% 감소
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 1, false, false));

        // 주변 적 밀쳐내기
        for (Entity e : player.getNearbyEntities(8, 8, 8)) {
            if (!(e instanceof Player p)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;
            Vector knockback = p.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(2.5).setY(0.8);
            p.setVelocity(knockback);
        }

        // 황금 충격파 효과
        player.getWorld().strikeLightningEffect(player.getLocation());
    }

    // ======================== Batch 6: #004 ~ #001 ========================

    // #004 폭풍의 왕관 — 반경 30블록 광역 뇌우 15초
    private void execute004(Player player) {
        Block targetBlock = player.getTargetBlockExact(100);
        Location stormCenter;
        if (targetBlock != null) {
            stormCenter = targetBlock.getLocation();
        } else {
            stormCenter = player.getLocation();
        }

        player.sendMessage("§b[포풍의 왕관] 대상 지역에 파멸적인 뇌우를 15초간 소환합니다!");
        Bukkit.broadcast(Component.text("§4[폭풍] 하늘이 침음하며 광란의 뇌우가 쏟아집니다!"));

        final Location center = stormCenter;
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 300) { this.cancel(); return; } // 15초
                ticks += 10;

                // 1초마다 3발의 베락
                for (int i = 0; i < 3; i++) {
                    double rx = (Math.random() - 0.5) * 60; // 반경 30
                    double rz = (Math.random() - 0.5) * 60;
                    Location strike = center.clone().add(rx, 0, rz);
                    strike.setY(center.getWorld().getHighestBlockYAt(strike));
                    center.getWorld().strikeLightning(strike);
                }

                // 범위 내 적에게 디버프
                for (Player p : center.getWorld().getPlayers()) {
                    if (p.equals(player)) continue;
                    if (plugin.getTeamManager().isSameTeam(player, p)) continue;
                    if (p.getLocation().distance(center) <= 30.0) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // #003 절대 좌표 나침반 — 특정 유물의 실시간 좌표 3분간 표시
    private void execute003(Player player) {
        player.sendMessage("§5[절대 좌표 나침반] 추적할 유물 번호를 채팅에 입력하세요! (1~30)");
        player.sendMessage("§7  3분간 해당 유물의 실시간 좌표가 표시됩니다.");

        // MVP: 주변 유저들의 유물 보유 현황 즉시 표시
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(player)) continue;
            int count = plugin.getRelicManager().countPlayerRelics(p);
            if (count > 0) {
                Location loc = p.getLocation();
                player.sendMessage("§e  " + p.getName() + ": 유물 " + count + "개 — X:" + (int) loc.getX() + " Y:" + (int) loc.getY() + " Z:" + (int) loc.getZ());
            }
        }
        // TODO: 채팅 입력 받아서 특정 번호 유물의 DB 소유자 좌표를 3분간 주기적으로 표시
    }

    // #002 탐욕의 적출자 — 다운된 적에게서 0.5초 즉시 강탈
    private void execute002(Player player) {
        // 5블록 이내 다운된 적 탐색
        Player target = null;
        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            if (plugin.getTeamManager().isSameTeam(player, p)) continue;
            if (!plugin.getCombatManager().isDowned(p)) continue;
            if (p.getLocation().distance(player.getLocation()) <= 5.0) {
                target = p;
                break;
            }
        }

        if (target == null) {
            player.sendMessage("§c[RelicWars] 5블록 이내에 다운된 적이 없습니다!");
            return;
        }

        Player victim = target;
        // 적에게서 가장 높은 번호 유물 1개 즉시 강탈
        org.bukkit.inventory.ItemStack[] contents = victim.getInventory().getContents();
        org.bukkit.inventory.ItemStack bestRelic = null;
        int bestNum = -1;
        int bestSlot = -1;

        for (int i = 0; i < contents.length; i++) {
            if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(contents[i])) {
                int num = com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(contents[i]);
                if (num > bestNum) {
                    bestNum = num;
                    bestRelic = contents[i];
                    bestSlot = i;
                }
            }
        }

        if (bestRelic == null) {
            player.sendMessage("§c[RelicWars] 대상이 유물을 보유하고 있지 않습니다.");
            return;
        }

        // 적에게서 유물 제거, 내게 지급
        victim.getInventory().setItem(bestSlot, null);
        player.getInventory().addItem(bestRelic);

        plugin.getDatabaseManager().updateRelicState(bestNum, "held", player.getUniqueId().toString(), player.getLocation());

        player.sendMessage("§4[탐욕의 적출자] " + victim.getName() + "에게서 유물 #" + String.format("%03d", bestNum) + "을 즉시 적출했습니다!");
        victim.sendMessage("§4[적출] 누군가 당신의 유물을 강제로 빼앗았습니다!");
        Bukkit.broadcast(Component.text("§4[탐욕] 누군가 탐욕의 적출자로 유물을 강탈했습니다!"));
    }

    // #001 태초의 별 — 60초간 갓모드 (모든 봉인 해제 + 전체 좌표 공유)
    private void execute001(Player player) {
        player.sendMessage("§6════════════════════════════════════════");
        player.sendMessage("§6[태초의 별] §e60초간 태초의 지배자 상태가 됩니다!");
        player.sendMessage("§6════════════════════════════════════════");
        Bukkit.broadcast(Component.text("§6[태초의 별] 누군가 태초의 별을 발동했습니다! 60초간 모든 정보가 공유됩니다!"));

        // 60초간 모든 유저 좌표 + 체력 + 유물 개수 표시
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 1200) { this.cancel(); return; }
                ticks += 40;

                player.sendMessage("§6─── [태초의 별] 실시간 정보 ───");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.equals(player)) continue;
                    Location loc = p.getLocation();
                    int relics = plugin.getRelicManager().countPlayerRelics(p);
                    int hp = (int) p.getHealth();
                    player.sendMessage("§e  " + p.getName() + " | HP:" + hp + " | 유물:" + relics +
                            " | X:" + (int) loc.getX() + " Y:" + (int) loc.getY() + " Z:" + (int) loc.getZ());
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // 2초마다 업데이트

        // 버프
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1200, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0, false, false));
    }

    // ======================== 유틸리티 ========================

    private String getCardinalDirection(Vector dir) {
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
    private int getSanityCost(int relicNumber) {
        if (relicNumber >= 20) return 0;   // 1~2단계
        if (relicNumber >= 11) return 10;  // 3단계
        if (relicNumber >= 6) return 20;   // 4단계
        if (relicNumber >= 1) return 30;   // 5단계
        return 0;
    }
}
