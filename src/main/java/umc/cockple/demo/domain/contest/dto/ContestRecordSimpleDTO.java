package umc.cockple.demo.domain.contest.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.ParticipationType;

import java.time.LocalDate;

public class ContestRecordSimpleDTO {

    @Builder
    public record Response(
            Long contestId,
            String contestName,
            ParticipationType type,
            Level level,
            LocalDate date,
            String medalImgUrl
    ) {
    }
}
