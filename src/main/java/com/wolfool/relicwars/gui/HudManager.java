package com.wolfool.relicwars.gui;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import kr.toxicity.hud.api.BetterHudAPI;
import kr.toxicity.hud.api.placeholder.HudPlaceholder;
import kr.toxicity.hud.api.player.HudPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.function.Function;

public class HudManager implements Manager {

    private final RelicWars plugin;

    public HudManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        if (Bukkit.getPluginManager().getPlugin("BetterHud") != null) {
            registerPlaceholders();
            plugin.getLogger().info("§a[RelicWars] BetterHud 연동 완료.");
        } else {
            plugin.getLogger().warning("§e[RelicWars] BetterHud 플러그인을 찾을 수 없어 HUD 연동을 건너뜁니다.");
        }
    }

    @Override
    public void shutdown() {
        // BetterHud placeholders will be cleared automatically on plugin disable/reload
    }

    private void registerPlaceholders() {
        // 1. 정신력 (Sanity) Placeholder: %relicwars_sanity%
        BetterHudAPI.inst().getPlaceholderManager().getNumberContainer().addPlaceholder(
            "relicwars_sanity",
            HudPlaceholder.of((args, event) -> {
                return (Function<HudPlayer, Number>) hudPlayer -> {
                    Player p = hudPlayer.bukkitPlayer();
                    if (p == null || !p.isOnline()) return 0;
                    return plugin.getSanityManager().getSanity(p);
                };
            })
        );

        // 2. 다운 상태 확인 Placeholder: %relicwars_is_downed%
        BetterHudAPI.inst().getPlaceholderManager().getBooleanContainer().addPlaceholder(
            "relicwars_is_downed",
            HudPlaceholder.of((args, event) -> {
                return (Function<HudPlayer, Boolean>) hudPlayer -> {
                    Player p = hudPlayer.bukkitPlayer();
                    if (p == null || !p.isOnline()) return false;
                    return plugin.getCombatManager().isDowned(p);
                };
            })
        );
        
        // 3. 파티(팀) 이름 Placeholder: %relicwars_team_name%
        BetterHudAPI.inst().getPlaceholderManager().getStringContainer().addPlaceholder(
            "relicwars_team_name",
            HudPlaceholder.of((args, event) -> {
                return (Function<HudPlayer, String>) hudPlayer -> {
                    Player p = hudPlayer.bukkitPlayer();
                    if (p == null || !p.isOnline()) return "무소속";
                    String teamId = plugin.getTeamManager().getTeamId(p);
                    return teamId != null ? "팀 " + teamId : "솔로";
                };
            })
        );
    }
}
