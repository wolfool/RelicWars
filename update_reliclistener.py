import os

filepath = "src/main/java/com/wolfool/relicwars/relic/RelicListener.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """        // 소문의 등불 GUI 처리
        if (event.getView().getTitle().equals("§5소문의 등불")) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInventory) {
                int slot = event.getSlot();
                if (slot == 3) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020Option2(player);
                } else if (slot == 5) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020Option3(player);
                }
            }
            return;
        }"""

replacement = """        // 소문의 등불 GUI 처리
        if (event.getView().getTitle().equals("§5소문의 등불")) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInventory) {
                int slot = event.getSlot();
                if (slot == 1) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020Option2(player); // 기존 Option2 (스캔)
                } else if (slot == 3) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020Option3(player); // 기존 Option3 (검색)
                } else if (slot == 5) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020OptionRandom(player); // 무작위 소문
                } else if (slot == 7) {
                    player.closeInventory();
                    plugin.getRelicAbilityHandler().execute020OptionPing(player); // 기만 전술
                }
            }
            return;
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Updated RelicListener GUI slots for #020")
else:
    # try regex fallback
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if "if (event.getView().getTitle().equals(\"§5소문의 등불\")) {" in line:
            end_idx = i + 13
            new_lines = lines[:i] + replacement.split('\n') + lines[end_idx+1:]
            with open(filepath, "w", encoding="utf-8") as f:
                f.write('\n'.join(new_lines))
            print("Updated RelicListener GUI slots for #020 with fallback")
            break
    else:
        print("Failed to find RelicListener target")
