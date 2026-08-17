package umc.cockple.demo.domain.game.domain;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.member.domain.Member;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameBoardMember")
class GameBoardMemberTest {

    @Test
    @DisplayName("회원과 게스트 원본을 동시에 연결할 수 없다")
    void validateSourceReference_rejectsMemberAndGuestTogether() {
        GameBoardMember gameBoardMember = GameBoardMember.builder()
                .member(Member.builder().build())
                .guest(Guest.builder().build())
                .build();

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();

            assertThat(validator.validate(gameBoardMember))
                    .singleElement()
                    .satisfies(violation -> assertThat(violation.getMessage())
                            .isEqualTo("게임판 명단은 회원과 게스트를 동시에 참조할 수 없습니다."));
        }
    }

    @Test
    @DisplayName("회원, 게스트, 수동 명단 원본 형태를 각각 허용한다")
    void validateSourceReference_acceptsSupportedSourceShapes() {
        GameBoardMember memberSource = GameBoardMember.builder()
                .member(Member.builder().build())
                .build();
        GameBoardMember guestSource = GameBoardMember.builder()
                .guest(Guest.builder().build())
                .build();
        GameBoardMember manualSource = GameBoardMember.builder().build();

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();

            assertThat(validator.validate(memberSource)).isEmpty();
            assertThat(validator.validate(guestSource)).isEmpty();
            assertThat(validator.validate(manualSource)).isEmpty();
        }
    }
}
