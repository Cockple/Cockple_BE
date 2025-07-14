package umc.cockple.demo.domain.contest.dto;

import lombok.Builder;

@Builder
public record ContestRecordDeleteCommand(
        Long contestId,
        Long memberId
) {
}
