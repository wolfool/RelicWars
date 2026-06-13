package com.wolfool.relicwars.relic;

import org.bukkit.Material;

/**
 * 31개 넘버링 유물(#000~#030)의 기본 정의 데이터.
 * 각 유물의 번호, 이름, 등급, 외형 아이템, 쿨타임(초)을 저장합니다.
 *
 * 등급 체계:
 *   5단계 (#001~#005) - 최상급 (가장 강력)
 *   4단계 (#006~#010) - 상급
 *   3단계 (#011~#015) - 중급
 *   2단계 (#016~#020) - 하급
 *   1단계 (#021~#030) - 최하급 (가장 약함)
 *   특수  (#000)      - 최종 보스 유물
 */
public enum RelicDefinition {

    // === 특수 유물 ===
    RELIC_000(0, "세계의 핵", 6, Material.NETHER_STAR, 0,
            "§5§l#000 §d세계의 핵",
            "§7최종 보스를 처치한 자만이 얻을 수 있는 궁극의 유물."),

    // === 5단계 (최상급) ===
    RELIC_001(1, "시간의 모래시계", 5, Material.CLOCK, 1800,
            "§6§l#001 §e시간의 모래시계",
            "§7시간을 되돌려 과거의 상태를 복원한다."),
    RELIC_002(2, "탐욕의 적출자", 5, Material.GOLDEN_SWORD, 7200,
            "§6§l#002 §e탐욕의 적출자",
            "§7다운된 적을 즉시 처형하고 모든 유물을 강제 드랍시킨다."),
    RELIC_003(3, "부활의 토템", 5, Material.TOTEM_OF_UNDYING, 2400,
            "§6§l#003 §e부활의 토템",
            "§7사망 시 자동으로 부활하며 유물 드랍을 1회 방지한다."),
    RELIC_004(4, "차원의 열쇠", 5, Material.ENDER_EYE, 1500,
            "§6§l#004 §e차원의 열쇠",
            "§7지정한 좌표로 차원을 넘나드는 텔레포트를 실행한다."),
    RELIC_005(5, "신의 눈", 5, Material.ENDER_PEARL, 1200,
            "§6§l#005 §e신의 눈",
            "§7모든 유저의 위치를 일시적으로 파악한다."),

    // === 4단계 (상급) ===
    RELIC_006(6, "불멸의 심장", 4, Material.HEART_OF_THE_SEA, 1500,
            "§c§l#006 §4불멸의 심장",
            "§7일정 시간 동안 완전한 무적 상태가 된다."),
    RELIC_007(7, "그림자 망토", 4, Material.PHANTOM_MEMBRANE, 1200,
            "§c§l#007 §4그림자 망토",
            "§7완전한 투명화 상태로 전환된다."),
    RELIC_008(8, "공간 닻", 4, Material.LODESTONE, 2400,
            "§c§l#008 §4공간 닻",
            "§7공간 왜곡장을 생성하여 아군 다운 위치를 텔레포트시킨다."),
    RELIC_009(9, "영혼 포식자", 4, Material.SCULK_CATALYST, 1500,
            "§c§l#009 §4영혼 포식자",
            "§7적의 정신력을 흡수하여 자신의 것으로 만든다."),
    RELIC_010(10, "충격 코어", 4, Material.END_CRYSTAL, 480,
            "§c§l#010 §4충격 코어",
            "§7강력한 넉백과 함께 주변의 모든 상호작용을 차단한다."),

    // === 3단계 (중급) ===
    RELIC_011(11, "대지의 방패", 3, Material.SHIELD, 900,
            "§a§l#011 §2대지의 방패",
            "§7주변에 보호 장벽을 생성하여 아군을 보호한다."),
    RELIC_012(12, "사냥꾼의 나침반", 3, Material.COMPASS, 600,
            "§a§l#012 §2사냥꾼의 나침반",
            "§7가장 가까운 유물 소유자의 방향을 가리킨다."),
    RELIC_013(13, "탐욕 소환진", 3, Material.BLAZE_POWDER, 1200,
            "§a§l#013 §2탐욕 소환진",
            "§7탐욕의 보스를 소환하여 주변을 혼란에 빠뜨린다."),
    RELIC_014(14, "전쟁의 뿔피리", 3, Material.GOAT_HORN, 900,
            "§a§l#014 §2전쟁의 뿔피리",
            "§7아군에게 전투 버프를, 적에게 디버프를 부여한다."),
    RELIC_015(15, "봉인의 사슬", 3, Material.CHAIN, 600,
            "§a§l#015 §2봉인의 사슬",
            "§7대상의 유물 능력 사용을 일시적으로 봉인한다."),

    // === 2단계 (하급) ===
    RELIC_016(16, "감시의 방패", 2, Material.SPYGLASS, 1800,
            "§9§l#016 §1감시의 방패",
            "§7넓은 범위의 감시 구역을 전개하여 적의 접근을 감지한다."),
    RELIC_017(17, "치유의 샘", 2, Material.GOLDEN_APPLE, 900,
            "§9§l#017 §1치유의 샘",
            "§7주변 아군의 체력과 정신력을 회복시킨다."),
    RELIC_018(18, "도플갱어", 2, Material.ARMOR_STAND, 600,
            "§9§l#018 §1도플갱어",
            "§7자신의 분신을 생성하여 적을 혼란시킨다."),
    RELIC_019(19, "자기장 발생기", 2, Material.IRON_BLOCK, 720,
            "§9§l#019 §1자기장 발생기",
            "§7주변의 드랍 아이템과 봉인 유물을 끌어당긴다."),
    RELIC_020(20, "예언자의 수정구", 2, Material.END_CRYSTAL, 1200,
            "§9§l#020 §1예언자의 수정구",
            "§7봉인 유물 위치와 특정 유물 소유자 정보를 확인한다."),

    // === 1단계 (최하급) ===
    RELIC_021(21, "바람의 장화", 1, Material.LEATHER_BOOTS, 300,
            "§7§l#021 §f바람의 장화",
            "§7일시적으로 이동 속도가 크게 증가한다."),
    RELIC_022(22, "광부의 곡괭이", 1, Material.GOLDEN_PICKAXE, 300,
            "§7§l#022 §f광부의 곡괭이",
            "§7주변의 광물 위치를 탐지한다."),
    RELIC_023(23, "흡혈의 송곳니", 1, Material.GHAST_TEAR, 180,
            "§7§l#023 §f흡혈의 송곳니",
            "§7적을 공격할 때 체력을 흡수한다."),
    RELIC_024(24, "수호자의 징표", 1, Material.SHIELD, 600,
            "§7§l#024 §f수호자의 징표",
            "§7다운된 팀원을 특수 조건 하에 즉시 구조한다."),
    RELIC_025(25, "안개의 망토", 1, Material.COBWEB, 300,
            "§7§l#025 §f안개의 망토",
            "§7주변에 연막을 생성하여 시야를 차단한다."),
    RELIC_026(26, "워든의 심장", 1, Material.SCULK_SHRIEKER, 600,
            "§7§l#026 §f워든의 심장",
            "§7소리 표식을 수집하면 바닐라 워든을 소환한다."),
    RELIC_027(27, "연결의 끈", 1, Material.LEAD, 300,
            "§7§l#027 §f연결의 끈",
            "§7팀원과의 거리에 따라 양쪽 모두에게 버프를 부여한다."),
    RELIC_028(28, "도박사의 주사위", 1, Material.BRICK, 600,
            "§7§l#028 §f도박사의 주사위",
            "§7랜덤한 효과를 발동시킨다. 행운일 수도, 불운일 수도."),
    RELIC_029(29, "메아리의 종", 1, Material.BELL, 300,
            "§7§l#029 §f메아리의 종",
            "§7주변의 은신 중인 적을 탐지한다."),
    RELIC_030(30, "낙뢰의 심지", 1, Material.BLAZE_ROD, 300,
            "§7§l#030 §f낙뢰의 심지",
            "§7지정한 위치에 낙뢰를 떨어뜨린다.");

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

    /**
     * 유물 번호로 RelicDefinition을 찾습니다.
     * @param number 유물 번호 (0~30)
     * @return 해당 유물 정의, 없으면 null
     */
    public static RelicDefinition getByNumber(int number) {
        for (RelicDefinition def : values()) {
            if (def.number == number) return def;
        }
        return null;
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
