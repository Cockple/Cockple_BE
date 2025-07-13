package umc.cockple.demo.domain.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuestInviteRequestDTO(
        @NotBlank(message = "게스트 이름은 필수입니다.")
        @Size(max = 17, message = "게스트 이름은 17자 이하여야 합니다.")
        String guestName,

        @NotBlank(message = "성별은 필수입니다.")
        @Pattern(regexp = "^(남성|여성)$",
                message = "성별은 '남성' 또는 '여성'이어야 합니다.")
        String gender,

        @NotBlank(message = "급수는 필수입니다.")
        @Pattern(regexp = "^(자강|준자강|A조|B조|C조|D조|초심|왕초심|급수없음)$",
                message = "올바른 급수를 입력해주세요.")
        String level
) {
}
