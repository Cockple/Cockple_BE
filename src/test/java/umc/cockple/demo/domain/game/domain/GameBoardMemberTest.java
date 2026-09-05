package umc.cockple.demo.domain.game.domain;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameBoardMember")
class GameBoardMemberTest {

    @Test
    @DisplayName("회원 참가 정보를 운동일 기준 스냅샷으로 생성한다")
    void createFromMember_copiesSnapshotAndDefaults() {
        Member member = Member.builder()
                .memberName("회원")
                .gender(Gender.FEMALE)
                .level(Level.B)
                .birth(LocalDate.of(2000, 7, 1))
                .build();

        GameBoardMember gameBoardMember = GameBoardMember.createFromMember(
                member, LocalDate.of(2035, 6, 30));

        assertThat(gameBoardMember.getMember()).isSameAs(member);
        assertThat(gameBoardMember.getGuest()).isNull();
        assertThat(gameBoardMember.getName()).isEqualTo("회원");
        assertThat(gameBoardMember.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(gameBoardMember.getLevel()).isEqualTo(Level.B);
        assertThat(gameBoardMember.getAgeGroup()).isEqualTo(AgeGroup.THIRTIES);
        assertThat(gameBoardMember.getShuttlecockSubmitted()).isFalse();
        assertThat(gameBoardMember.getParticipating()).isTrue();
        assertThat(gameBoardMember.getGameCount()).isZero();
    }

    @Test
    @DisplayName("게스트 정보를 연령대 없이 스냅샷으로 생성한다")
    void createFromGuest_copiesSnapshotAndDefaults() {
        Guest guest = Guest.builder()
                .guestName("게스트")
                .gender(Gender.MALE)
                .level(Level.C)
                .build();

        GameBoardMember gameBoardMember = GameBoardMember.createFromGuest(guest);

        assertThat(gameBoardMember.getMember()).isNull();
        assertThat(gameBoardMember.getGuest()).isSameAs(guest);
        assertThat(gameBoardMember.getName()).isEqualTo("게스트");
        assertThat(gameBoardMember.getGender()).isEqualTo(Gender.MALE);
        assertThat(gameBoardMember.getLevel()).isEqualTo(Level.C);
        assertThat(gameBoardMember.getAgeGroup()).isNull();
        assertThat(gameBoardMember.getShuttlecockSubmitted()).isFalse();
        assertThat(gameBoardMember.getParticipating()).isTrue();
        assertThat(gameBoardMember.getGameCount()).isZero();
    }

    @Test
    @DisplayName("명단 표시 정보를 수정하고 연령대를 제거할 수 있다")
    void updateInfo_changesEditableSnapshotFields() {
        GameBoardMember gameBoardMember = GameBoardMember.create(
                "수정 전", Gender.MALE, Level.D, AgeGroup.THIRTIES);

        gameBoardMember.updateInfo("수정 후", Gender.FEMALE, Level.B, null);

        assertThat(gameBoardMember.getName()).isEqualTo("수정 후");
        assertThat(gameBoardMember.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(gameBoardMember.getLevel()).isEqualTo(Level.B);
        assertThat(gameBoardMember.getAgeGroup()).isNull();
    }

    @Test
    @DisplayName("참여 상태를 변경하며 같은 값 요청은 멱등적으로 처리한다")
    void changeParticipation_updatesIdempotently() {
        GameBoardMember gameBoardMember = GameBoardMember.create(
                "선수", Gender.MALE, Level.D, AgeGroup.TWENTIES);

        gameBoardMember.changeParticipation(false);
        gameBoardMember.changeParticipation(false);

        assertThat(gameBoardMember.getParticipating()).isFalse();
    }

    @Test
    @DisplayName("셔틀콕 제출 상태를 변경하며 같은 값 요청은 멱등적으로 처리한다")
    void changeShuttlecockSubmission_updatesIdempotently() {
        GameBoardMember gameBoardMember = GameBoardMember.create(
                "선수", Gender.MALE, Level.D, AgeGroup.TWENTIES);

        gameBoardMember.changeShuttlecockSubmission(true);
        gameBoardMember.changeShuttlecockSubmission(true);

        assertThat(gameBoardMember.getShuttlecockSubmitted()).isTrue();
    }

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
