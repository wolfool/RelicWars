package com.wolfool.relicwars.event;

import com.wolfool.relicwars.RelicWars;
import com.wolfool.relicwars.relic.RelicDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 방치된 유물(상자에 들어있거나 바닥에 버려진)을 찾아내어 
 * 경과 시간에 따라 점진적으로 구체적인 위치 힌트를 방송합니다.
 */
public class ForgottenRelicTask extends BukkitRunnable {

    private final RelicWars plugin;

    public ForgottenRelicTask(RelicWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // 비동기 스레드에서 실행됨
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null || conn.isClosed()) return;

            // state 가 'dropped' 인 유물들 검색
            String query = "SELECT relic_number, location_world, location_x, location_y, location_z, dropped_at FROM relic_ownership WHERE state = 'dropped'";
            try (PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                long now = System.currentTimeMillis();

                while (rs.next()) {
                    int relicNum = rs.getInt("relic_number");
                    long droppedAt = rs.getLong("dropped_at");
                    String world = rs.getString("location_world");
                    double x = rs.getDouble("location_x");
                    double y = rs.getDouble("location_y");
                    double z = rs.getDouble("location_z");

                    long minutesElapsed = (now - droppedAt) / (1000 * 60);

                    // 1분 간격으로 체크하므로, 정확히 30, 60, 90, 120분이 되었을 때 딱 한 번 방송하도록 조건 설정
                    if (minutesElapsed == 30) {
                        broadcastHint(relicNum, world, x, y, z, 1);
                    } else if (minutesElapsed == 60) {
                        broadcastHint(relicNum, world, x, y, z, 2);
                    } else if (minutesElapsed == 90) {
                        broadcastHint(relicNum, world, x, y, z, 3);
                    } else if (minutesElapsed == 120) {
                        broadcastHint(relicNum, world, x, y, z, 4);
                    } else if (minutesElapsed > 120 && minutesElapsed % 30 == 0) {
                        // 120분 이후에는 30분마다 계속 제일 강력한 힌트(4단계) 방송
                        broadcastHint(relicNum, world, x, y, z, 4);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("방치된 유물 체크 중 데이터베이스 오류: " + e.getMessage());
        }
    }

    private void broadcastHint(int relicNum, String world, double x, double y, double z, int stage) {
        RelicDefinition def = RelicDefinition.getByNumber(relicNum);
        if (def == null || world == null) return;

        String worldName = world.contains("nether") ? "네더" : (world.contains("end") ? "엔드" : "오버월드");
        String message = "";

        switch (stage) {
            case 1:
                // 30분: 방향 힌트
                message = "§e[소문] " + worldName + " 어딘가에 " + def.getTierColor() + def.getName() + " §e유물이 방치되어 있다는 소문이 돕니다.";
                break;
            case 2:
                // 60분: 1000 단위 흐림
                int blur1000X = (int) (Math.round(x / 1000) * 1000);
                int blur1000Z = (int) (Math.round(z / 1000) * 1000);
                message = "§e[소문] " + worldName + " X: " + blur1000X + ", Z: " + blur1000Z + " 부근에서 " + def.getTierColor() + def.getName() + " §e유물의 강한 기운이 느껴집니다.";
                break;
            case 3:
                // 90분: 500 단위 흐림
                int blur500X = (int) (Math.round(x / 500) * 500);
                int blur500Z = (int) (Math.round(z / 500) * 500);
                message = "§e[소문] " + worldName + " X: " + blur500X + ", Z: " + blur500Z + " 부근에 " + def.getTierColor() + def.getName() + " §e유물이 잠들어 있습니다.";
                break;
            case 4:
            default:
                // 120분: 50단위 흐림 + 정확한 Y좌표 (파묻기 카운터)
                int blur50X = (int) (Math.round(x / 50) * 50);
                int blur50Z = (int) (Math.round(z / 50) * 50);
                int exactY = (int) y;
                message = "§6[강력한 소문] §e" + worldName + " X: " + blur50X + ", Z: " + blur50Z + " 부근, §c높이(Y): " + exactY + " §e에 " + def.getTierColor() + def.getName() + " §e유물이 봉인되어 있습니다!";
                break;
        }

        Bukkit.broadcast(Component.text(message));
    }
}
