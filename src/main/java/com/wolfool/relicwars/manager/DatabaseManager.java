package com.wolfool.relicwars.manager;

import com.wolfool.relicwars.RelicWars;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * SQLite 데이터베이스 연결 및 테이블 초기화를 관리하는 매니저.
 * 유저 데이터, 팀 정보, 유물 스폰 이력 등을 영속적으로 저장합니다.
 */
public class DatabaseManager {

    private final RelicWars plugin;
    private Connection connection;

    public DatabaseManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    /**
     * SQLite 데이터베이스에 연결하고 필수 테이블을 생성합니다.
     * @return 초기화 성공 여부
     */
    public boolean initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "relicwars.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            createTables();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite 연결 실패", e);
            return false;
        }
    }

    /**
     * 필수 테이블들을 생성합니다. (이미 존재하면 무시)
     */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            // 유저 데이터 테이블
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    sanity INTEGER DEFAULT 100,
                    team_id TEXT,
                    last_login INTEGER
                )
            """);

            // 팀 테이블
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS teams (
                    team_id TEXT PRIMARY KEY,
                    leader_uuid TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """);

            // 유물 스폰 이력 (30개 유물이 모두 등장했는지 추적)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS relic_spawns (
                    relic_number INTEGER PRIMARY KEY,
                    first_spawned_at INTEGER,
                    has_spawned INTEGER DEFAULT 0
                )
            """);

            // 유물 소유 현황 (실시간 유물 위치/소유자 추적)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS relic_ownership (
                    relic_number INTEGER PRIMARY KEY,
                    owner_uuid TEXT,
                    state TEXT DEFAULT 'unspawned',
                    location_world TEXT,
                    location_x REAL,
                    location_y REAL,
                    location_z REAL,
                    dropped_at INTEGER
                )
            """);

            plugin.getLogger().info("§a[RelicWars] 데이터베이스 테이블 초기화 완료.");
        }
    }

    /**
     * 데이터베이스 연결을 반환합니다.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * 데이터베이스 연결을 안전하게 종료합니다.
     */
    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("§a[RelicWars] 데이터베이스 연결 종료.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "데이터베이스 연결 종료 중 오류 발생", e);
            }
        }
    }
}
