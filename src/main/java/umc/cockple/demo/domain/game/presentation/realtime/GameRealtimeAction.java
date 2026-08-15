package umc.cockple.demo.domain.game.presentation.realtime;

public enum GameRealtimeAction {
    SUBSCRIBE,    // 게임판 구독 (라이브 업데이트 수신 시작)
    UNSUBSCRIBE,  // 게임판 구독 해제
    MOVE_COURT,   // #3 코트 위치 변경 (게임을 다른 코트로 이동)
}
