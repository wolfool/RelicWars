import os

# ============================================================
# Fix 1: cleanupPlayer() - 비행 해제 + 누락 Set 추가
# ============================================================
filepath = "src/main/java/com/wolfool/relicwars/relic/ability/RelicAbilityHandler.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

old_cleanup = """    public void cleanupPlayer(Player player) {
        java.util.UUID id = player.getUniqueId();
        active001Omega.remove(id);
        active005DamageReduction.remove(id);
        active029FallImmunity.remove(id);
        active027FireImmunity.remove(id);
        active025FastRevive.remove(id);
        active023Marked.remove(id);
        active021Duel.remove(id);
        active020ScanMode.remove(id);
        active015Casting.remove(id);
        active010EMP.remove(id);
        active008Shadow.remove(id);
        active006Leap.remove(id);
        active003TrackerWait.remove(id);
        
        java.util.Iterator<Map.Entry<java.util.UUID, java.util.UUID>> it = active021Duel.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<java.util.UUID, java.util.UUID> entry = it.next();
            if (entry.getValue().equals(id)) {
                it.remove();
            }
        }
    }"""

new_cleanup = """    public void cleanupPlayer(Player player) {
        java.util.UUID id = player.getUniqueId();
        
        // #001 비행 모드 해제 (반드시 Set 제거 전에 실행)
        if (active001Omega.contains(id)) {
            if (player.isOnline() && player.getGameMode() != org.bukkit.GameMode.CREATIVE 
                    && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
        
        active001Omega.remove(id);
        active005DamageReduction.remove(id);
        active029FallImmunity.remove(id);
        active027FireImmunity.remove(id);
        active025FastRevive.remove(id);
        active023Marked.remove(id);
        active021Duel.remove(id);
        active020ScanMode.remove(id);
        active020PingMode.remove(id);
        active015Casting.remove(id);
        active010EMP.remove(id);
        active008Shadow.remove(id);
        active006Leap.remove(id);
        active003TrackerWait.remove(id);
        cooldown005.remove(id);
        
        // 결투 대상에서도 제거
        java.util.Iterator<Map.Entry<java.util.UUID, java.util.UUID>> it = active021Duel.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<java.util.UUID, java.util.UUID> entry = it.next();
            if (entry.getValue().equals(id)) {
                it.remove();
            }
        }
    }"""

if old_cleanup in content:
    content = content.replace(old_cleanup, new_cleanup)
    print("[Fix 1] cleanupPlayer 비행 해제 + 누락 Set 추가 완료")
else:
    print("[Fix 1] 실패: cleanupPlayer를 찾을 수 없음")

# ============================================================
# Fix 2: execute005 전용 Set 분리 (active029FallImmunity 재사용 제거)
# ============================================================
old_005 = '        active029FallImmunity.add(id); // 임시로 같은 Set 활용 (별도 Set 필요하지만 MVP)'
new_005 = '        active005DamageReduction.add(id); // 전용 Set 사용'

if old_005 in content:
    content = content.replace(old_005, new_005)
    print("[Fix 2] execute005 전용 Set 분리 완료")
else:
    print("[Fix 2] 실패: execute005 Set 재사용 코드를 찾을 수 없음")

# ============================================================
# Fix 3: pending019Relic -> Map<UUID, Item> 변경
# ============================================================
old_pending = '    private org.bukkit.entity.Item pending019Relic;'
new_pending = '    private final java.util.Map<java.util.UUID, org.bukkit.entity.Item> pending019Relic = new java.util.HashMap<>();'

if old_pending in content:
    content = content.replace(old_pending, new_pending)
    # pending019Relic 사용처 변경
    content = content.replace(
        '        pending019Relic = nearest;',
        '        pending019Relic.put(player.getUniqueId(), nearest);'
    )
    content = content.replace(
        '        if (pending019Relic == null || !pending019Relic.isValid()) {',
        '        org.bukkit.entity.Item pRelic = pending019Relic.get(player.getUniqueId());\n        if (pRelic == null || !pRelic.isValid()) {'
    )
    content = content.replace(
        '        plugin.getSealedRelicManager().reduceSealTime(pending019Relic, 0.5);',
        '        plugin.getSealedRelicManager().reduceSealTime(pRelic, 0.5);'
    )
    content = content.replace(
        '        player.sendMessage("§d[봉인의 바늘] " + pending019Relic.getName() + "의 봉인 시간을 §l절반§d으로 단축했습니다!");',
        '        player.sendMessage("§d[봉인의 바늘] " + pRelic.getName() + "의 봉인 시간을 §l절반§d으로 단축했습니다!");'
    )
    content = content.replace(
        '        pending019Relic = null;',
        '        pending019Relic.remove(player.getUniqueId());'
    )
    content = content.replace(
        '        plugin.getSealedRelicManager().reduceSealTime(pending019Relic, 2.0);',
        '        plugin.getSealedRelicManager().reduceSealTime(pRelic, 2.0);'
    )
    content = content.replace(
        '        player.sendMessage("§d[봉인의 바늘] " + pending019Relic.getName() + "의 봉인 시간을 §l2배§d로 연장했습니다!");',
        '        player.sendMessage("§d[봉인의 바늘] " + pRelic.getName() + "의 봉인 시간을 §l2배§d로 연장했습니다!");'
    )
    print("[Fix 3] pending019Relic Map 변경 완료")
else:
    print("[Fix 3] 실패: pending019Relic을 찾을 수 없음")

# ============================================================
# Fix 4: getAttribute NPE 안전 처리 (RelicAbilityHandler)
# ============================================================
old_attr = 'player.getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_MAX_HEALTH")).getValue()'
new_attr = 'java.util.Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue()'

if old_attr in content:
    content = content.replace(old_attr, new_attr)
    print("[Fix 4] getAttribute valueOf -> MAX_HEALTH + NPE 보호 완료")
else:
    print("[Fix 4] 실패: valueOf 패턴을 찾을 수 없음")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print(f"[RelicAbilityHandler] 저장 완료")

# ============================================================
# Fix 5: #001 벼락 -> setHealth(0) 대신 다운 시스템 경유
# ============================================================
filepath2 = "src/main/java/com/wolfool/relicwars/relic/RelicListener.java"
with open(filepath2, "r", encoding="utf-8") as f:
    content2 = f.read()

old_lightning = """                                target.setHealth(0.0);
                                target.sendMessage("§4[심판] 태초의 지배자가 내린 벼락에 맞아 즉사했습니다.");"""

new_lightning = """                                target.sendMessage("§4[심판] 태초의 지배자가 내린 벼락에 맞았습니다!");
                                // 다운 시스템 경유: 높은 데미지를 입혀서 다운 판정을 거침
                                target.damage(9999.0, player);"""

if old_lightning in content2:
    content2 = content2.replace(old_lightning, new_lightning)
    print("[Fix 5] #001 벼락 다운 시스템 경유 수정 완료")
else:
    print("[Fix 5] 실패: 벼락 코드를 찾을 수 없음")

with open(filepath2, "w", encoding="utf-8") as f:
    f.write(content2)
print(f"[RelicListener] 저장 완료")

# ============================================================
# Fix 6: extractStealDrop -> shuffle 대신 내림차순 정렬
# ============================================================
filepath3 = "src/main/java/com/wolfool/relicwars/relic/RelicManager.java"
with open(filepath3, "r", encoding="utf-8") as f:
    content3 = f.read()

old_shuffle = '        java.util.Collections.shuffle(relics);'
new_sort = """        // 가장 높은 번호(약한) 유물부터 강탈당하도록 내림차순 정렬
        relics.sort((a, b) -> {
            int numA = RelicItemUtil.getRelicNumber(a);
            int numB = RelicItemUtil.getRelicNumber(b);
            return Integer.compare(numB, numA); // 내림차순: 높은 번호(약한) 먼저
        });"""

if old_shuffle in content3:
    content3 = content3.replace(old_shuffle, new_sort)
    print("[Fix 6] extractStealDrop 내림차순 정렬 수정 완료")
else:
    print("[Fix 6] 실패: shuffle 코드를 찾을 수 없음")

with open(filepath3, "w", encoding="utf-8") as f:
    f.write(content3)
print(f"[RelicManager] 저장 완료")

print("\n========== 모든 수정 완료 ==========")
