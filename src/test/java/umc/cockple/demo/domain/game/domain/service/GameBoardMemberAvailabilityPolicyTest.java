package umc.cockple.demo.domain.game.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    @Test
    @DisplayName("PLAYING 선수는 시작 후 경과 시간이나 시작 시각 유무와 관계없이 제외한다")
    void filterAvailable_excludesPlayingMembersRegardlessOfElapsedTime() {
        GameBoardMember recent = GameFixture.member(1L, board, "최근", Level.A);
        GameBoardMember unknown = GameFixture.member(2L, board, "시각없음", Level.A);
        GameBoardMember exact = GameFixture.member(3L, board, "정확히10분", Level.A);
        GameBoardMember older = GameFixture.member(4L, board, "10분초과", Level.A);
        GameBoardMember available = GameFixture.member(5L, board, "가용", Level.A);
        Game recentGame = GameFixture.playingGame(
                1L, board, null, now.minusMinutes(9), GameFixture.player(recent, 0));
        Game unknownGame = GameFixture.playingGame(
                2L, board, null, null, GameFixture.player(unknown, 0));
        Game exactGame = GameFixture.playingGame(
                3L, board, null, now.minusMinutes(10), GameFixture.player(exact, 0));
        Game olderGame = GameFixture.playingGame(
                4L, board, null, now.minusMinutes(11), GameFixture.player(older, 0));

        List<GameBoardMember> result = policy.filterAvailable(
                List.of(recent, unknown, exact, older, available),
                List.of(recentGame, unknownGame, exactGame, olderGame));

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
