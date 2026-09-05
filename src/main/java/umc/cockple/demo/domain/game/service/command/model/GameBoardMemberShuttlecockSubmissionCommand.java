package umc.cockple.demo.domain.game.service.command.model;

public record GameBoardMemberShuttlecockSubmissionCommand(
        Long gameBoardId,
        Long gameBoardMemberId,
        boolean shuttlecockSubmitted
) {
}
