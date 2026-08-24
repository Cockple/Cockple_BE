package umc.cockple.demo.domain.game.domain.service.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.GameMatchType;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GameCandidatePoolSelector")
class GameCandidatePoolSelectorTest {

    private final GameCandidatePoolSelector selector = new GameCandidatePoolSelector();

    @Test
    @DisplayName("급수없음 선수는 경기 수가 가장 적어도 후보 풀에서 제외한다")
    void select_excludesMembersWithoutLevel() {
        List<GameBoardMember> members = List.of(
                member(1L, Gender.MALE, Level.NONE, 0),
                member(2L, Gender.MALE, Level.A, 1),
                member(3L, Gender.MALE, Level.A, 1),
                member(4L, Gender.MALE, Level.A, 1),
                member(5L, Gender.MALE, Level.A, 1));

        List<GameBoardMember> result = selector.select(members, GameMatchType.MEN_DOUBLES);

        assertThat(ids(result)).containsExactly(2L, 3L, 4L, 5L);
    }

    @Test
    @DisplayName("필요한 구성이 완성된 최초 경기 수 단계에서 확장을 멈춘다")
    void select_stopsAtFirstSufficientGameCountThreshold() {
        List<GameBoardMember> members = List.of(
                member(5L, Gender.MALE, Level.A, 1),
                member(4L, Gender.MALE, Level.A, 0),
                member(2L, Gender.MALE, Level.A, 0),
                member(1L, Gender.MALE, Level.A, 0),
                member(3L, Gender.MALE, Level.A, 0));

        List<GameBoardMember> result = selector.select(members, GameMatchType.MEN_DOUBLES);

        assertThat(ids(result)).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    @DisplayName("최소 경기 수에서 정확히 5경기 차이인 선수까지 확장한다")
    void select_includesExactMaximumGameCountGap() {
        List<GameBoardMember> members = List.of(
                member(1L, Gender.FEMALE, Level.A, 0),
                member(2L, Gender.FEMALE, Level.A, 5),
                member(3L, Gender.FEMALE, Level.A, 5),
                member(4L, Gender.FEMALE, Level.A, 5));

        List<GameBoardMember> result = selector.select(members, GameMatchType.WOMEN_DOUBLES);

        assertThat(ids(result)).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    @DisplayName("최소 경기 수 +5 안에 필요한 구성이 없으면 GAME417 예외를 던진다")
    void select_rejectsWhenCandidateExpansionFails() {
        List<GameBoardMember> members = List.of(
                member(1L, Gender.MALE, Level.A, 0),
                member(2L, Gender.MALE, Level.A, 1),
                member(3L, Gender.MALE, Level.A, 5),
                member(4L, Gender.MALE, Level.A, 6));

        assertThatThrownBy(() -> selector.select(members, GameMatchType.MEN_DOUBLES))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(GameErrorCode.RANDOM_MATCH_NOT_FOUND));
    }

    @Test
    @DisplayName("혼복도 최소 경기 수 +5 안에 남녀 각 2명이 없으면 GAME417 예외를 던진다")
    void select_rejectsInsufficientMixedCompositionWithinGameCountGap() {
        List<GameBoardMember> members = List.of(
                member(1L, Gender.MALE, Level.A, 0),
                member(2L, Gender.MALE, Level.A, 0),
                member(3L, Gender.FEMALE, Level.A, 0),
                member(4L, Gender.FEMALE, Level.A, 6));

        assertThatThrownBy(() -> selector.select(members, GameMatchType.MIXED_DOUBLES))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(GameErrorCode.RANDOM_MATCH_NOT_FOUND));
    }

    @Test
    @DisplayName("선택된 동일 성별 타입과 다른 선수는 후보 풀에서 제외한다")
    void select_excludesOtherGenderForSameGenderDoubles() {
        List<GameBoardMember> members = List.of(
                member(1L, Gender.FEMALE, Level.A, 0),
                member(2L, Gender.MALE, Level.A, 0),
                member(3L, Gender.MALE, Level.A, 0),
                member(4L, Gender.MALE, Level.A, 0),
                member(5L, Gender.MALE, Level.A, 0));

        List<GameBoardMember> result = selector.select(members, GameMatchType.MEN_DOUBLES);

        assertThat(ids(result)).containsExactly(2L, 3L, 4L, 5L);
    }

    @Test
    @DisplayName("동일 경기 수의 단일 성별 후보 풀은 ID 순으로 최대 12명까지 선택한다")
    void select_limitsSameGenderPoolToTwelveById() {
        List<GameBoardMember> members = new ArrayList<>();
        for (long id = 13L; id >= 1L; id--) {
            members.add(member(id, Gender.MALE, Level.A, 0));
        }

        List<GameBoardMember> result = selector.select(members, GameMatchType.MEN_DOUBLES);

        assertThat(ids(result)).containsExactly(
                1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    @DisplayName("혼복 12명 제한에서도 남녀 각 2명을 보장한다")
    void select_preservesGenderCompositionAtMixedPoolLimit() {
        List<GameBoardMember> members = new ArrayList<>();
        for (long id = 1L; id <= 11L; id++) {
            members.add(member(id, Gender.MALE, Level.A, 0));
        }
        members.add(member(12L, Gender.FEMALE, Level.A, 0));
        members.add(member(13L, Gender.FEMALE, Level.A, 0));

        List<GameBoardMember> result = selector.select(members, GameMatchType.MIXED_DOUBLES);

        assertThat(result).hasSize(12);
        assertThat(ids(result)).contains(12L, 13L).doesNotContain(11L);
    }

    private List<Long> ids(List<GameBoardMember> members) {
        return members.stream().map(GameBoardMember::getId).toList();
    }

    private GameBoardMember member(Long id, Gender gender, Level level, int gameCount) {
        return GameBoardMember.builder()
                .id(id)
                .gender(gender)
                .level(level)
                .gameCount(gameCount)
                .build();
    }
}
