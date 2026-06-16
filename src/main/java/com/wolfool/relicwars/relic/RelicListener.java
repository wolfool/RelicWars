package com.wolfool.relicwars.relic;

import com.wolfool.relicwars.RelicWars;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * 유물 관련 이벤트를 처리하는 리스너.
 * - 우클릭 능력 발동 (쿨타임 검증)
 * - 보관 제한 (엔더 상자, 셜커 상자 등)
 * - 유물 드랍 시 봉인 처리 (Phase 3에서 확장)
 */
public class RelicListener implements Listener {

    private final RelicWars plugin;
    private final RelicManager relicManager;


    /**
     * 유물을 설치할 수 없는 블록(아이템 액자, 갑옷 거치대 등)에 대한
     * 상호작용을 차단하기 위한 블록 Material 목록.
     */
    private static final Set<Material> BLOCKED_INTERACT_BLOCKS = Set.of(
            Material.ITEM_FRAME,
            Material.GLOW_ITEM_FRAME,
            Material.ARMOR_STAND
    );

    public RelicListener(RelicWars plugin, RelicManager relicManager) {
        this.plugin = plugin;
        this.relicManager = relicManager;
    }

    // ======================== 우클릭 능력 발동 ========================

    /**
     * 유물을 들고 우클릭했을 때 능력을 발동합니다.
     * - 양손(메인핸드/오프핸드) 모두 허용
     * - 쿨타임 체크 (아이템 자체에 귀속)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLeftClick001(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            org.bukkit.entity.Player player = event.getPlayer();
            if (plugin.getCombatManager().isDowned(player)) return; // 다운 중 사용 불가
            if (plugin.getRelicAbilityHandler().isEmpAffected(player.getUniqueId())) return; // EMP 중 사용 불가
            if (plugin.getRelicAbilityHandler().active001Omega.contains(player.getUniqueId())) {
                org.bukkit.inventory.ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem == null || handItem.getType() == org.bukkit.Material.AIR) {
                    // 벼락 내부 쿨타임 (3초)
                    long now = System.currentTimeMillis();
                    Long lastUse = plugin.getRelicAbilityHandler().getLastLightningTime(player.getUniqueId());
                    if (lastUse != null && now - lastUse < 3000) {
                        int remaining = (int) ((3000 - (now - lastUse)) / 1000) + 1;
                        player.sendActionBar(net.kyori.adventure.text.Component.text("§c[심판의 벼락] 재시전까지 " + remaining + "초"));
                        return;
                    }
                    plugin.getRelicAbilityHandler().setLastLightningTime(player.getUniqueId(), now);

                    // 빈 손 좌클릭 시 심판의 벼락
                    org.bukkit.block.Block targetBlock = player.getTargetBlockExact(100);
                    if (targetBlock != null) {
                        org.bukkit.Location loc = targetBlock.getLocation();
                        loc.getWorld().strikeLightningEffect(loc);
                        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
                            if (e instanceof org.bukkit.entity.Player target && !target.equals(player)) {
                                if (plugin.getTeamManager().isSameTeam(player, target)) continue;
                                target.sendMessage("§4[심판] 태초의 지배자가 내린 벼락에 맞았습니다!");
                                // 다운 시스템 경유: 높은 데미지를 입혀서 다운 판정을 거침
                                target.damage(9999.0, player);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerUseRelic(PlayerInteractEvent event) {
        // 우클릭만 처리
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // 메인핸드 이벤트만 처리 (OFF_HAND 이벤트에 의한 이중 발동 방지)
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)) return;

        int relicNumber = com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(item);
        RelicDefinition def = RelicDefinition.getByNumber(relicNumber);
        if (def == null) return;

        // 바닐라 동작(아이템 던지기, 먹기, 블록 설치 등)을 완벽히 차단하여 유물이 소진되는 것을 방지합니다.
        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);

        // 다운 상태에서는 유물 능력 사용 불가
        if (plugin.getCombatManager().isDowned(player)) {
            player.sendMessage("§c[RelicWars] 다운 상태에서는 유물 능력을 사용할 수 없습니다!");
            event.setCancelled(true);
            return;
        }

        // 쥐타임 중인지 체크 (태초의 지배자 상태면 쿨타임 무시)
        boolean isOmega = plugin.getRelicAbilityHandler().active001Omega.contains(player.getUniqueId());
        if (!isOmega && com.wolfool.relicwars.relic.RelicItemUtil.isOnCooldown(item)) {
            int remaining = com.wolfool.relicwars.relic.RelicItemUtil.getRemainingCooldownSeconds(item);
            player.sendMessage("§c[RelicWars] " + def.getDisplayName() +
                    " §c쿨타임 중입니다. (남은 시간: §e" +
                    com.wolfool.relicwars.relic.RelicItemUtil.formatCooldown(remaining) + "§c)");
            event.setCancelled(true);
            return;
        }

        // --- 능력 발동 ---
        // execute()가 true를 반환하면 성공 → 쿨타임 시작
        // false를 반환하면 실패 (대상 없음 등) → 쿨타임 미적용
        boolean success = plugin.getRelicAbilityHandler().execute(player, def);
        if (success && def.getCooldownSeconds() > 0) {
            if (!isOmega) {
                com.wolfool.relicwars.relic.RelicItemUtil.startCooldown(item, def.getCooldownSeconds());
                player.sendMessage("§a[RelicWars] " + def.getDisplayName() +
                        " §a능력 발동! (쿨타임: §e" +
                        com.wolfool.relicwars.relic.RelicItemUtil.formatCooldown(def.getCooldownSeconds()) + "§a)");
            } else {
                player.sendMessage("§6[태초의 지배자] §a" + def.getDisplayName() + " 능력 발동! (쿨타임 면제)");
            }
        }
    }

    /**
     * 불멸의 심장(토템)이 바닐라 효과로 인해 소모되는 것을 방지합니다.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityResurrect(org.bukkit.event.entity.EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // 양손을 확인하여 토템 유물인지 확인
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(main) && main.getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
            event.setCancelled(true);
        } else if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(off) && off.getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
            event.setCancelled(true);
        }
    }

    // ======================== 보관 제한 ========================

    /**
     * 유물을 금지된 인벤토리(엔더 상자, 셜커 상자, 깔때기, 발사기 등)에
     * 넣으려 할 때 차단합니다.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = event.getView().getTopInventory();

        // 소문의 등불 GUI 처리
        if (event.getView().getTitle().equals(com.wolfool.relicwars.relic.ability.RelicAbilityHandler.GUI_TITLE_020)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInventory) {
                int slot = event.getSlot();
                if (slot == 3) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020Option2(player); // 봉인 유물 스캔
                } else if (slot == 5) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020Option3(player); // 소유자 검색
                }
            }
            return;
        }
        
        // 봉인의 바늘 GUI 처리
        if (event.getView().getTitle().equals(com.wolfool.relicwars.relic.ability.RelicAbilityHandler.GUI_TITLE_019)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInventory) {
                int slot = event.getSlot();
                if (slot == 3) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute019Option1(player);
                } else if (slot == 5) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute019Option2(player);
                }
            }
            return;
        }

        // 합법적인 자기 인벤토리인지 확인 (CRAFTING = E키 기본 인벤토리, PLAYER = 기타 유저 인벤, CREATIVE = 크리에이티브 모드 인벤)
        boolean isPlayerInventory = topInventory.getType() == InventoryType.CRAFTING || 
                                    topInventory.getType() == InventoryType.PLAYER || 
                                    topInventory.getType() == InventoryType.CREATIVE;

        if (!isPlayerInventory) {
            // 외부 상자/보관함/GUI 등에 유물을 옮기려는 시도 전면 차단
            
            // Shift-클릭으로 넣기 방지
            if (event.isShiftClick()) {
                ItemStack clickedItem = event.getCurrentItem();
                if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(clickedItem)) {
                    event.setCancelled(true);
                    player.sendMessage("§c[RelicWars] 유물은 자신의 인벤토리에만 보관할 수 있습니다!");
                    return;
                }
            }

            // 직접 커서로 넣기 방지
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInventory)) {
                ItemStack cursor = event.getCursor();
                if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage("§c[RelicWars] 유물은 자신의 인벤토리에만 보관할 수 있습니다!");
                    return;
                }
            }

            // 핫바 스왑(숫자키) 방지
            if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInventory)) {
                    ItemStack hotbarItem = event.getView().getBottomInventory().getItem(event.getHotbarButton());
                    if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(hotbarItem)) {
                        event.setCancelled(true);
                        player.sendMessage("§c[RelicWars] 유물은 자신의 인벤토리에만 보관할 수 있습니다!");
                        return;
                    }
                }
            }
        }
        
        // 번들(꾸러미) 악용 방지 (자신의 인벤토리더라도 번들 안에는 넣을 수 없음)
        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        if (cursorItem != null && cursorItem.getType() == org.bukkit.Material.BUNDLE && com.wolfool.relicwars.relic.RelicItemUtil.isRelic(currentItem)) {
            event.setCancelled(true);
            player.sendMessage("§c[RelicWars] 유물을 꾸러미에 넣을 수 없습니다!");
            return;
        }
        if (currentItem != null && currentItem.getType() == org.bukkit.Material.BUNDLE && com.wolfool.relicwars.relic.RelicItemUtil.isRelic(cursorItem)) {
            event.setCancelled(true);
            player.sendMessage("§c[RelicWars] 유물을 꾸러미에 넣을 수 없습니다!");
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInventory = event.getView().getTopInventory();
        boolean isPlayerInventory = topInventory.getType() == InventoryType.CRAFTING || 
                                    topInventory.getType() == InventoryType.PLAYER || 
                                    topInventory.getType() == InventoryType.CREATIVE;
        if (!isPlayerInventory) {
            ItemStack cursor = event.getOldCursor();
            if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(cursor)) {
                for (int slot : event.getRawSlots()) {
                    if (slot < topInventory.getSize()) { // Top inventory 쪽에 드래그 하려는 경우
                        event.setCancelled(true);
                        player.sendMessage("§c[RelicWars] 유물은 외부 상자에 넣을 수 없습니다!");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)) {
                event.getInventory().setResult(null); // 제작 결과 삭제
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        // 손 바꾸기는 허용 (양손 다 플레이어 인벤토리)
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        // 호퍼 등이 아이템을 옮길 때
        if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        // 호퍼 등이 바닥의 아이템을 주울 때 (봉인 상태이든 아니든 막음)
        ItemStack item = event.getItem().getItemStack();
        if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)) {
            event.setCancelled(true);
        }
    }

    // ======================== 드랍 및 줍기 추적 ========================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c[RelicWars] 유물은 'Q'키로 버릴 수 없습니다! (사망 시, 혹은 /relic transfer 로 양도만 가능)");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack item = event.getItem().getItemStack();
            if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)) {
                int relicNum = com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(item);
                plugin.getDatabaseManager().updateRelicState(relicNum, "held", player.getUniqueId().toString(), player.getLocation());
            }
        }
    }

    /**
     * 유물을 아이템 액자나 갑옷 거치대에 설치하는 것을 방지합니다.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Material blockType = event.getClickedBlock().getType();
        if (!BLOCKED_INTERACT_BLOCKS.contains(blockType)) return;

        ItemStack item = event.getItem();
        if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c[RelicWars] 유물은 여기에 설치할 수 없습니다!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof org.bukkit.entity.ItemFrame || 
            event.getRightClicked() instanceof org.bukkit.entity.GlowItemFrame) {
            ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
            if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c[RelicWars] 유물은 아이템 액자에 넣을 수 없습니다!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ItemStack item = event.getPlayerItem();
        if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c[RelicWars] 유물은 갑옷 거치대에 장착할 수 없습니다!");
        }
    }

    // ======================== 중복 복사본 검사 ========================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 메인 스레드에서 인벤토리 스냅샷 수집
            java.util.Map<Integer, Integer> relicSnapshot = new java.util.HashMap<>(); // slot -> relicNum
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(item)) {
                    relicSnapshot.put(i, com.wolfool.relicwars.relic.RelicItemUtil.getRelicNumber(item));
                }
            }
            if (relicSnapshot.isEmpty()) return;

            // DB 조회는 async에서
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                java.util.List<Integer> fakeSlots = new java.util.ArrayList<>();
                for (java.util.Map.Entry<Integer, Integer> entry : relicSnapshot.entrySet()) {
                    String dbOwner = plugin.getDatabaseManager().getRelicOwner(entry.getValue());
                    if (dbOwner == null || !dbOwner.equals(player.getUniqueId().toString())) {
                        fakeSlots.add(entry.getKey());
                        plugin.getLogger().warning("[보안] " + player.getName() + "의 인벤토리에서 복사된/비정상 유물 #" + entry.getValue() + " 이 적발되어 회수 조치됨.");
                    }
                }
                if (!fakeSlots.isEmpty()) {
                    // 삭제는 메인 스레드에서
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            for (int slot : fakeSlots) {
                                player.getInventory().setItem(slot, null);
                            }
                            player.sendMessage("§c[RelicWars] 인벤토리에서 비정상적인 유물(복사본)이 감지되어 시스템에 의해 회수되었습니다.");
                        }
                    });
                }
            });
        }, 20L); // 접속 1초 후 검사
    }

    // ======================== 채팅 가로채기 (#020 소문의 등불) ========================

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        if (plugin.getRelicAbilityHandler().active020PingMode.contains(player.getUniqueId())) {
            event.setCancelled(true);
            String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getRelicAbilityHandler().active020PingMode.remove(player.getUniqueId());
                try {
                    int targetNum = Integer.parseInt(msg);
                    if (!plugin.getRelicManager().hasRelic(player, targetNum)) {
                        player.sendMessage("§c[소문의 등불] 기만 전술은 본인이 소지한 유물의 번호만 사용할 수 있습니다!");
                        return;
                    }
                    
                    int x = (int)(Math.random() * 2000 - 1000);
                    int z = (int)(Math.random() * 2000 - 1000);
                    String dir = plugin.getRelicAbilityHandler().getCardinalDirection(new org.bukkit.util.Vector(x - player.getLocation().getX(), 0, z - player.getLocation().getZ()));
                    
                    com.wolfool.relicwars.relic.RelicDefinition def = com.wolfool.relicwars.relic.RelicDefinition.getByNumber(targetNum);
                    com.wolfool.relicwars.util.RumorUtil.broadcastRumor(new org.bukkit.Location(player.getWorld(), x, 60, z), "§b[소문] " + dir + "쪽에서 " + def.name() + "의 기운이 느껴집니다...");
                    player.sendMessage("§d[소문의 등불] 기만 전술 성공! 적들이 가짜 위치로 향할 것입니다.");
                    
                } catch (NumberFormatException e) {
                    player.sendMessage("§c[RelicWars] 숫자로만 입력해주세요.");
                }
            });
            return;
        }

        if (plugin.getRelicAbilityHandler().active020ScanMode.contains(player.getUniqueId())) {
            event.setCancelled(true);
            
            String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            
            // 동기 스레드로 넘겨서 Bukkit API 안전하게 호출
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getRelicAbilityHandler().active020ScanMode.remove(player.getUniqueId());
                
                try {
                    int targetNum = Integer.parseInt(msg);
                    com.wolfool.relicwars.relic.RelicDefinition def = com.wolfool.relicwars.relic.RelicDefinition.getByNumber(targetNum);
                    if (def == null) {
                        player.sendMessage("§c[RelicWars] 존재하지 않는 유물 번호입니다. (1~30)");
                        return;
                    }
                    
                    String ownerUuid = plugin.getDatabaseManager().getRelicOwner(targetNum);
                    if (ownerUuid == null) {
                        player.sendMessage("§7[소문의 등불] §e" + def.getName() + " §7유물은 아직 누구의 소유도 아닙니다.");
                    } else {
                        org.bukkit.entity.Player targetPlayer = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(ownerUuid));
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            if (plugin.getRelicAbilityHandler().active008Shadow.contains(targetPlayer.getUniqueId())) {
                                player.sendMessage("§7[소문의 등불] §e" + def.getName() + " §7유물의 소유자: §8[알 수 없는 그림자] §7(탐지 불가)");
                            } else {
                                player.sendMessage("§7[소문의 등불] §e" + def.getName() + " §7유물의 소유자: §a" + targetPlayer.getName() + " §7(온라인)");
                            }
                        } else {
                            org.bukkit.OfflinePlayer offlineTarget = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(ownerUuid));
                            player.sendMessage("§7[소문의 등불] §e" + def.getName() + " §7유물의 소유자: §c" + (offlineTarget.getName() != null ? offlineTarget.getName() : "알 수 없음") + " §7(오프라인)");
                        }
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§c[RelicWars] 유물 번호는 숫자여야 합니다. 검색이 취소되었습니다.");
                }
            });
        } else if (plugin.getRelicAbilityHandler().active003TrackerWait.contains(player.getUniqueId())) {
            event.setCancelled(true);
            
            String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getRelicAbilityHandler().active003TrackerWait.remove(player.getUniqueId());
                
                try {
                    int targetNum = Integer.parseInt(msg);
                    com.wolfool.relicwars.relic.RelicDefinition def = com.wolfool.relicwars.relic.RelicDefinition.getByNumber(targetNum);
                    if (def == null) {
                        player.sendMessage("§c[RelicWars] 존재하지 않는 유물 번호입니다. (1~30)");
                        return;
                    }
                    
                    plugin.getRelicAbilityHandler().start003Tracker(player, targetNum);
                } catch (NumberFormatException e) {
                    player.sendMessage("§c[RelicWars] 유물 번호는 숫자여야 합니다. 추적이 취소되었습니다.");
                }
            });
        }
    }
}
