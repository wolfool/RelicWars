package com.wolfool.relicwars.relic;

import com.wolfool.relicwars.RelicWars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 유물 아이템(ItemStack)을 생성하고, PDC(PersistentDataContainer)를 통해
 * 유물 고유 데이터(번호, 쿨타임)를 읽고 쓰는 유틸리티 클래스.
 */
public class RelicItemUtil {

    // === PDC 키 (NamespacedKey) ===
    /** 유물 번호 (int: 0~30) */
    public static final NamespacedKey KEY_RELIC_NUMBER =
            new NamespacedKey(RelicWars.getInstance(), "relic_number");

    /** 쿨타임 만료 시각 (long: epoch millis). 이 값이 현재 시각보다 크면 아직 쿨타임 중. */
    public static final NamespacedKey KEY_COOLDOWN_UNTIL =
            new NamespacedKey(RelicWars.getInstance(), "cooldown_until");

    /** 유물 여부 마커 (byte: 1). 이 태그가 있으면 유물 아이템으로 인식. */
    public static final NamespacedKey KEY_IS_RELIC =
            new NamespacedKey(RelicWars.getInstance(), "is_relic");

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    /**
     * RelicDefinition으로부터 유물 ItemStack을 새로 생성합니다.
     * 쿨타임은 0(즉시 사용 가능)으로 초기화됩니다.
     *
     * @param definition 유물 정의
     * @return 생성된 유물 아이템
     */
    public static ItemStack createRelicItem(RelicDefinition definition) {
        ItemStack item = new ItemStack(definition.getMaterial(), 1);
        ItemMeta meta = item.getItemMeta();

        // --- 이름 ---
        meta.displayName(LEGACY.deserialize(definition.getDisplayName())
                .decoration(TextDecoration.ITALIC, false));

        // --- 로어(Lore) ---
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§8" + definition.getTierName())
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize(definition.getLoreDescription())
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (definition.getCooldownSeconds() > 0) {
            lore.add(LEGACY.deserialize("§e쿨타임: §f" + formatCooldown(definition.getCooldownSeconds()))
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§8§o넘버링 유물 — 우클릭으로 능력 발동")
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);

        // --- 인챈트 효과 (시각적 반짝임) ---
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // --- PDC 태그 ---
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_IS_RELIC, PersistentDataType.BYTE, (byte) 1);
        pdc.set(KEY_RELIC_NUMBER, PersistentDataType.INTEGER, definition.getNumber());
        pdc.set(KEY_COOLDOWN_UNTIL, PersistentDataType.LONG, 0L); // 쿨타임 없음
        
        // --- CustomModelData ---
        meta.setCustomModelData(10000 + definition.getNumber());

        item.setItemMeta(meta);
        return item;
    }

    // ======================== PDC 읽기/쓰기 유틸 ========================

    /**
     * 이 아이템이 유물인지 확인합니다.
     */
    public static boolean isRelic(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(KEY_IS_RELIC, PersistentDataType.BYTE);
    }

    /**
     * 유물의 번호를 반환합니다. 유물이 아니면 -1을 반환합니다.
     */
    public static int getRelicNumber(ItemStack item) {
        if (!isRelic(item)) return -1;
        Integer num = item.getItemMeta().getPersistentDataContainer()
                .get(KEY_RELIC_NUMBER, PersistentDataType.INTEGER);
        return num != null ? num : -1;
    }

    /**
     * 유물의 쿨타임이 아직 돌고 있는지 확인합니다.
     * @return true면 아직 쿨타임 중
     */
    public static boolean isOnCooldown(ItemStack item) {
        if (!isRelic(item)) return false;
        Long until = item.getItemMeta().getPersistentDataContainer()
                .get(KEY_COOLDOWN_UNTIL, PersistentDataType.LONG);
        if (until == null) return false;
        return System.currentTimeMillis() < until;
    }

    /**
     * 남은 쿨타임 시간(초)을 반환합니다.
     */
    public static int getRemainingCooldownSeconds(ItemStack item) {
        if (!isRelic(item)) return 0;
        Long until = item.getItemMeta().getPersistentDataContainer()
                .get(KEY_COOLDOWN_UNTIL, PersistentDataType.LONG);
        if (until == null) return 0;
        long remaining = until - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    /**
     * 유물의 쿨타임을 시작합니다. (아이템 NBT에 만료 시각을 기록)
     * 쿨타임은 아이템 자체에 귀속되므로, 유저가 바뀌어도 쿨타임은 유지됩니다.
     *
     * @param item 유물 아이템
     * @param seconds 쿨타임 시간(초)
     */
    public static void startCooldown(ItemStack item, int seconds) {
        if (!isRelic(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer()
                .set(KEY_COOLDOWN_UNTIL, PersistentDataType.LONG,
                        System.currentTimeMillis() + (seconds * 1000L));
        item.setItemMeta(meta);
    }

    /**
     * 유물의 쿨타임을 초기화합니다.
     */
    public static void resetCooldown(ItemStack item) {
        if (!isRelic(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer()
                .set(KEY_COOLDOWN_UNTIL, PersistentDataType.LONG, 0L);
        item.setItemMeta(meta);
    }

    // ======================== 유틸 ========================

    /**
     * 초를 "X분 Y초" 또는 "X시간 Y분" 형식으로 변환합니다.
     */
    public static String formatCooldown(int totalSeconds) {
        if (totalSeconds >= 3600) {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            return hours + "시간" + (minutes > 0 ? " " + minutes + "분" : "");
        } else if (totalSeconds >= 60) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return minutes + "분" + (seconds > 0 ? " " + seconds + "초" : "");
        } else {
            return totalSeconds + "초";
        }
    }
}
