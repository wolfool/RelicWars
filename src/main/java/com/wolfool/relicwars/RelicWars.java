package com.wolfool.relicwars;

import com.wolfool.relicwars.manager.ConfigManager;
import com.wolfool.relicwars.manager.DatabaseManager;
import com.wolfool.relicwars.relic.RelicManager;
import com.wolfool.relicwars.combat.CombatManager;
import com.wolfool.relicwars.relic.SealedRelicManager;
import com.wolfool.relicwars.event.EventManager;
import com.wolfool.relicwars.team.TeamManager;
import com.wolfool.relicwars.boss.BossManager;
import com.wolfool.relicwars.ending.EndingManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * RelicWars 메인 플러그인 클래스.
 * 플러그인의 생명주기(onEnable/onDisable)를 관리하고,
 * 모든 매니저(Manager)를 초기화하는 진입점입니다.
 */
public final class RelicWars extends JavaPlugin {

    private static RelicWars instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RelicManager relicManager;
    private CombatManager combatManager;
    private SealedRelicManager sealedRelicManager;
    private EventManager eventManager;
    private TeamManager teamManager;
    private BossManager bossManager;
    private EndingManager endingManager;

    /**
     * 플러그인 인스턴스를 반환합니다. (싱글톤)
     */
    public static RelicWars getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // --- 1. 설정 파일 로드 ---
        configManager = new ConfigManager(this);
        configManager.load();
        getLogger().info("§a[RelicWars] config.yml 로드 완료.");

        // --- 2. 데이터베이스 초기화 ---
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().log(Level.SEVERE, "§c[RelicWars] 데이터베이스 초기화 실패! 플러그인을 비활성화합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("§a[RelicWars] SQLite 데이터베이스 연결 완료.");

        // --- 3. 매니저 등록 ---
        relicManager = new RelicManager(this);
        relicManager.initialize();
        
        sealedRelicManager = new SealedRelicManager(this);
        sealedRelicManager.initialize();
        
        combatManager = new CombatManager(this);
        combatManager.initialize();

        eventManager = new EventManager(this);
        eventManager.initialize();

        teamManager = new TeamManager(this);
        teamManager.initialize();

        bossManager = new BossManager(this);
        bossManager.initialize();

        endingManager = new EndingManager(this);
        endingManager.initialize();

        // --- 4. 이벤트/커맨드 등록 ---(Phase 7에서 구현) ---
        // TODO: /relic 명령어 등록
        // TODO: /team 명령어 등록

        getLogger().info("§a============================================");
        getLogger().info("§a  RelicWars v" + getDescription().getVersion() + " 활성화 완료!");
        getLogger().info("§a============================================");
    }

    @Override
    public void onDisable() {
        // --- 매니저 종료 ---
        if (endingManager != null) endingManager.shutdown();
        if (bossManager != null) bossManager.shutdown();
        if (teamManager != null) teamManager.shutdown();
        if (eventManager != null) eventManager.shutdown();
        if (combatManager != null) combatManager.shutdown();
        if (sealedRelicManager != null) sealedRelicManager.shutdown();
        if (relicManager != null) relicManager.shutdown();

        // --- 데이터베이스 연결 종료 ---
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("§c[RelicWars] 플러그인이 비활성화되었습니다.");
    }

    // --- Getter ---
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RelicManager getRelicManager() {
        return relicManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public SealedRelicManager getSealedRelicManager() {
        return sealedRelicManager;
    }

    public EventManager getEventManager() { return eventManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public BossManager getBossManager() { return bossManager; }
    public EndingManager getEndingManager() { return endingManager; }
}
