import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """    // #020 소문의 등불 (4가지 옵션 중 하나를 선택하는 GUI 오픈)
    private void execute020(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("§5소문의 등불"));

        inv.setItem(3, createGuiItem(Material.ENDER_EYE, "§5[봉인 유물 스캔]", "§7현재 바닥에 봉인된", "§7유물들의 위치를 파악합니다."));
        inv.setItem(5, createGuiItem(Material.NAME_TAG, "§e[소유자 검색 모드]", "§7특정 번호의 유물을 누가", "§7가졌는지 알아볼 수 있는 검색 모드를 켭니다."));

        player.openInventory(inv);
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "§b[소문] 누군가 소문의 등불을 켰습니다.");
    }"""

replacement = """    // #020 소문의 등불 (4가지 옵션 중 하나를 선택하는 GUI 오픈)
    private void execute020(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("§5소문의 등불"));

        inv.setItem(1, createGuiItem(Material.ENDER_EYE, "§5[봉인 유물 스캔]", "§7현재 바닥에 봉인된", "§7유물들의 위치를 파악합니다."));
        inv.setItem(3, createGuiItem(Material.NAME_TAG, "§e[소유자 검색 모드]", "§7특정 번호의 유물을 누가", "§7가졌는지 알아볼 수 있는 검색 모드를 켭니다."));
        inv.setItem(5, createGuiItem(Material.PAPER, "§c[무작위 가짜 소문]", "§7월드 무작위 위치에", "§7가짜 소문을 브로드캐스트합니다."));
        inv.setItem(7, createGuiItem(Material.COMPASS, "§b[기만 전술]", "§7내가 가진 유물 번호를 입력하면", "§7해당 유물이 다른 곳에 있는 것처럼", "§7서버 전체에 가짜 소문을 퍼뜨립니다."));

        player.openInventory(inv);
        com.wolfool.relicwars.util.RumorUtil.broadcastRumor(player.getLocation(), "§b[소문] 누군가 소문의 등불을 켰습니다.");
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Updated execute020")
else:
    # Try Regex
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if "private void execute020(Player player) {" in line:
            end_idx = i + 9
            new_lines = lines[:i] + replacement.split('\n') + lines[end_idx+1:]
            with open(filepath, "w", encoding="utf-8") as f:
                f.write('\n'.join(new_lines))
            print("Updated execute020 with fallback")
            break
    else:
        print("Failed to find execute020")
