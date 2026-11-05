package umc.cockple.demo.domain.game.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameBoardMemberAvailabilityPolicy")
class GameBoardMemberAvailabilityPolicyTest {

    private final GameBoardMemberAvailabilityPolicy policy =
            new GameBoardMemberAvailabilityPolicy();

    private final GameBoard board = GameFixture.gameBoard(1L);
    private final LocalDateTime now = LocalDateTime.of(2026, 8, 24, 20, 0);

    @Test
    @DisplayName("불참 선수와 WAITING 선수는 제외한다")
    void filterAvailable_excludesInactiveAndWaitingMembers() {
        GameBoardMember inactive = GameFixture.member(1L, board, "불참", Level.A);
        inactive.changeParticipation(false);
        GameBoardMember waiting = GameFixture.member(2L, board, "대기", Level.A);
        GameBoardMember available = GameFixture.member(3L, board, "가용", Level.A);
        Game waitingGame = GameFixture.waitingGame(
                1L, board, 1, GameFixture.player(waiting, 0));

        List<GameBoardMember> result = policy.filterAvailable(
                List.of(inactive, waiting, available), List.of(waitingGame));

        assertThat(result).containsExactly(available);
    }

    @ParameterizedTest(name = "PLAYING 시작 {0}분 경과")
    @ValueSource(longs = {1, 9, 10, 11, 60})
    @DisplayName("PLAYING 선수는 시작 후 경과 시간과 관계없이 제외한다")
    void filterAvailable_excludesPlayingMembersRegardlessOfElapsedTime(long elapsedMinutes) {
        GameBoardMember playing = GameFixture.member(1L, board, "진행중", Level.A);
        GameBoardMember available = GameFixture.member(2L, board, "가용", Level.A);
        Game playingGame = GameFixture.playingGame(
                1L, board, null, now.minusMinutes(elapsedMinutes), GameFixture.player(playing, 0));

        List<GameBoardMember> result = policy.filterAvailable(
                List.of(playing, available), List.of(playingGame));

        assertThat(result).containsExactly(available);
    }

    @Test
    @DisplayName("급수가 없는 선수는 제외한다")
    void filterAvailable_excludesMembersWithoutLevel() {
        GameBoardMember noLevel = GameFixture.member(1L, board, "급수없음", Level.NONE);
        GameBoardMember available = GameFixture.member(2L, board, "가용", Level.A);

        List<GameBoardMember> result = policy.filterAvailable(
                List.of(noLevel, available), List.of());

        assertThat(result).containsExactly(available);
    }

    @Test
    @DisplayName("PLAYING 선수는 시작 시각이 없어도 제외한다")
    void filterAvailable_excludesPlayingMembersWithoutStartedAt() {
        GameBoardMember playing = GameFixture.member(1L, board, "시각없음", Level.A);
        GameBoardMember available = GameFixture.member(2L, board, "가용", Level.A);
        Game playingGame = GameFixture.playingGame(
                1L, board, null, null, GameFixture.player(playing, 0));

        List<GameBoardMember> result = policy.filterAvailable(
                List.of(playing, available), List.of(playingGame));

        assertThat(result).containsExactly(available);
    }

    @Test
    @DisplayName("여러 활성 게임 중 하나라도 차단 조건이면 제외한다")
    void filterAvailable_excludesMemberWhenAnyActiveGameBlocksSelection() {
        GameBoardMember member = GameFixture.member(1L, board, "중복", Level.A);
        Game oldPlaying = GameFixture.playingGame(
                1L, board, null, now.minusMinutes(20), GameFixture.player(member, 0));
        Game waiting = GameFixture.waitingGame(
                2L, board, 1, GameFixture.player(member, 0));

        List<GameBoardMember> result = policy.filterAvailable(
                List.of(member), List.of(oldPlaying, waiting));

        assertThat(result).isEmpty();
        assertThat(policy.hasWaitingConflict(List.of(member), List.of(oldPlaying, waiting))).isTrue();
    }

    @Test
    @DisplayName("수동 편성: 이미 대기(WAITING) 게임에 편성된 회원은 hasWaitingConflict가 true")
    void hasWaitingConflict_blocksWaitingMember() {
        GameBoardMember member = GameFixture.member(1L, board, "대기중", Level.A);
        Game waiting = GameFixture.waitingGame(1L, board, 1, GameFixture.player(member, 0));

        assertThat(policy.hasWaitingConflict(List.of(member), List.of(waiting))).isTrue();
    }

    @Test
    @DisplayName("수동 편성: 진행 중(PLAYING)이기만 한 회원은 hasWaitingConflict가 false")
    void hasWaitingConflict_ignoresPlayingMembers() {
        GameBoardMember recent = GameFixture.member(1L, board, "방금진행", Level.A);
        Game recentPlaying = GameFixture.playingGame(
                1L, board, null, now.minusMinutes(1), GameFixture.player(recent, 0));

        assertThat(policy.hasWaitingConflict(List.of(recent), List.of(recentPlaying))).isFalse();
    }
}
