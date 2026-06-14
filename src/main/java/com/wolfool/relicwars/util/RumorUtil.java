package com.wolfool.relicwars.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RumorUtil {

    private static final String[] DIRECTIONS = {
            "남", "남서", "서", "북서", "북", "북동", "동", "남동"
    };

    /**
     * 대상 위치(source)를 기준으로 모든 플레이어에게 상대적인 방향을 포함한 소문을 전송합니다.
     * @param source 사건이 발생한 위치
     * @param messageFormat 방향 문자열(%s)을 포함한 메시지 포맷 (예: "§b[소문] %s쪽에서 번개 소리가 들렸습니다.")
     */
    public static void broadcastRumor(Location source, String messageFormat) {
        if (source == null || source.getWorld() == null) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(source.getWorld())) continue;

            double dx = source.getX() - p.getLocation().getX();
            double dz = source.getZ() - p.getLocation().getZ();

            // 거리가 너무 가까우면(5블록 이내) '근처'로 표시
            if (dx * dx + dz * dz < 25.0) {
                p.sendMessage(Component.text(String.format(messageFormat, "바로 근처")));
                continue;
            }

            // 각도 계산 (-PI to PI)
            double angle = Math.atan2(dz, dx);
            
            // 마인크래프트 좌표계 기준 방향 매핑
            // x: 동(+), 서(-) / z: 남(+), 북(-)
            // Math.atan2(z, x) 결과에 따라 매핑
            double degrees = Math.toDegrees(angle);
            if (degrees < 0) {
                degrees += 360;
            }

            // 0도: 동, 90도: 남, 180도: 서, 270도: 북
            // 이를 DIRECTIONS 배열(남, 남서, 서, 북서, 북, 북동, 동, 남동) 인덱스에 맞게 매핑
            // 남(90) -> 0
            // 서(180) -> 2
            // 북(270) -> 4
            // 동(0/360) -> 6
            int index = (int) Math.round(((degrees - 90) / 45.0));
            if (index < 0) {
                index += 8;
            }
            index = index % 8;

            String dirString = DIRECTIONS[index];
            p.sendMessage(Component.text(String.format(messageFormat, dirString)));
        }
    }
}
