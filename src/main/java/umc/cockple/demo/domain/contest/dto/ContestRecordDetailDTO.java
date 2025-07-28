package umc.cockple.demo.domain.contest.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.global.enums.ParticipationType;

import java.time.LocalDate;
import java.util.List;

public class ContestRecordDetailDTO {

    @Builder
    public record Response(
            Long contestId,
            String contestName,
            LocalDate date,
            MedalType medalType,
            ParticipationType type,
            Level level,
            String content,
            Boolean contentIsOpen,
            Boolean videoIsOpen,
            List<String> contestImgUrls,
            List<String> contestVideoUrls
    ) {
    }
}
