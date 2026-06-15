import os

filepath = "RelicWars_Relics.md"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Fallback using regex because of potential mojibake
lines = content.split('\n')
for i, line in enumerate(lines):
    if "### #013" in line:
        end_idx = i + 7
        new_lines = lines[:i+1] + [
            "- **형태:** 뼈다귀 (BONE)",
            "- **로어:** *\"탐욕의 눈을 뜬 자, 세계의 모든 은밀함을 꿰뚫어본다.\"*",
            "- **획득:** 보스 — **탐욕의 눈** 처치. (유물 3개 이상 모일 때 스폰되는 이벤트 몹)",
            "- **능력 (쿨타임 45분):** 60초간 '초월적 탐지기' 발동. 서버 내 자신을 제외한 모든 플레이어들의 '소유한 유물의 총 개수'와 '대략적인 위치(반경 10블록 오차)'를 5초마다 실시간으로 탐지하여 채팅과 파티클로 알려줍니다.",
            "- **리스크:** 발동 즉시 서버의 모든 플레이어에게 \"누군가 탐욕의 눈을 떴습니다...\"라는 알림이 전송됩니다.",
            ""
        ] + lines[end_idx+1:]
        with open(filepath, "w", encoding="utf-8") as f:
            f.write('\n'.join(new_lines))
        print("Updated RelicWars_Relics.md for #013")
        break
else:
    print("Failed to find #013 in RelicWars_Relics.md")
