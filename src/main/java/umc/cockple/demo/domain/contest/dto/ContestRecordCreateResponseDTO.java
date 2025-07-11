package umc.cockple.demo.domain.contest.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ContestRecordCreateResponseDTO(Long contestId, LocalDateTime createdAt) {

}
