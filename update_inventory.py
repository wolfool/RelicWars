import os

filepath = "src/main/java/com/wolfool/relicwars/relic/RelicListener.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """            // 직접 커서로 놓기 방지
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInventory)) {
                ItemStack cursor = event.getCursor();
                if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage("§c[RelicWars] 유물은 자신의 인벤토리에만 보관할 수 있습니다!");
                    return;
                }
            }
        }
    }"""

replacement = """            // 직접 커서로 놓기 방지
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInventory)) {
                ItemStack cursor = event.getCursor();
                if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage("§c[RelicWars] 유물은 자신의 인벤토리에만 보관할 수 있습니다!");
                    return;
                }
            }
            
            // 핫바 스왑(숫자키) 방지
            if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInventory)) {
                    ItemStack hotbarItem = event.getView().getBottomInventory().getItem(event.getHotbarButton());
                    if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(hotbarItem)) {
                        event.setCancelled(true);
                        player.sendMessage("§c[RelicWars] 유물은 자신의 인벤토리에만 보관할 수 있습니다!");
                        return;
                    }
                }
            }
        }
        
        // 번들(꾸러미) 악용 방지 (자신의 인벤토리더라도 번들 안에는 넣을 수 없음)
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (cursor != null && cursor.getType() == org.bukkit.Material.BUNDLE && com.wolfool.relicwars.relic.RelicItemUtil.isRelic(current)) {
            event.setCancelled(true);
            player.sendMessage("§c[RelicWars] 유물을 꾸러미에 넣을 수 없습니다!");
            return;
        }
        if (current != null && current.getType() == org.bukkit.Material.BUNDLE && com.wolfool.relicwars.relic.RelicItemUtil.isRelic(cursor)) {
            event.setCancelled(true);
            player.sendMessage("§c[RelicWars] 유물을 꾸러미에 넣을 수 없습니다!");
            return;
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added inventory protections successfully.")
else:
    # Handle corrupted text format
    import re
    # Fallback using regex because of potential mojibake in original string
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if "if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(cursor)) {" in line and "event.setCancelled(true);" in lines[i+1]:
            # This is the cursor check. We need to find the end of the if block
            end_idx = i + 5
            if lines[end_idx].strip() == "}":
                # We found the end of the method
                new_lines = lines[:end_idx] + [
                    "            // 핫바 스왑(숫자키) 방지",
                    "            if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {",
                    "                if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInventory)) {",
                    "                    ItemStack hotbarItem = event.getView().getBottomInventory().getItem(event.getHotbarButton());",
                    "                    if (com.wolfool.relicwars.relic.RelicItemUtil.isRelic(hotbarItem)) {",
                    "                        event.setCancelled(true);",
                    "                        player.sendMessage(\"§c[RelicWars] 유물은 자신의 인벤토리에만 보관할 수 있습니다!\");",
                    "                        return;",
                    "                    }",
                    "                }",
                    "            }",
                    "        }",
                    "        ",
                    "        // 번들(꾸러미) 악용 방지 (자신의 인벤토리더라도 번들 안에는 넣을 수 없음)",
                    "        ItemStack cursorItem = event.getCursor();",
                    "        ItemStack currentItem = event.getCurrentItem();",
                    "        if (cursorItem != null && cursorItem.getType() == org.bukkit.Material.BUNDLE && com.wolfool.relicwars.relic.RelicItemUtil.isRelic(currentItem)) {",
                    "            event.setCancelled(true);",
                    "            player.sendMessage(\"§c[RelicWars] 유물을 꾸러미에 넣을 수 없습니다!\");",
                    "            return;",
                    "        }",
                    "        if (currentItem != null && currentItem.getType() == org.bukkit.Material.BUNDLE && com.wolfool.relicwars.relic.RelicItemUtil.isRelic(cursorItem)) {",
                    "            event.setCancelled(true);",
                    "            player.sendMessage(\"§c[RelicWars] 유물을 꾸러미에 넣을 수 없습니다!\");",
                    "            return;",
                    "        }",
                    "    }"
                ] + lines[end_idx+2:]
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write('\n'.join(new_lines))
                print("Added using fallback regex-like search.")
                break
    else:
        print("Could not find insertion point.")
