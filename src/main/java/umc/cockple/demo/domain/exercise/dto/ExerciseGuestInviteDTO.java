package umc.cockple.demo.domain.exercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDateTime;

public class ExerciseGuestInviteDTO {

    @Schema(name = "ExerciseGuestInviteRequest", description = "게스트 초대 요청")
    public record Request(
            @NotBlank(message = "게스트 이름은 필수입니다.")
            @Size(max = 17, message = "게스트 이름은 17자 이하여야 합니다.")
            String guestName,

            @NotBlank(message = "성별은 필수입니다.")
            String gender,

            @NotBlank(message = "급수는 필수입니다.")
            String level
    ) {
        public Gender toParsedGender() {
            return Gender.fromKorean(gender);
        }

        public Level toParsedLevel() {
            return Level.fromKorean(level);
        }
    }

    @Builder
    public record Command(
            String guestName,
            Gender gender,
            Level level,
            Long inviterId
    ) {
    }

    @Builder
    public record Response(
            Long guestId,
            LocalDateTime invitedAt,
            Integer currentParticipants
    ) {
    }
}
