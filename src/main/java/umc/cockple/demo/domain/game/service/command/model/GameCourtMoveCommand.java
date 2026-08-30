package umc.cockple.demo.domain.game.service.command.model;

/**
 * 코트 위치 변경 내부 command 모델
 *
 * @param gameBoardId  게임판 ID
 * @param courtId      게임이 현재 올라가 있는 원본 코트 ID
 * @param targetCourtNo 이동할 목적지 코트 번호
 */
public record GameCourtMoveCommand(
        Long gameBoardId,
        Long courtId,
        Integer targetCourtNo
) {
}
