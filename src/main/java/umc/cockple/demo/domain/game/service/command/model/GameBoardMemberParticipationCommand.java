package umc.cockple.demo.domain.game.service.command.model;

public record GameBoardMemberParticipationCommand(
        Long gameBoardId,
        Long gameBoardMemberId,
        boolean participating
) {
}
