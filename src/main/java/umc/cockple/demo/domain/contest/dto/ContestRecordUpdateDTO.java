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

public class ContestRecordUpdateDTO {

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

            boolean contentIsOpen,

            boolean videoIsOpen,

            // 새로 추가할 영상 링크들
            List<AddContestVideoRequest> contestVideos,

            // 새로 추가할 이미지
            List<AddContestImgRequest> contestImgs,

            // 삭제할 이미지 id
            List<Long> contestImgsToDelete,

            // 삭제할 영상의 비디오 id
            List<Long> contestVideoIdsToDelete

    ) {
    }

    @Builder
    public record Response(
            Long contestId,
            List<ContestImgResponse> contestImgs,
            List<ContestVideoResponse> contestVideos,
            LocalDateTime UpdatedAt
    ) {
    }
}
