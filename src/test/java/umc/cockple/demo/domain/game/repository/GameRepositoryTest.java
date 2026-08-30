package umc.cockple.demo.domain.game.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.domain.service.GamePairCount;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.global.config.QuerydslConfig;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
@DisplayName("GameRepository")
class GameRepositoryTest {

    @Autowired private GameRepository gameRepository;
    @Autowired private TestEntityManager entityManager;

    @Test
    @DisplayName("완료 경기의 대상 멤버 페어 횟수와 최근 완료 경기 멤버만 조회한다")
    void completedPairQueries_returnAggregatesAndLatestGameMembers() {
        GameBoard board = entityManager.persist(GameBoard.create());
        List<GameBoardMember> members = List.of(
                persistMember(board, "선수1"),
                persistMember(board, "선수2"),
                persistMember(board, "선수3"),
                persistMember(board, "선수4"),
                persistMember(board, "선수5"),
                persistMember(board, "선수6"));
        persistGame(board, GameStatus.COMPLETED,
                LocalDateTime.of(2026, 8, 25, 10, 0), members.subList(0, 4));
        persistGame(board, GameStatus.COMPLETED,
                LocalDateTime.of(2026, 8, 25, 11, 0),
                List.of(members.get(0), members.get(1), members.get(4), members.get(5)));
        persistGame(board, GameStatus.PLAYING, null, members.subList(0, 4));
        entityManager.flush();
        entityManager.clear();

        List<Long> targetIds = members.subList(0, 4).stream()
                .map(GameBoardMember::getId)
                .toList();
        List<GamePairCount> pairCounts = gameRepository.countCompletedGamePairs(
                board.getId(), targetIds);
        List<Long> latestMemberIds = gameRepository.findLatestCompletedGameMemberIds(board.getId());

        assertThat(pairCounts).hasSize(6);
        assertThat(pairCounts).anySatisfy(pairCount -> {
            assertThat(pairCount.memberIdA()).isEqualTo(members.get(0).getId());
            assertThat(pairCount.memberIdB()).isEqualTo(members.get(1).getId());
            assertThat(pairCount.count()).isEqualTo(2);
        });
        assertThat(latestMemberIds).containsExactly(
                members.get(0).getId(),
                members.get(1).getId(),
                members.get(4).getId(),
                members.get(5).getId());
    }

    private GameBoardMember persistMember(GameBoard board, String name) {
        GameBoardMember member = GameBoardMember.create(
                name, Gender.MALE, Level.A, AgeGroup.TWENTIES);
        board.addGameBoardMember(member);
        return entityManager.persist(member);
    }

    private void persistGame(
            GameBoard board,
            GameStatus status,
            LocalDateTime completedAt,
            List<GameBoardMember> members) {
        Game game = Game.builder()
                .gameBoard(board)
                .status(status)
                .completedAt(completedAt)
                .build();
        for (int index = 0; index < members.size(); index++) {
            game.addPlayer(GamePlayer.create(members.get(index), index));
        }
        entityManager.persist(game);
    }
}
