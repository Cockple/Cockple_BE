package umc.cockple.demo.domain.game.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.service.GamePairCount;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.GameRepository;

import java.util.Collection;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameReader {

    private final GameRepository gameRepository;

    public List<Game> readAllByGameBoardAndStatuses(
            Long gameBoardId, Collection<GameStatus> statuses) {
        return gameRepository.findByGameBoardIdAndStatusInWithPlayers(gameBoardId, statuses);
    }

    public boolean existsByGameBoardMemberAndStatuses(
            Long gameBoardMemberId, Collection<GameStatus> statuses) {
        return gameRepository.existsByGameBoardMemberIdAndStatusIn(gameBoardMemberId, statuses);
    }

    public List<GamePairCount> readCompletedPairCounts(
            Long gameBoardId,
            Collection<Long> gameBoardMemberIds) {
        return gameRepository.countCompletedGamePairs(gameBoardId, gameBoardMemberIds);
    }

    public List<Long> readLatestCompletedGameMemberIds(Long gameBoardId) {
        return gameRepository.findLatestCompletedGameMemberIds(gameBoardId);
    }
}
