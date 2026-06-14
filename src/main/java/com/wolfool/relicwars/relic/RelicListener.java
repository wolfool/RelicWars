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
     * 유물을 넣을 수 없는 인벤토리 타입 목록.
     */
    private static final Set<InventoryType> BLOCKED_INVENTORY_TYPES = Set.of(
            InventoryType.ENDER_CHEST,
            InventoryType.SHULKER_BOX,
            InventoryType.BREWING,
            InventoryType.ANVIL,
            InventoryType.GRINDSTONE,
            InventoryType.SMITHING,
            InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE,
            InventoryType.SMOKER,
            InventoryType.DROPPER,
            InventoryType.DISPENSER,
            InventoryType.HOPPER
    );

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
    public void onPlayerUseRelic(PlayerInteractEvent event) {
        // 우클릭만 처리
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!RelicItemUtil.isRelic(item)) return;

        int relicNumber = RelicItemUtil.getRelicNumber(item);
        RelicDefinition def = RelicDefinition.getByNumber(relicNumber);
        if (def == null) return;

        // 다운 상태에서는 유물 능력 사용 불가
        if (plugin.getCombatManager().isDowned(player)) {
            player.sendMessage("§c[RelicWars] 다운 상태에서는 유물 능력을 사용할 수 없습니다!");
            event.setCancelled(true);
            return;
        }

        // 쿨타임 중인지 체크
        if (RelicItemUtil.isOnCooldown(item)) {
            int remaining = RelicItemUtil.getRemainingCooldownSeconds(item);
            player.sendMessage("§c[RelicWars] " + def.getDisplayName() +
                    " §c쿨타임 중입니다. (남은 시간: §e" +
                    RelicItemUtil.formatCooldown(remaining) + "§c)");
            event.setCancelled(true);
            return;
        }

        // --- 능력 발동 (Phase 6에서 각 유물별로 구현) ---
        if (def.getCooldownSeconds() > 0) {
            RelicItemUtil.startCooldown(item, def.getCooldownSeconds());
            player.sendMessage("§a[RelicWars] " + def.getDisplayName() +
                    " §a능력 발동! (쿨타임: §e" +
                    RelicItemUtil.formatCooldown(def.getCooldownSeconds()) + "§a)");

            // 유물 스킬 실행
            plugin.getRelicAbilityHandler().execute(player, def);
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
        if (event.getView().getTitle().equals("§5소문의 등불")) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInventory) {
                int slot = event.getSlot();
                if (slot == 1) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020Option1(player);
                } else if (slot == 3) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020Option2(player);
                } else if (slot == 5) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020Option3(player);
                } else if (slot == 7) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020Option4(player);
                }
            }
            return;
        }

        // 금지된 인벤토리 타입인지 확인
        if (!BLOCKED_INVENTORY_TYPES.contains(topInventory.getType())) return;

        // Shift-클릭: 유물이 자동으로 금지 인벤토리로 이동하는 것 방지
        if (event.isShiftClick()) {
            ItemStack clickedItem = event.getCurrentItem();
            if (RelicItemUtil.isRelic(clickedItem)) {
                event.setCancelled(true);
                player.sendMessage("§c[RelicWars] 유물은 이 보관함에 넣을 수 없습니다!");
                return;
            }
        }

        // 금지 인벤토리 슬롯에 직접 놓기 방지
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(topInventory)) {
            ItemStack cursor = event.getCursor();
            if (RelicItemUtil.isRelic(cursor)) {
                event.setCancelled(true);
                player.sendMessage("§c[RelicWars] 유물은 이 보관함에 넣을 수 없습니다!");
                return;
            }
        }

        // 합법적인 상자(Chest, Barrel)에 유물을 넣을 때 DB 업데이트
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInventory)) {
            ItemStack cursor = event.getCursor();
            if (RelicItemUtil.isRelic(cursor)) {
                // 커서에 든 유물을 상자에 내려놓음
                int relicNum = RelicItemUtil.getRelicNumber(cursor);
                plugin.getDatabaseManager().updateRelicState(relicNum, "dropped", null, topInventory.getLocation());
            }
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            // 쉬프트 클릭으로 상자에 넣음
            ItemStack clickedItem = event.getCurrentItem();
            if (RelicItemUtil.isRelic(clickedItem)) {
                int relicNum = RelicItemUtil.getRelicNumber(clickedItem);
                plugin.getDatabaseManager().updateRelicState(relicNum, "dropped", null, topInventory.getLocation());
            }
        }

        // 상자에서 유물을 꺼낼 때 DB 업데이트
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInventory)) {
            ItemStack clickedItem = event.getCurrentItem();
            if (RelicItemUtil.isRelic(clickedItem) && (event.getAction() == InventoryAction.PICKUP_ALL || event.getAction() == InventoryAction.PICKUP_HALF || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                int relicNum = RelicItemUtil.getRelicNumber(clickedItem);
                plugin.getDatabaseManager().updateRelicState(relicNum, "held", player.getUniqueId().toString(), player.getLocation());
            }
        }
    }

    // ======================== 드랍 및 줍기 추적 ========================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (RelicItemUtil.isRelic(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c[RelicWars] 유물은 'Q'키로 버릴 수 없습니다! (사망 시, 혹은 /relic transfer 로 양도만 가능)");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack item = event.getItem().getItemStack();
            if (RelicItemUtil.isRelic(item)) {
                int relicNum = RelicItemUtil.getRelicNumber(item);
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
        if (RelicItemUtil.isRelic(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c[RelicWars] 유물은 여기에 설치할 수 없습니다!");
        }
    }

    // ======================== 채팅 가로채기 (#020 소문의 등불) ========================

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
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
                            player.sendMessage("§7[소문의 등불] §e" + def.getName() + " §7유물의 소유자: §a" + targetPlayer.getName() + " §7(온라인)");
                        } else {
                            org.bukkit.OfflinePlayer offlineTarget = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(ownerUuid));
                            player.sendMessage("§7[소문의 등불] §e" + def.getName() + " §7유물의 소유자: §c" + (offlineTarget.getName() != null ? offlineTarget.getName() : "알 수 없음") + " §7(오프라인)");
                        }
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§c[RelicWars] 유물 번호는 숫자여야 합니다. 검색이 취소되었습니다.");
                }
            });
        }
    }
}
