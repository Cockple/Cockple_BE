package umc.cockple.demo.domain.game.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import umc.cockple.demo.domain.game.domain.QGame;
import umc.cockple.demo.domain.game.domain.QGamePlayer;
import umc.cockple.demo.domain.game.domain.service.GamePairCount;
import umc.cockple.demo.domain.game.enums.GameStatus;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GameRepositoryCustomImpl implements GameRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<GamePairCount> countCompletedGamePairs(
            Long gameBoardId,
            Collection<Long> gameBoardMemberIds) {
        if (gameBoardMemberIds.size() < 2) {
            return List.of();
        }

        QGame game = QGame.game;
        QGamePlayer firstPlayer = new QGamePlayer("firstPlayer");
        QGamePlayer secondPlayer = new QGamePlayer("secondPlayer");
        NumberExpression<Long> completedGameCount = game.id.countDistinct();

        List<Tuple> rows = queryFactory
                .select(
                        firstPlayer.gameBoardMember.id,
                        secondPlayer.gameBoardMember.id,
                        completedGameCount)
                .from(game)
                .join(game.players, firstPlayer)
                .join(game.players, secondPlayer)
                .where(
                        game.gameBoard.id.eq(gameBoardId),
                        game.status.eq(GameStatus.COMPLETED),
                        firstPlayer.gameBoardMember.id.in(gameBoardMemberIds),
                        secondPlayer.gameBoardMember.id.in(gameBoardMemberIds),
                        firstPlayer.gameBoardMember.id.lt(secondPlayer.gameBoardMember.id))
                .groupBy(
                        firstPlayer.gameBoardMember.id,
                        secondPlayer.gameBoardMember.id)
                .fetch();

        return rows.stream()
                .map(row -> new GamePairCount(
                        row.get(firstPlayer.gameBoardMember.id),
                        row.get(secondPlayer.gameBoardMember.id),
                        row.get(completedGameCount)))
                .toList();
    }

    @Override
    public List<Long> findLatestCompletedGameMemberIds(Long gameBoardId) {
        QGame game = QGame.game;
        QGamePlayer gamePlayer = QGamePlayer.gamePlayer;

        Long latestCompletedGameId = queryFactory
                .select(game.id)
                .from(game)
                .where(
                        game.gameBoard.id.eq(gameBoardId),
                        game.status.eq(GameStatus.COMPLETED))
                .orderBy(game.completedAt.desc(), game.id.desc())
                .fetchFirst();
        if (latestCompletedGameId == null) {
            return List.of();
        }

        return queryFactory
                .select(gamePlayer.gameBoardMember.id)
                .from(gamePlayer)
                .where(gamePlayer.game.id.eq(latestCompletedGameId))
                .orderBy(gamePlayer.gameBoardMember.id.asc())
                .fetch();
    }
}
