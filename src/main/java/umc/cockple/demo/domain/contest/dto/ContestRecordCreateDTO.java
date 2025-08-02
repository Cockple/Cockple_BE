package umc.cockple.demo.domain.contest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.domain.party.enums.ParticipationType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ContestRecordCreateDTO {

    @Builder
    public record Request(

            @NotBlank(message = "대회 이름은 필수입니다.")
            @Size(max = 60, message = "대회 이름은 최대 60자 입니다")
            String contestName,

            LocalDate date,

            MedalType medalType,

            @NotNull(message = "참여 형태는 필수입니다.")
            ParticipationType type,

            @NotNull(message = "참여 급수는 필수입니다.")
            Level level,

            @Size(max = 100, message = "대회 기록은 최대 100자까지 가능합니다.")
            String content,

            Boolean contentIsOpen,

            Boolean videoIsOpen,

            List<String> contestVideos,

            @Size(max = 3, message = "이미지는 3개까지 업로드 가능합니다.")
            List<String> contestImgs

    ) {
    }

    @Builder
    public record Command(
            Long memberId,
            String contestName,
            LocalDate date,
            MedalType medalType,
            ParticipationType type,
            Level level,
            String content,
            Boolean contentIsOpen,
            Boolean videoIsOpen,
            List<String> contestVideos,
            List<String> contestImgs
    ) {
    }

    @Builder
    public record Response(
            Long contestId,
            LocalDateTime createdAt
    ) {
    }
}
