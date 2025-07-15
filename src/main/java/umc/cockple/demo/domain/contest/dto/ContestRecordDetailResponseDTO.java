package umc.cockple.demo.domain.contest.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.MedalType;
import umc.cockple.demo.global.enums.ParticipationType;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ContestRecordDetailResponseDTO(
        Long contestId,
        String contestName,
        LocalDate date,
        MedalType medalType,
        ParticipationType type,
        Level level,
        String content,
        List<String> contestImgUrls,
        List<String> contestVideoUrls
) {
}
