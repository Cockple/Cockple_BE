package umc.cockple.demo.domain.contest.dto;

import lombok.Builder;

public class ContestMedalSummaryDTO {

    @Builder
    public record Response(
            int myMedalTotal,
            int goldCount,
            int silverCount,
            int bronzeCount
    ) {
    }

}
