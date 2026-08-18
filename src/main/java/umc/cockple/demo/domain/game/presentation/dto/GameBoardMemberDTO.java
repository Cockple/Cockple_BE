package umc.cockple.demo.domain.game.presentation.dto;

import java.util.List;

public class GameBoardMemberDTO {

    public record Response(
            int totalCount,
            List<MemberInfo> gameBoardMembers
    ) {
    }

    public record MemberInfo(
            Long gameBoardMemberId,
            boolean inGame,
            boolean waiting,
            boolean participating,
            int gameCount,
            String profileImageUrl,
            String name,
            String ageGroup,
            String level,
            boolean shuttlecockSubmitted
    ) {
    }
}
