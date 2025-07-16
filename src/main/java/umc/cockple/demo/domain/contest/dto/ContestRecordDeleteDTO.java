package umc.cockple.demo.domain.contest.dto;

import lombok.Builder;

public class ContestRecordDeleteDTO {

    @Builder
    public record Response(
            Long deleteContestId
    ) {
    }

    @Builder
    public record Command(
            Long contestId,
            Long memberId
    ) {
    }
}
