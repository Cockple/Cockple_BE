package umc.cockple.demo.domain.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

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
        public Gender toParsedGender() {
                return switch (gender) {
                        case "남성" -> Gender.MALE;
                        case "여성" -> Gender.FEMALE;
                        default -> throw new ExerciseException(ExerciseErrorCode.INVALID_GENDER_FORMAT);
                };
        }

        public Level toParsedLevel() {
                String normalizedLevel = level.trim().toUpperCase();

                return switch (normalizedLevel) {
                        case "자강" -> Level.EXPERT;
                        case "준자강" -> Level.SEMI_EXPERT;
                        case "A조" -> Level.A;
                        case "B조" -> Level.B;
                        case "C조" -> Level.C;
                        case "D조" -> Level.D;
                        case "초심" -> Level.BEGINNER;
                        case "왕초심" -> Level.NOVICE;
                        case "급수없음" -> Level.NONE;
                        default -> throw new ExerciseException(ExerciseErrorCode.INVALID_LEVEL_FORMAT);
                };
}
