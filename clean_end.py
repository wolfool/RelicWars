import os

filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

target = """        player.sendMessage("§c[불멸의 심장] 이 유물은 직접 사용하는 것이 아닌, 소지 시 자동으로 발동되는 최상급 패시브입니다.");
    }
}"""

replacement = """}"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Cleaned up final execute005 garbage")
else:
    print("Target not found")
