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
                    dropped_world TEXT,
                    dropped_x REAL,
                    dropped_y REAL,
                    dropped_z REAL,
                    dropped_at INTEGER
                )
            """);

            // 기존 DB 마이그레이션: location_* → dropped_* 컬럼 변환
            migrateColumns(stmt);

            plugin.getLogger().info("§a[RelicWars] 데이터베이스 테이블 초기화 완료.");
        }
    }

    /**
     * 기존 DB의 location_* 컬럼을 dropped_*로 마이그레이션합니다.
     * SQLite는 ALTER TABLE RENAME COLUMN을 지원하므로 컬럼명을 직접 변경합니다.
     * 이미 dropped_* 컬럼이 있으면 아무것도 하지 않습니다.
     */
    private void migrateColumns(Statement stmt) {
        try {
            // dropped_world 컬럼이 이미 존재하는지 확인
            var rs = stmt.executeQuery("PRAGMA table_info(relic_ownership)");
            boolean hasDroppedWorld = false;
            boolean hasLocationWorld = false;
            while (rs.next()) {
                String colName = rs.getString("name");
                if ("dropped_world".equals(colName)) hasDroppedWorld = true;
                if ("location_world".equals(colName)) hasLocationWorld = true;
            }
            rs.close();

            if (hasDroppedWorld) return; // 이미 마이그레이션 완료

            if (hasLocationWorld) {
                // 구 스키마 → 신 스키마 컬럼명 변경
                stmt.executeUpdate("ALTER TABLE relic_ownership RENAME COLUMN location_world TO dropped_world");
                stmt.executeUpdate("ALTER TABLE relic_ownership RENAME COLUMN location_x TO dropped_x");
                stmt.executeUpdate("ALTER TABLE relic_ownership RENAME COLUMN location_y TO dropped_y");
                stmt.executeUpdate("ALTER TABLE relic_ownership RENAME COLUMN location_z TO dropped_z");
                plugin.getLogger().info("§a[RelicWars] DB 마이그레이션 완료: location_* → dropped_*");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "DB 마이그레이션 실패 (무시 가능)", e);
        }
    }

    /**
     * 유물의 현재 상태(보유 중인지, 바닥/상자에 방치되었는지)와 위치를 업데이트합니다.
     */
    public synchronized void updateRelicState(int relicNumber, String state, String ownerUuid, org.bukkit.Location loc) {
        if (connection == null) return;
        
        String query = """
            INSERT INTO relic_ownership (relic_number, owner_uuid, state, dropped_world, dropped_x, dropped_y, dropped_z, dropped_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(relic_number) DO UPDATE SET 
                owner_uuid = excluded.owner_uuid,
                state = excluded.state,
                dropped_world = excluded.dropped_world,
                dropped_x = excluded.dropped_x,
                dropped_y = excluded.dropped_y,
                dropped_z = excluded.dropped_z,
                dropped_at = CASE WHEN excluded.state = 'sealed' THEN excluded.dropped_at 
                                  ELSE relic_ownership.dropped_at END
        """;

        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, relicNumber);
            pstmt.setString(2, ownerUuid);
            pstmt.setString(3, state);
            if (loc != null && loc.getWorld() != null) {
                pstmt.setString(4, loc.getWorld().getName());
                pstmt.setDouble(5, loc.getX());
                pstmt.setDouble(6, loc.getY());
                pstmt.setDouble(7, loc.getZ());
            } else {
                pstmt.setString(4, null);
                pstmt.setDouble(5, 0);
                pstmt.setDouble(6, 0);
                pstmt.setDouble(7, 0);
            }
            pstmt.setLong(8, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "유물 상태 업데이트 실패: #" + relicNumber, e);
        }
    }

    /**
     * 유물의 현재 소유자 UUID를 조회합니다.
     * @param relicNumber 유물 번호
     * @return 소유자의 UUID 문자열 (소유자가 없거나 스폰되지 않았으면 null)
     */
    public synchronized String getRelicOwner(int relicNumber) {
        String query = "SELECT owner_uuid FROM relic_ownership WHERE relic_number = ? AND state = 'held'";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, relicNumber);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("owner_uuid");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "유물 소유자 조회 실패: #" + relicNumber, e);
        }
        return null;
    }

    /**
     * 유물의 현재 상태(state)를 조회합니다.
     */
    public synchronized String getRelicState(int relicNumber) {
        String query = "SELECT state FROM relic_ownership WHERE relic_number = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, relicNumber);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("state");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "유물 상태 조회 실패: #" + relicNumber, e);
        }
        return "unspawned";
    }

    /**
     * 유물이 unspawned 상태일 때만 원자적으로 상태를 변경합니다.
     * check-and-update를 하나의 synchronized 메서드에서 수행하여 Race Condition 방지.
     * @return 성공 여부
     */
    public synchronized boolean tryClaimRelic(int relicNumber, String state, String ownerUuid, org.bukkit.Location loc) {
        String currentState = getRelicState(relicNumber);
        if (!"unspawned".equals(currentState)) {
            return false; // 이미 다른 곳에서 스폰됨
        }
        updateRelicState(relicNumber, state, ownerUuid, loc);
        return true;
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
