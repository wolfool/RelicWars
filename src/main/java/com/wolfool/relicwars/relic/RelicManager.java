package com.wolfool.relicwars.relic;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

/**
 * 유물의 생성, 소유 추적, 드랍 로직을 총괄하는 매니저.
 * 모든 유물 관련 비즈니스 로직의 중심입니다.
 */
public class RelicManager implements Manager {

    private final RelicWars plugin;

    /** 전체 유물 스폰 이력 (유물 번호 → 스폰 여부). 엔딩 조건 추적에 사용. */
    private final Set<Integer> spawnedRelics = new HashSet<>();

    public RelicManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        // 리스너 등록
        Bukkit.getPluginManager().registerEvents(new RelicListener(plugin, this), plugin);
        plugin.getLogger().info("§a[RelicWars] RelicManager 초기화 완료.");
    }

    @Override
    public void shutdown() {
        // 데이터 저장 등 (Phase 4에서 구현)
        plugin.getLogger().info("§a[RelicWars] RelicManager 종료.");
    }

    // ======================== 유물 생성 ========================

    /**
     * 유물을 새로 생성하여 반환합니다.
     * @param relicNumber 유물 번호 (0~30)
     * @return 유물 아이템, 정의되지 않은 번호면 null
     */
    public ItemStack createRelic(int relicNumber) {
        RelicDefinition def = RelicDefinition.getByNumber(relicNumber);
        if (def == null) return null;
        markAsSpawned(relicNumber);
        return RelicItemUtil.createRelicItem(def);
    }

    /**
     * 특정 플레이어에게 유물을 지급합니다.
     * @return 지급 성공 여부
     */
    public boolean giveRelic(Player player, int relicNumber) {
        int currentCount = countPlayerRelics(player);
        int max = plugin.getConfigManager().getMaxRelicsPerPlayer();
        if (currentCount >= max) {
            player.sendMessage("§c[RelicWars] 유물은 최대 " + max + "개까지만 소지할 수 있습니다.");
            return false;
        }

        ItemStack relic = createRelic(relicNumber);
        if (relic == null) {
            player.sendMessage("§c[RelicWars] 존재하지 않는 유물 번호입니다: #" + String.format("%03d", relicNumber));
            return false;
        }

        player.getInventory().addItem(relic);
        RelicDefinition def = RelicDefinition.getByNumber(relicNumber);
        player.sendMessage("§a[RelicWars] " + def.getDisplayName() + " §a유물을 획득했습니다!");
        return true;
    }

    // ======================== 소지 유물 조회 ========================

    /**
     * 플레이어가 소지 중인 유물 아이템 목록을 반환합니다.
     */
    public List<ItemStack> getPlayerRelics(Player player) {
        List<ItemStack> relics = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (RelicItemUtil.isRelic(item)) {
                relics.add(item);
            }
        }
        return relics;
    }

    /**
     * 플레이어가 소지 중인 유물 개수를 반환합니다.
     */
    public boolean hasRelic(Player player, int relicNumber) {
        return getPlayerRelicNumbers(player).contains(relicNumber);
    }

    public int countPlayerRelics(Player player) {
        return getPlayerRelics(player).size();
    }

    /**
     * 플레이어가 소지 중인 유물의 번호 목록을 정렬하여 반환합니다.
     */
    public List<Integer> getPlayerRelicNumbers(Player player) {
        List<Integer> numbers = new ArrayList<>();
        for (ItemStack item : getPlayerRelics(player)) {
            numbers.add(RelicItemUtil.getRelicNumber(item));
        }
        Collections.sort(numbers);
        return numbers;
    }

    // ======================== 드랍 로직 ========================

    /**
     * [1단계] 다운 시 즉시 드랍: 가장 낮은 번호(최강) 유물 1개를 드랍합니다.
     * @return 드랍된 유물 아이템, 유물이 없으면 null
     */
    public ItemStack extractBestRelic(Player player) {
        List<ItemStack> relics = getPlayerRelics(player);
        if (relics.isEmpty()) return null;

        // 가장 낮은 번호 (최강) 유물 찾기
        ItemStack bestRelic = null;
        int lowestNumber = Integer.MAX_VALUE;

        for (ItemStack item : relics) {
            int num = RelicItemUtil.getRelicNumber(item);
            if (num < lowestNumber) {
                lowestNumber = num;
                bestRelic = item;
            }
        }

        if (bestRelic != null) {
            player.getInventory().remove(bestRelic);
        }
        return bestRelic;
    }

    /**
     * [2단계] 강탈(Steal) 드랍: 소유 개수에 따라 config의 steal-drop-rules 기준으로
     * 가장 높은 번호(약한) 유물들을 드랍합니다.
     * @return 드랍된 유물 아이템 목록
     */
    public List<ItemStack> extractStealDrop(Player player) {
        List<ItemStack> relics = getPlayerRelics(player);
        if (relics.isEmpty()) return Collections.emptyList();

        // 소유수에 따른 강탈 개수 확인
        int dropCount = plugin.getConfigManager().getDownedDropCount(relics.size());
        if (dropCount <= 0) return Collections.emptyList();

        // 가장 높은 번호(약한) 유물부터 강탈당하도록 내림차순 정렬
        java.util.Collections.shuffle(relics);

        List<ItemStack> dropped = new ArrayList<>();
        for (int i = 0; i < dropCount && i < relics.size(); i++) {
            ItemStack item = relics.get(i);
            player.getInventory().remove(item);
            dropped.add(item);
        }
        return dropped;
    }

    // ======================== 스폰 이력 추적 ========================

    /**
     * 유물을 스폰 이력에 등록합니다.
     */
    public void markAsSpawned(int relicNumber) {
        spawnedRelics.add(relicNumber);
    }

    /**
     * 30개 넘버링 유물(#001~#030)이 모두 스폰되었는지 확인합니다.
     */
    public boolean areAllRelicsSpawned() {
        for (int i = 1; i <= 30; i++) {
            if (!spawnedRelics.contains(i)) return false;
        }
        return true;
    }

    /**
     * 아직 스폰되지 않은 유물 번호 목록을 반환합니다.
     */
    public List<Integer> getUnspawnedRelics() {
        List<Integer> unspawned = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            if (!spawnedRelics.contains(i)) unspawned.add(i);
        }
        return unspawned;
    }
    public boolean isOnCooldown(org.bukkit.entity.Player p, int id) { return false; }
    public void setCooldown(org.bukkit.entity.Player p, int id) {}
}
