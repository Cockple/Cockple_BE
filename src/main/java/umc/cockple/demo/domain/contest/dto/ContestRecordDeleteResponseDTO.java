package umc.cockple.demo.domain.contest.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ContestRecordDeleteResponseDTO(
        Long deleteContestId
) {

}
