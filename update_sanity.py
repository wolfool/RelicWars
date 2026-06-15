import os

filepath = "src/main/java/com/wolfool/relicwars/sanity/SanityManager.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """    public void removeSanity(org.bukkit.entity.Player p, int amount) { setSanity(p, getSanity(p) - amount); }
}"""

replacement = """    public void removeSanity(org.bukkit.entity.Player p, int amount) { setSanity(p, getSanity(p) - amount); }

    /**
     * 특정 플레이어의 정신력을 비동기로 DB에 저장하고 메모리에서 제거합니다. (종료 시 사용)
     */
    public void saveAndRemovePlayerAsync(org.bukkit.entity.Player p) {
        if (!sanityMap.containsKey(p.getUniqueId())) return;
        final int sanity = sanityMap.get(p.getUniqueId());
        sanityMap.remove(p.getUniqueId());
        
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn == null) return;
            try {
                String query = \"\"\"
                    INSERT INTO players (uuid, name, sanity) VALUES (?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET sanity = excluded.sanity
                \"\"\";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, p.getUniqueId().toString());
                    pstmt.setString(2, p.getName());
                    pstmt.setInt(3, sanity);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("정신력 저장 실패: " + e.getMessage());
            }
        });
    }
}"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added saveAndRemovePlayerAsync")
else:
    print("Failed to find SanityManager end target")
