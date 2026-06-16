package com.wolfool.relicwars.integration;

import com.wolfool.relicwars.RelicWars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * BetterHud 커스텀 Placeholder 등록 (리플렉션 기반).
 * BetterHud JAR이 Java 25로 컴파일되어 직접 import 불가.
 * 
 * 등록되는 placeholder:
 * [Number] relicwars_sanity, relicwars_sanity_max, relicwars_sanity_percent,
 *          relicwars_relic_count, relicwars_team_relic_count, relicwars_capture_progress
 * [Boolean] relicwars_is_downed, relicwars_capture_active, relicwars_combat_tagged, relicwars_is_omega
 */
public class RelicHudPlaceholders {

    private final RelicWars plugin;

    public RelicHudPlaceholders(RelicWars plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void register() {
        try {
            // BetterHudAPI.inst()
            Class<?> apiClass = Class.forName("kr.toxicity.hud.api.BetterHudAPI");
            Method instMethod = apiClass.getMethod("inst");
            Object hud = instMethod.invoke(null);

            // hud.getPlaceholderManager()
            Method getPlaceholderManager = hud.getClass().getMethod("getPlaceholderManager");
            Object pm = getPlaceholderManager.invoke(hud);

            // pm.getNumberContainer(), pm.getBooleanContainer()
            Method getNumberContainer = pm.getClass().getMethod("getNumberContainer");
            Method getBooleanContainer = pm.getClass().getMethod("getBooleanContainer");
            Object numberContainer = getNumberContainer.invoke(pm);
            Object boolContainer = getBooleanContainer.invoke(pm);

            // HudPlaceholder.of() 메서드 가져오기
            Class<?> hudPlaceholderClass = Class.forName("kr.toxicity.hud.api.placeholder.HudPlaceholder");
            Class<?> placeholderFunctionClass = Class.forName("kr.toxicity.hud.api.placeholder.HudPlaceholder$PlaceholderFunction");
            Method ofMethod = placeholderFunctionClass.getMethod("of", Function.class);

            // PlaceholderContainer.addPlaceholder(String, HudPlaceholder)
            Method addPlaceholder = numberContainer.getClass().getMethod("addPlaceholder", String.class, hudPlaceholderClass);
            Method addBoolPlaceholder = boolContainer.getClass().getMethod("addPlaceholder", String.class, hudPlaceholderClass);

            // HudPlaceholder.of(PlaceholderFunction)
            Method hudOf = hudPlaceholderClass.getMethod("of", placeholderFunctionClass);

            // HudPlayer.uuid() 메서드 참조 준비
            Class<?> hudPlayerClass = Class.forName("kr.toxicity.hud.api.player.HudPlayer");
            Method uuidMethod = hudPlayerClass.getMethod("uuid");

            // === Number Placeholders ===
            registerNumber(addPlaceholder, hudOf, ofMethod, numberContainer, uuidMethod,
                    "relicwars_sanity", uuid -> {
                        Player p = Bukkit.getPlayer(uuid);
                        return p != null ? plugin.getSanityManager().getSanity(p) : 0;
                    });

            registerNumber(addPlaceholder, hudOf, ofMethod, numberContainer, uuidMethod,
                    "relicwars_sanity_max", uuid -> plugin.getConfigManager().getSanityMax());

            registerNumber(addPlaceholder, hudOf, ofMethod, numberContainer, uuidMethod,
                    "relicwars_sanity_percent", uuid -> {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p == null) return 0;
                        int s = plugin.getSanityManager().getSanity(p);
                        int m = plugin.getConfigManager().getSanityMax();
                        return m > 0 ? (s * 100 / m) : 0;
                    });

            registerNumber(addPlaceholder, hudOf, ofMethod, numberContainer, uuidMethod,
                    "relicwars_relic_count", uuid -> {
                        Player p = Bukkit.getPlayer(uuid);
                        return p != null ? plugin.getRelicManager().countPlayerRelics(p) : 0;
                    });

            registerNumber(addPlaceholder, hudOf, ofMethod, numberContainer, uuidMethod,
                    "relicwars_team_relic_count", uuid -> {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p == null) return 0;
                        String teamId = plugin.getTeamManager().getTeamId(p);
                        if (teamId != null) return plugin.getTeamManager().getTeamRelicCount(teamId);
                        return plugin.getRelicManager().countPlayerRelics(p);
                    });

            registerNumber(addPlaceholder, hudOf, ofMethod, numberContainer, uuidMethod,
                    "relicwars_capture_progress", uuid ->
                            Math.round(plugin.getEndingManager().getCaptureProgress() * 100));

            // === Boolean Placeholders ===
            registerBool(addBoolPlaceholder, hudOf, ofMethod, boolContainer, uuidMethod,
                    "relicwars_is_downed", uuid -> {
                        Player p = Bukkit.getPlayer(uuid);
                        return p != null && plugin.getCombatManager().isDowned(p);
                    });

            registerBool(addBoolPlaceholder, hudOf, ofMethod, boolContainer, uuidMethod,
                    "relicwars_capture_active", uuid ->
                            plugin.getEndingManager().isCaptureActive());

            registerBool(addBoolPlaceholder, hudOf, ofMethod, boolContainer, uuidMethod,
                    "relicwars_combat_tagged", uuid -> {
                        Player p = Bukkit.getPlayer(uuid);
                        return p != null && plugin.getCombatManager().isInCombat(p);
                    });

            registerBool(addBoolPlaceholder, hudOf, ofMethod, boolContainer, uuidMethod,
                    "relicwars_is_omega", uuid -> {
                        Player p = Bukkit.getPlayer(uuid);
                        return p != null && plugin.getRelicAbilityHandler().active001Omega.contains(p.getUniqueId());
                    });

            plugin.getLogger().info("§a[RelicWars] BetterHud placeholder 10개 등록 완료.");
        } catch (Exception e) {
            plugin.getLogger().warning("[RelicWars] BetterHud placeholder 등록 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Number placeholder 등록 헬퍼 (리플렉션).
     */
    private void registerNumber(Method addPlaceholder, Method hudOf, Method pfOf,
                                 Object container, Method uuidMethod,
                                 String name, Function<UUID, Number> valueFunc) throws Exception {
        // PlaceholderFunction.of(Function<HudPlayer, Number>)
        Function<Object, Number> hudPlayerFunc = hudPlayer -> {
            try {
                UUID uuid = (UUID) uuidMethod.invoke(hudPlayer);
                return valueFunc.apply(uuid);
            } catch (Exception e) { return 0; }
        };
        Object pf = pfOf.invoke(null, (Function<Object, Number>) hudPlayerFunc);
        Object placeholder = hudOf.invoke(null, pf);
        addPlaceholder.invoke(container, name, placeholder);
    }

    /**
     * Boolean placeholder 등록 헬퍼 (리플렉션).
     */
    private void registerBool(Method addPlaceholder, Method hudOf, Method pfOf,
                               Object container, Method uuidMethod,
                               String name, Function<UUID, Boolean> valueFunc) throws Exception {
        Function<Object, Boolean> hudPlayerFunc = hudPlayer -> {
            try {
                UUID uuid = (UUID) uuidMethod.invoke(hudPlayer);
                return valueFunc.apply(uuid);
            } catch (Exception e) { return false; }
        };
        Object pf = pfOf.invoke(null, (Function<Object, Boolean>) hudPlayerFunc);
        Object placeholder = hudOf.invoke(null, pf);
        addPlaceholder.invoke(container, name, placeholder);
    }

    public void unregister() {
        // BetterHud는 리로드 시 자동 정리됨
    }
}
