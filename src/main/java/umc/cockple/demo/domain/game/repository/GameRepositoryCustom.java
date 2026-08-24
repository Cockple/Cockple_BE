package umc.cockple.demo.domain.game.repository;

import umc.cockple.demo.domain.game.domain.service.GamePairCount;

import java.util.Collection;
import java.util.List;

public interface GameRepositoryCustom {

    List<GamePairCount> countCompletedGamePairs(
            Long gameBoardId,
            Collection<Long> gameBoardMemberIds);

    List<Long> findLatestCompletedGameMemberIds(Long gameBoardId);
}
