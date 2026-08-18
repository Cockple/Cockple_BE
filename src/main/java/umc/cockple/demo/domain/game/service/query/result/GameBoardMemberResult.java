package umc.cockple.demo.domain.game.service.query.result;

import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

public record GameBoardMemberResult(
        int totalCount,
        List<MemberView> gameBoardMembers
) {
    public record MemberView(
            Long gameBoardMemberId,
            boolean inGame,
            boolean waiting,
            boolean participating,
            int gameCount,
            String profileImageUrl,
            String name,
            Gender gender,
            AgeGroup ageGroup,
            Level level,
            boolean shuttlecockSubmitted
    ) {
    }
}
