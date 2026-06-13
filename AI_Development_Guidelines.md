# RelicWars AI 협업 개발 가이드라인 (AI Development Guidelines)

이 문서는 `RelicWars` 플러그인 개발을 위해 참여하는 모든 AI 어시스턴트(및 개발자)가 일관된 방향성을 유지하고 충돌 없이 협업하기 위해 작성된 가이드라인입니다. 개발 전 또는 코드를 수정하기 전에 반드시 이 문서를 숙지해 주세요.

---

## 1. 프로젝트 핵심 요약
- **목표:** 넘버링 유물, 탐욕 보스, 정신력(Sanity), 다운/강탈 시스템을 포함하는 하드코어 SMP 플러그인 제작.
- **환경:** Minecraft Paper 1.21.4 / Java 21 / Maven
- **기획서 위치:** 
  - 세부 도감 및 전체 규칙: `RelicWars_design_Full.txt`
  - 핵심 시스템 요약: `RelicWars_design_Summary.md`

## 2. AI 협업 및 코드 작성 원칙 (매우 중요)
1. **NMS 절대 사용 금지:** 버전 호환성을 위해 NMS(Nether Minecraft Server) 코드는 일절 사용하지 않습니다. 모든 기능은 오직 **Paper API 1.21.4**만을 사용하여 구현합니다.
2. **기준 문서 우선:** 코드를 짤 때 기획서(Full.txt 및 Summary.md)와 충돌하는 내용이 있다면, 기획서를 우선시합니다. 만약 기획의 수정이 불가피하다면 사용자의 승인을 받고 기획서를 먼저 업데이트한 후 코드를 작성합니다.
3. **PersistentDataContainer (PDC) 활용:** 유물이나 특수 아이템을 식별할 때 이름(DisplayName)이나 로어(Lore)에 의존하지 마십시오. 반드시 PDC를 사용해 고유 ID(`relic_001` 등)를 저장하고 검증해야 합니다.
4. **비동기(Async) 처리:** SQLite 데이터베이스(relicwars.db)에 접근하여 저장/불러오기를 할 때는 절대 메인 서버 틱(Tick)을 멈추게 해선 안 됩니다. DB 작업은 반드시 비동기(Asynchronous) 스레드에서 처리하세요.
5. **엄격한 이벤트 통제:** 유물이 시스템 밖으로 빠져나가는 것을 막기 위해 `PlayerDropItemEvent`, `InventoryClickEvent`, 엔티티 상호작용 등 모든 예외 상황을 꼼꼼히 Cancel 처리해야 합니다.

## 3. 개발 단계 (Phases)
여러 AI가 작업을 이어받더라도 흐름이 끊기지 않도록 아래의 순서대로 개발을 진행합니다.

- **Phase 1: 기반 설정 (Foundation)**
  - Maven `pom.xml` 세팅 및 `plugin.yml` 작성.
  - SQLite 연결 유틸리티 및 `config.yml` 로드 매니저 구현.
- **Phase 2: 유물 기초 시스템 (Core Items)**
  - PDC를 활용한 유물 아이템 생성/검증 팩토리 클래스 구현.
  - 유물 이동 방지(버리기, 상자 보관 등) 이벤트 리스너 통합.
- **Phase 3: 핵심 메커니즘 (Mechanics)**
  - 다운(Downed) 상태 머신, 구조(Revive), 유물 강탈(Steal) 로직 구현.
  - 유물 봉인(Sealed) 오브젝트 (ItemDisplay + Interaction) 생성 로직.
  - 정신력(Sanity) 소모 및 디버프 스케줄러 구현.
  - 2인 팀 시스템 구현.
- **Phase 4: 콘텐츠 구현 (Gimmicks & Bosses)**
  - 유물 #030부터 #001까지의 개별 액티브/패시브 능력 구현.
  - 커스텀 보스 (바닐라 몹 베이스 + 속성 부여 + 스킬 스케줄러) 및 행동 기믹 구현.

## 4. 인수인계 및 커뮤니케이션
- **주석 및 설명:** 복잡한 기믹(예: 유물 강탈 알고리즘, 다운 상태의 뷰 처리 등)을 구현할 때는 다음 AI가 코드를 바로 이해할 수 있도록 JavaDoc과 주석을 상세히 남겨야 합니다.
- **Commit 메시지:** Git 커밋을 할 때는 어떤 Phase의 어떤 작업을 했는지 명확히 기록합니다. (예: `feat: Phase 3 - 다운 및 구조 시스템 구현`)
- **작업 인계:** 한 AI의 작업이 끝나면, 다음 AI에게 **"현재 Phase X까지 완료되었으며, 다음은 Y 클래스의 Z 메서드를 구현할 차례입니다."**라고 명확히 컨텍스트를 넘겨주세요.
