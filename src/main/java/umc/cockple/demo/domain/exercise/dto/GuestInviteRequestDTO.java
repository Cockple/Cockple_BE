package umc.cockple.demo.domain.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

public record GuestInviteRequestDTO(
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
