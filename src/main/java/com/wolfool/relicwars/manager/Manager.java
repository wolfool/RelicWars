package com.wolfool.relicwars.manager;

/**
 * 모든 매니저(Manager)가 구현해야 하는 인터페이스.
 * 플러그인 활성화 시 초기화(initialize), 비활성화 시 정리(shutdown)를 보장합니다.
 */
public interface Manager {

    /**
     * 매니저를 초기화합니다.
     * 이벤트 리스너 등록, 스케줄러 시작 등의 작업을 수행합니다.
     */
    void initialize();

    /**
     * 매니저를 정리합니다.
     * 스케줄러 취소, 데이터 저장 등 서버 종료 전 필요한 작업을 수행합니다.
     */
    void shutdown();
}
