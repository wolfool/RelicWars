package com.wolfool.relicwars.team;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TeamManager implements Manager {

    private final RelicWars plugin;
    
    // Player UUID -> Team ID
    private final Map<UUID, String> playerTeams = new HashMap<>();
    
    // Team ID -> List of Member UUIDs
    private final Map<String, List<UUID>> teamMembers = new HashMap<>();
    
    // Team ID -> Expiration Time Millis
    private final Map<String, Long> teamExpireTimes = new HashMap<>();

    // Target UUID -> Inviter UUID
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final Map<UUID, BukkitTask> inviteTasks = new HashMap<>();
    private BukkitTask expireTask;

    public TeamManager(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        loadTeamsFromDB();
        startExpirationTask();
        plugin.getLogger().info("§a[RelicWars] TeamManager 초기화 완료.");
    }

    @Override
    public void shutdown() {
        if (expireTask != null) expireTask.cancel();
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
                    
                    List<UUID> list = teamMembers.get(teamId);
                    if (list == null) {
                        list = new ArrayList<>();
                        teamMembers.put(teamId, list);
                    }
                    list.add(uuid);
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

    public int getTeamRelicCount(String teamId) {
        int total = 0;
        for (UUID memberUuid : getTeamMembers(teamId)) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
                total += plugin.getRelicManager().countPlayerRelics(p);
            } else {
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

    public void invitePlayer(Player inviter, Player target) {
        if (hasTeam(target)) {
            inviter.sendMessage("§c[RelicWars] 상대방이 이미 팀에 속해 있습니다.");
            return;
        }

        String teamId = getTeamId(inviter);
        int currentMembers = teamId == null ? 1 : getTeamMembers(teamId).size();
        if (currentMembers >= plugin.getConfigManager().getTeamMaxMembers()) {
            inviter.sendMessage("§c[RelicWars] 팀의 최대 인원(" + plugin.getConfigManager().getTeamMaxMembers() + "명)을 초과할 수 없습니다.");
            return;
        }

        int inviterRelics = teamId == null ? plugin.getRelicManager().countPlayerRelics(inviter) : getTeamRelicCount(teamId);
        int targetRelics = plugin.getRelicManager().countPlayerRelics(target);
        if (inviterRelics + targetRelics > plugin.getConfigManager().getTeamMaxTotalRelics()) {
            inviter.sendMessage("§c[RelicWars] 팀 결성에 필요한 유물 합계 제한(" + plugin.getConfigManager().getTeamMaxTotalRelics() + "개)을 초과합니다.");
            return;
        }

        if (pendingInvites.containsKey(target.getUniqueId())) {
            inviter.sendMessage("§c[RelicWars] 상대방이 이미 다른 초대를 받고 있습니다.");
            return;
        }

        pendingInvites.put(target.getUniqueId(), inviter.getUniqueId());
        
        inviter.sendMessage("§a[RelicWars] " + target.getName() + "님에게 팀 초대를 보냈습니다.");
        target.sendMessage("§a[RelicWars] " + inviter.getName() + "님으로부터 팀 초대가 도착했습니다!");
        target.sendMessage("§a[RelicWars] 60초 내에 /team accept 명령어로 수락하세요.");

        int expireSeconds = plugin.getConfigManager().getTeamInviteExpireSeconds();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInvites.remove(target.getUniqueId()) != null) {
                if (target.isOnline()) target.sendMessage("§c[RelicWars] 팀 초대가 만료되었습니다.");
                if (inviter.isOnline()) inviter.sendMessage("§c[RelicWars] " + target.getName() + "님에 대한 팀 초대가 만료되었습니다.");
            }
        }, expireSeconds * 20L);
        inviteTasks.put(target.getUniqueId(), task);
    }

    public void acceptInvite(Player target) {
        UUID inviterUuid = pendingInvites.remove(target.getUniqueId());
        BukkitTask task = inviteTasks.remove(target.getUniqueId());
        if (task != null) task.cancel();

        if (inviterUuid == null) {
            target.sendMessage("§c[RelicWars] 받은 팀 초대가 없습니다.");
            return;
        }

        Player inviter = Bukkit.getPlayer(inviterUuid);
        if (inviter == null || !inviter.isOnline()) {
            target.sendMessage("§c[RelicWars] 초대한 플레이어가 오프라인입니다.");
            return;
        }

        if (hasTeam(target)) {
            target.sendMessage("§c[RelicWars] 이미 팀에 속해 있습니다.");
            return;
        }

        String teamId = getTeamId(inviter);
        int currentMembers = teamId == null ? 1 : getTeamMembers(teamId).size();
        if (currentMembers >= plugin.getConfigManager().getTeamMaxMembers()) {
            target.sendMessage("§c[RelicWars] 초대한 사람의 팀이 가득 찼습니다.");
            return;
        }

        int inviterRelics = teamId == null ? plugin.getRelicManager().countPlayerRelics(inviter) : getTeamRelicCount(teamId);
        int targetRelics = plugin.getRelicManager().countPlayerRelics(target);
        if (inviterRelics + targetRelics > plugin.getConfigManager().getTeamMaxTotalRelics()) {
            target.sendMessage("§c[RelicWars] 팀 결성에 필요한 유물 합계 제한을 초과합니다.");
            return;
        }

        if (teamId == null) {
            // 새 팀 생성
            teamId = UUID.randomUUID().toString().substring(0, 8);
            playerTeams.put(inviter.getUniqueId(), teamId);
            
            List<UUID> list = new ArrayList<>();
            list.add(inviter.getUniqueId());
            teamMembers.put(teamId, list);
            
            // 2시간 만료
            teamExpireTimes.put(teamId, System.currentTimeMillis() + (2L * 60 * 60 * 1000));
            
            saveTeamToDB(inviter.getUniqueId(), teamId);
        }

        playerTeams.put(target.getUniqueId(), teamId);
        teamMembers.get(teamId).add(target.getUniqueId());
        saveTeamToDB(target.getUniqueId(), teamId);

        target.sendMessage("§a[RelicWars] " + inviter.getName() + "님의 팀에 가입했습니다!");
        for (UUID member : getTeamMembers(teamId)) {
            Player p = Bukkit.getPlayer(member);
            if (p != null && p.isOnline() && !p.equals(target)) {
                p.sendMessage("§a[RelicWars] " + target.getName() + "님이 팀에 가입했습니다!");
            }
        }
    }

    public void leaveTeam(Player player) {
        String teamId = getTeamId(player);
        if (teamId == null) {
            player.sendMessage("§c[RelicWars] 소속된 팀이 없습니다.");
            return;
        }

        playerTeams.remove(player.getUniqueId());
        
        List<UUID> members = teamMembers.get(teamId);
        if (members != null) {
            members.remove(player.getUniqueId());
            if (members.size() <= 1) { // 1명 이하로 남으면 팀 폭파
                for (UUID remaining : members) {
                    playerTeams.remove(remaining);
                    removeTeamFromDB(remaining);
                    Player p = Bukkit.getPlayer(remaining);
                    if (p != null && p.isOnline()) {
                        p.sendMessage("§c[RelicWars] 팀원이 모두 떠나 팀이 해체되었습니다.");
                    }
                }
                teamMembers.remove(teamId);
            } else {
                for (UUID remaining : members) {
                    Player p = Bukkit.getPlayer(remaining);
                    if (p != null && p.isOnline()) {
                        p.sendMessage("§c[RelicWars] " + player.getName() + "님이 팀을 탈퇴했습니다.");
                    }
                }
            }
        }
        
        removeTeamFromDB(player.getUniqueId());
        player.sendMessage("§a[RelicWars] 팀에서 탈퇴했습니다.");
    }

    private void saveTeamToDB(UUID uuid, String teamId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return;
            try {
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

    private void removeTeamFromDB(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return;
            try {
                String query = "UPDATE players SET team_id = NULL WHERE uuid = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("팀 DB 삭제 실패: " + e.getMessage());
            }
        });
    }

    private void startExpirationTask() {
        expireTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            List<String> toDisband = new ArrayList<>();

            for (Map.Entry<String, Long> entry : teamExpireTimes.entrySet()) {
                String teamId = entry.getKey();
                long expireTime = entry.getValue();

                if (now >= expireTime) {
                    toDisband.add(teamId);
                } else if (expireTime - now <= 5 * 60 * 1000 && expireTime - now > 4 * 60 * 1000) {
                    for (UUID member : getTeamMembers(teamId)) {
                        Player p = Bukkit.getPlayer(member);
                        if (p != null && p.isOnline()) {
                            p.sendMessage("§e[RelicWars] 팀 유지 시간이 5분 남았습니다. 5분 후 자동으로 팀이 해체됩니다.");
                        }
                    }
                }
            }

            for (String teamId : toDisband) {
                teamExpireTimes.remove(teamId);
                List<UUID> members = teamMembers.remove(teamId);
                if (members != null) {
                    for (UUID member : members) {
                        playerTeams.remove(member);
                        removeTeamFromDB(member);
                        Player p = Bukkit.getPlayer(member);
                        if (p != null && p.isOnline()) {
                            p.sendMessage("§c[RelicWars] 팀 유지 시간(2시간)이 만료되어 팀이 자동 해체되었습니다.");
                        }
                    }
                }
            }
        }, 20L * 60, 20L * 60);
    }
}
