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
        loadTeamsFromDatabase();
        plugin.getLogger().info("§a[RelicWars] TeamManager 초기화 완료.");
    }

    @Override
    public void shutdown() {
        // 실시간 DB 저장이므로 별도 정리 생략
        plugin.getLogger().info("§a[RelicWars] TeamManager 종료.");
    }

    private void loadTeamsFromDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;
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
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return 0;
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
}
