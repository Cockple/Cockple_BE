package umc.cockple.demo.domain.game.presentation.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class GameBoardMemberDTO {

    public record CreateRequest(
            @NotBlank(message = "이름은 필수입니다.")
            String name,
            @NotBlank(message = "성별은 필수입니다.")
            String gender,
            @NotBlank(message = "급수는 필수입니다.")
            String level,
            String ageGroup
    ) {
    }

    public record CreateResponse(
            Long gameBoardMemberId
    ) {
    }

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
