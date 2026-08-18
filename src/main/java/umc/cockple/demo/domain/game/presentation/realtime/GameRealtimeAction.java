package umc.cockple.demo.domain.game.presentation.realtime;

public enum GameRealtimeAction {
    SUBSCRIBE,    // 게임판 구독 (라이브 업데이트 수신 시작)
    UNSUBSCRIBE,  // 게임판 구독 해제
    MOVE_COURT,   // 코트 위치 변경 (게임을 다른 코트로 이동)
    START_GAME,   // 게임 시작 (대기 게임을 코트에 배치)
    CREATE_GAME,  // 게임 대기 생성 (선택 멤버로 대기 게임 생성)
    DELETE_GAME,  // 게임 취소/대기 삭제 (restore=true면 복원용 플레이어 반환)
    MOVE_TO_WAITING, // 대기열 이동 (진행 게임을 기록 없이 대기열 맨 앞으로 되돌리고 코트 비움)
    COMPLETE_GAME, // 게임 완료 (진행 게임 완료 처리 + 참여자 게임횟수 +1, 코트 비움)
}
