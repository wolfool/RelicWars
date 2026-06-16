package com.wolfool.relicwars.relic;

import org.bukkit.Material;

/**
 * 31개 넘버링 유물(#000~#030)의 기본 정의 데이터.
 * 각 유물의 번호, 이름, 등급, 외형 아이템, 쿨타임(초)을 저장합니다.
 *
 * 등급 체계 (번호가 낮을수록 강함):
 *   1단계 (#025~#030) - 최하급 (정신력 소모 없음)
 *   2단계 (#019~#024) - 하급   (정신력 소모 없음)
 *   3단계 (#011~#018) - 중급   (#017~#015: 정신력 10, #018/#019: 2단계)
 *   4단계 (#006~#010) - 상급   (정신력 20 소모)
 *   5단계 (#001~#005) - 최상급 (정신력 30 소모)
 *   특수  (#000)      - 시즌 엔딩 유물
 */
public enum RelicDefinition {

    // === 특수 유물 ===
    RELIC_000(0, "왕좌의 핵", 6, Material.DRAGON_EGG, 0,
            "§5§l#000 §d왕좌의 핵",
            "§7시즌 엔딩 유물. 중앙 제단에 바치고 30분을 방어하라."),

    // === 5단계 (최상급, 정신력 30 소모) ===
    RELIC_001(1, "태초의 별", 5, Material.NETHER_STAR, 9000,
            "§6§l#001 §e태초의 별",
            "§760초간 '태초의 지배자' 상태. 모든 봉인 해제 + 무한 능력 연사."),
    RELIC_002(2, "탐욕의 적출자", 5, Material.GHAST_TEAR, 7200,
            "§6§l#002 §e탐욕의 적출자",
            "§7다운된 적의 유물을 0.5초 만에 즉시 적출하여 내 인벤토리로."),
    RELIC_003(3, "절대 좌표 나침반", 5, Material.RECOVERY_COMPASS, 2700,
            "§6§l#003 §e절대 좌표 나침반",
            "§7특정 유물의 실시간 절대 좌표를 3분간 추적한다."),
    RELIC_004(4, "폭풍의 왕관", 5, Material.TRIDENT, 1200,
            "§6§l#004 §e폭풍의 왕관",
            "§7반경 30블록에 15초간 파멸적 뇌우를 소환한다."),
    RELIC_005(5, "불멸의 심장", 5, Material.TOTEM_OF_UNDYING, 5400,
            "§6§l#005 §e불멸의 심장",
            "§7[패시브] 다운 직전 즉시 체력 100% 부활 + 황금 충격파."),

    // === 4단계 (상급, 정신력 20 소모) ===
    RELIC_006(6, "차원 도약석", 4, Material.ENDER_PEARL, 600,
            "§c§l#006 §4차원 도약석",
            "§730블록 순간이동 + 5초 내 복귀 시 모든 피해 무효화."),
    RELIC_007(7, "파수꾼의 돔", 4, Material.SHIELD, 1800,
            "§c§l#007 §4파수꾼의 돔",
            "§7반경 8블록 절대 방어막 15초. 적 진입/투사체 차단."),
    RELIC_008(8, "그림자 막", 4, Material.PHANTOM_MEMBRANE, 2400,
            "§c§l#008 §4그림자 막",
            "§73분간 팀 전체 탐지 면역 + 가짜 신호 3개 생성."),
    RELIC_009(9, "파괴자의 서", 4, Material.BOOK, 1200,
            "§c§l#009 §4파괴자의 서",
            "§750블록 내 봉인 유물의 봉인을 즉시 파괴한다."),
    RELIC_010(10, "충격 코어", 4, Material.END_CRYSTAL, 480,
            "§c§l#010 §4충격 코어",
            "§7광역 넉백 + 5초간 반경 20블록 모든 상호작용/유물 능력 차단."),

    // === 3단계 (중급, 정신력 10 소모) ===
    RELIC_011(11, "공명의 종", 3, Material.BELL, 1800,
            "§a§l#011 §2공명의 종",
            "§7300블록 내 모든 유물 보유자 위치+개수 강제 노출. 은신 관통."),
    RELIC_012(12, "약탈자의 장갑", 3, Material.LEATHER, 1500,
            "§a§l#012 §2약탈자의 장갑",
            "§710블록 내 적의 정신력 30 강탈 + 공포 연출."),
    RELIC_013(13, "탐욕의 뼈", 3, Material.BONE, 2700,
            "§a§l#013 §2탐욕의 뼈",
            "§7탐욕 보스를 적진에 배달. 마커 주변 적 위치 10초간 노출."),
    RELIC_014(14, "전장의 뿔", 3, Material.GOAT_HORN, 1500,
            "§a§l#014 §2전장의 뿔",
            "§760초간 팀 위치 공유 + 봉인 유물/다운된 적 방향 표시 + 이속 버프."),
    RELIC_015(15, "회수자의 갈고리", 3, Material.FISHING_ROD, 900,
            "§a§l#015 §2회수자의 갈고리",
            "§720블록 밖 봉인 유물을 낚아채 + 획득 시간 절반 단축."),
    RELIC_016(16, "감시의 방패", 3, Material.SPYGLASS, 1800,
            "§a§l#016 §2감시의 방패",
            "§7반경 80블록 감시 구역 5분. 적 진입/봉인 유물 출현/강탈 시도 경고."),
    RELIC_017(17, "왜곡의 닻", 3, Material.LODESTONE, 2400,
            "§a§l#017 §2왜곡의 닻",
            "§7공간 왜곡장. 다운 시 닻 위치로 즉시 텔레포트되어 확킬 회피."),

    // === 3단계 (중급, 정신력 10 소모) — #019~#011 ===
    RELIC_018(18, "흔적 렌즈", 3, Material.SPYGLASS, 720,
            "§a§l#018 §2흔적 렌즈",
            "§7200블록 내 유물 보유자의 발자국을 등급별 색상으로 추적."),
    RELIC_019(19, "봉인의 바늘", 3, Material.COMPASS, 1080,
            "§a§l#019 §2봉인의 바늘",
            "§7봉인 유물의 시간을 절반으로 단축/2배 연장 + 위치 추적."),
    RELIC_020(20, "소문의 등불", 2, Material.LANTERN, 1200,
            "§9§l#020 §1소문의 등불",
            "§7서버 전체 유물 정보 스캔. 미발견 유물/봉인 위치/소유자 확인."),
    RELIC_021(21, "결투자의 파편", 2, Material.IRON_SWORD, 1500,
            "§9§l#021 §1결투자의 파편",
            "§715블록 내 적과 강제 1:1 결투장 20초. 외부 진입/탈출 불가."),
    RELIC_022(22, "탐욕의 동전", 2, Material.GOLD_INGOT, 1200,
            "§9§l#022 §1탐욕의 동전",
            "§7진짜와 동일한 가짜 봉인 유물 트랩. 우클릭 시 구속+발광."),
    RELIC_023(23, "사냥꾼의 표식", 2, Material.BOW, 720,
            "§9§l#023 §1사냥꾼의 표식",
            "§760초간 사냥 표식. 벽 너머 발광 + 강탈 시간 10초→3초."),
    RELIC_024(24, "붉은 봉합", 2, Material.RED_DYE, 900,
            "§9§l#024 §1붉은 봉합",
            "§730블록 밖 다운 팀원을 내 위치로 순간이동 후 구조 시작."),

    // === 1단계 (최하급, 정신력 소모 없음) ===
    RELIC_025(25, "최후의 봉합", 1, Material.STRING, 900,
            "§7§l#025 §f최후의 봉합",
            "§730초 안개 + 구조 시간 8초→2초 + 부활 팀원 도주 버프."),
    RELIC_026(26, "어둠매듭", 1, Material.SCULK_SHRIEKER, 480,
            "§7§l#026 §f어둠매듭",
            "§710초 완벽 은신. 투명화+닉네임 숨김+발소리 제거+스컬크 무시."),
    RELIC_027(27, "용암의 눈", 1, Material.MAGMA_CREAM, 600,
            "§7§l#027 §f용암의 눈",
            "§715초 화염/용암 면역 + 용암 보행 + 불길 장벽 생성."),
    RELIC_028(28, "심해의 폐", 1, Material.HEART_OF_THE_SEA, 600,
            "§7§l#028 §f심해의 폐",
            "§73분 수중 무적 + 지상에서 물 소환 후 대쉬 이동기."),
    RELIC_029(29, "추락왕의 깃털", 1, Material.FEATHER, 300,
            "§7§l#029 §f추락왕의 깃털",
            "§715초 낙하 데미지 면역 + 공중 이단 점프 1회."),
    RELIC_030(30, "낙뢰의 심지", 1, Material.BLAZE_ROD, 360,
            "§7§l#030 §f낙뢰의 심지",
            "§7바라보는 곳에 1.5초 뒤 낙뢰. 높은 데미지+넉백+5초 발광.");

    private final int number;
    private final String name;
    private final int tier;
    private final Material material;
    private final int cooldownSeconds;
    private final String displayName;
    private final String loreDescription;

    RelicDefinition(int number, String name, int tier, Material material, int cooldownSeconds,
                    String displayName, String loreDescription) {
        this.number = number;
        this.name = name;
        this.tier = tier;
        this.material = material;
        this.cooldownSeconds = cooldownSeconds;
        this.displayName = displayName;
        this.loreDescription = loreDescription;
    }

    public int getNumber() { return number; }
    public String getName() { return name; }
    public int getTier() { return tier; }
    public Material getMaterial() { return material; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public String getDisplayName() { return displayName; }
    public String getLoreDescription() { return loreDescription; }

    // O(1) 조회를 위한 번호 → 정의 맵
    private static final java.util.Map<Integer, RelicDefinition> BY_NUMBER = new java.util.HashMap<>();
    static { for (RelicDefinition d : values()) BY_NUMBER.put(d.number, d); }

    /**
     * 유물 번호로 RelicDefinition을 찾습니다.
     * @param number 유물 번호 (0~30)
     * @return 해당 유물 정의, 없으면 null
     */
    public static RelicDefinition getByNumber(int number) {
        return BY_NUMBER.get(number);
    }

    /**
     * 등급 색상을 반환합니다.
     */
    public String getTierColor() {
        return switch (tier) {
            case 6 -> "§5";  // 특수 (보라)
            case 5 -> "§6";  // 최상급 (금)
            case 4 -> "§c";  // 상급 (빨강)
            case 3 -> "§a";  // 중급 (초록)
            case 2 -> "§9";  // 하급 (파랑)
            case 1 -> "§7";  // 최하급 (회색)
            default -> "§f";
        };
    }

    /**
     * 등급 이름(한글)을 반환합니다.
     */
    public String getTierName() {
        return switch (tier) {
            case 6 -> "§5특수";
            case 5 -> "§6★★★★★ 최상급";
            case 4 -> "§c★★★★ 상급";
            case 3 -> "§a★★★ 중급";
            case 2 -> "§9★★ 하급";
            case 1 -> "§7★ 최하급";
            default -> "§f알 수 없음";
        };
    }
}
