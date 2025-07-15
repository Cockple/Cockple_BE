package umc.cockple.demo.domain.contest.dto;

import lombok.Builder;

@Builder
public record ContestMedalSummaryResponseDTO(
        int myMedalTotal,
        int goldCount,
        int silverCount,
        int bronzeCount
) {
}
