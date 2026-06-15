import os

filepath = "src/main/java/com/wolfool/relicwars/sanity/SanityListener.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {"""

replacement = """    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        // 플레이어 종료 시 정신력 비동기 저장 및 맵 정리
        plugin.getSanityManager().saveAndRemovePlayerAsync(event.getPlayer());
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added PlayerQuitEvent to SanityListener")
else:
    print("Failed to find SanityListener target")
