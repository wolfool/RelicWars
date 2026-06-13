package com.wolfool.relicwars;

import com.wolfool.relicwars.manager.ConfigManager;
import com.wolfool.relicwars.manager.DatabaseManager;
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

        // --- 3. 매니저 등록 (Phase 2 이후 추가 예정) ---
        // TODO: RelicManager 등록
        // TODO: CombatManager 등록
        // TODO: TeamManager 등록
        // TODO: BossManager 등록
        // TODO: EventManager 등록

        // --- 4. 명령어 등록 (Phase 7에서 구현) ---
        // TODO: /relic 명령어 등록
        // TODO: /team 명령어 등록

        getLogger().info("§a============================================");
        getLogger().info("§a  RelicWars v" + getDescription().getVersion() + " 활성화 완료!");
        getLogger().info("§a============================================");
    }

    @Override
    public void onDisable() {
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
}
