package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class PartyMemberDTO {
    @Builder
    public record Response(
            Summary summary,
            List<MemberDetail> members
    ) {}

    @Builder
    public record Summary(
            Integer totalCount,
            Integer femaleCount,
            Integer maleCount
    ) {}

    @Builder
    public record MemberDetail(
            Long memberId,
            String nickname,
            String profileImageUrl,
            String role,
            String gender,
            String level,
            Boolean isMe,
            LocalDate lastExerciseDate
    ) {}
}
