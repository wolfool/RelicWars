package com.wolfool.relicwars.team;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 듀오(2인) 팀 시스템 매니저.
 * DB와 연동하여 플레이어의 소속 팀을 관리합니다.
 */
public class TeamManager implements Manager {

    private final RelicWars plugin;
    
    // Player UUID -> Team ID
    private final Map<UUID, String> playerTeams = new HashMap<>();
    
    // Team ID -> List of Member UUIDs
    private final Map<String, List<UUID>> teamMembers = new HashMap<>();

    public TeamManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        loadTeamsFromDB();
        plugin.getLogger().info("§a[RelicWars] TeamManager 초기화 완료.");
    }

    @Override
    public void shutdown() {
        // 실시간 DB 저장이므로 별도 정리 생략
        plugin.getLogger().info("§a[RelicWars] TeamManager 종료.");
    }

    private void loadTeamsFromDB() {
        Connection conn = plugin.getDatabaseManager().getConnection();
        if (conn == null) return;

        try {
            String query = "SELECT uuid, team_id FROM players WHERE team_id IS NOT NULL";
            try (PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String teamId = rs.getString("team_id");
                    playerTeams.put(uuid, teamId);
                    teamMembers.computeIfAbsent(teamId, k -> new ArrayList<>()).add(uuid);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("팀 데이터 로드 실패: " + e.getMessage());
        }
    }

    public boolean hasTeam(Player player) {
        return playerTeams.containsKey(player.getUniqueId());
    }

    public String getTeamId(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public List<UUID> getTeamMembers(String teamId) {
        return teamMembers.getOrDefault(teamId, Collections.emptyList());
    }

    /**
     * 특정 팀이 보유한 모든 유물의 개수 합계를 반환합니다.
     */
    public int getTeamRelicCount(String teamId) {
        int total = 0;
        for (UUID memberUuid : getTeamMembers(teamId)) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
                total += plugin.getRelicManager().countPlayerRelics(p);
            } else {
                // 오프라인 유저의 유물은 DB에서 가져와야 하지만, MVP에서는 온라인 유저 기준(또는 DB 조회)으로 처리
                // 여기서는 간단히 DB 쿼리로 대체 가능하나 임시로 온라인 유저만 합산
                total += countRelicsFromDB(memberUuid.toString());
            }
        }
        return total;
    }

    private int countRelicsFromDB(String uuidStr) {
        int count = 0;
        Connection conn = plugin.getDatabaseManager().getConnection();
        if (conn == null) return 0;
        try {
            String query = "SELECT COUNT(*) FROM relic_ownership WHERE owner_uuid = ? AND state = 'held'";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, uuidStr);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) count = rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {}
        return count;
    }

    public boolean isSameTeam(Player p1, Player p2) {
        String t1 = getTeamId(p1);
        String t2 = getTeamId(p2);
        return t1 != null && t1.equals(t2);
    }

    /**
     * 두 플레이어를 새로운 팀으로 결성합니다.
     */
    public void createTeam(Player p1, Player p2) {
        String teamId = UUID.randomUUID().toString().substring(0, 8);
        
        playerTeams.put(p1.getUniqueId(), teamId);
        playerTeams.put(p2.getUniqueId(), teamId);
        
        teamMembers.put(teamId, Arrays.asList(p1.getUniqueId(), p2.getUniqueId()));
        
        saveTeamToDB(p1.getUniqueId(), teamId);
        saveTeamToDB(p2.getUniqueId(), teamId);
    }

    private void saveTeamToDB(UUID uuid, String teamId) {
        // 백그라운드 스레드에서 저장 권장되지만, MVP이므로 동기/간단한 비동기로 처리
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return;
            try {
                // 플레이어 데이터가 없을 수 있으므로 UPSERT
                String query = """
                    INSERT INTO players (uuid, team_id) VALUES (?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET team_id = excluded.team_id
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, teamId);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("팀 저장 실패: " + e.getMessage());
            }
        });
    }

    private void removeTeamFromDB(String teamId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return;
            try {
                String query = "DELETE FROM teams WHERE team_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, teamId);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("팀 DB 삭제 실패: " + e.getMessage());
            }
        });
    }
}
