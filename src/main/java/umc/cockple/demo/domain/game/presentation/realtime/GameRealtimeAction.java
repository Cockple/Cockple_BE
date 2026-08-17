package umc.cockple.demo.domain.game.presentation.realtime;

public enum GameRealtimeAction {
    SUBSCRIBE,    // 게임판 구독 (라이브 업데이트 수신 시작)
    UNSUBSCRIBE,  // 게임판 구독 해제
    MOVE_COURT,   // 코트 위치 변경 (게임을 다른 코트로 이동)
    START_GAME,   // 게임 시작 (대기 게임을 코트에 배치)
}
