package umc.cockple.demo.domain.game.domain.service.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.GameMatchType;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GameMatchTypeSelector")
class GameMatchTypeSelectorTest {

    private final GameMatchTypeSelector selector = new GameMatchTypeSelector();
    private final GameBoard board = GameFixture.gameBoard(1L);

    @Test
    @DisplayName("남자 2명과 여자 2명이면 혼복만 선택 가능하다")
    void select_returnsMixedDoubles() {
        List<GameBoardMember> candidates = List.of(
                member(1L, Gender.MALE), member(2L, Gender.MALE),
                member(3L, Gender.FEMALE), member(4L, Gender.FEMALE));

        List<GameMatchType> availableTypes = selector.findAvailableTypes(candidates);

        assertThat(availableTypes).containsExactly(GameMatchType.MIXED_DOUBLES);
        assertThat(selector.selectFrom(availableTypes)).isEqualTo(GameMatchType.MIXED_DOUBLES);
    }

    @Test
    @DisplayName("남자 4명이면 남복만 선택 가능하다")
    void select_returnsMenDoubles() {
        List<GameBoardMember> candidates = List.of(
                member(1L, Gender.MALE), member(2L, Gender.MALE),
                member(3L, Gender.MALE), member(4L, Gender.MALE));

        List<GameMatchType> availableTypes = selector.findAvailableTypes(candidates);

        assertThat(availableTypes).containsExactly(GameMatchType.MEN_DOUBLES);
        assertThat(selector.selectFrom(availableTypes)).isEqualTo(GameMatchType.MEN_DOUBLES);
    }

    @Test
    @DisplayName("여자 4명이면 여복만 선택 가능하다")
    void select_returnsWomenDoubles() {
        List<GameBoardMember> candidates = List.of(
                member(1L, Gender.FEMALE), member(2L, Gender.FEMALE),
                member(3L, Gender.FEMALE), member(4L, Gender.FEMALE));

        List<GameMatchType> availableTypes = selector.findAvailableTypes(candidates);

        assertThat(availableTypes).containsExactly(GameMatchType.WOMEN_DOUBLES);
        assertThat(selector.selectFrom(availableTypes)).isEqualTo(GameMatchType.WOMEN_DOUBLES);
    }

    @Test
    @DisplayName("가능한 성별 구성이 없으면 GAME416 예외를 던진다")
    void select_rejectsInsufficientGenderComposition() {
        List<GameBoardMember> candidates = List.of(
                member(1L, Gender.MALE), member(2L, Gender.MALE),
                member(3L, Gender.MALE), member(4L, Gender.FEMALE));

        assertThatThrownBy(() -> selector.findAvailableTypes(candidates))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(GameErrorCode.INSUFFICIENT_GENDER_COMPOSITION));
    }

    private GameBoardMember member(Long id, Gender gender) {
        return GameBoardMember.builder()
                .id(id)
                .gameBoard(board)
                .name("선수" + id)
                .gender(gender)
                .level(Level.A)
                .participating(true)
                .gameCount(0)
                .shuttlecockSubmitted(false)
                .build();
    }
}
