package umc.cockple.demo.domain.game.service.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.service.GamePairCount;
import umc.cockple.demo.domain.game.enums.GameMatchType;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.service.query.result.GameRandomMatchResult;
import umc.cockple.demo.domain.game.domain.service.GamePairHistoryCalculator;
import umc.cockple.demo.domain.game.domain.service.GamePairHistoryCalculator.GamePairHistory;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardMemberReader;
import umc.cockple.demo.domain.game.service.support.reader.GameReader;
import umc.cockple.demo.domain.game.domain.service.matching.GameBestMatchSelector;
import umc.cockple.demo.domain.game.domain.service.matching.GameCandidatePoolSelector;
import umc.cockple.demo.domain.game.domain.service.matching.GameMatchTypeSelector;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;
import umc.cockple.demo.domain.game.domain.service.GameBoardMemberAvailabilityPolicy;
import umc.cockple.demo.global.enums.Level;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameRandomMatchQueryService {

    private static final List<GameStatus> ACTIVE_STATUSES =
            List.of(GameStatus.WAITING, GameStatus.PLAYING);

    private final GameBoardReader gameBoardReader;
    private final GameBoardMemberReader gameBoardMemberReader;
    private final GameReader gameReader;
    private final GameBoardAccessValidator gameBoardAccessValidator;
    private final GameBoardMemberAvailabilityPolicy availabilityPolicy;
    private final GameMatchTypeSelector matchTypeSelector;
    private final GameCandidatePoolSelector candidatePoolSelector;
    private final GamePairHistoryCalculator pairHistoryCalculator;
    private final GameBestMatchSelector bestMatchSelector;

    public GameRandomMatchResult match(Long memberId, Long gameBoardId) {
        gameBoardAccessValidator.validateGameHost(gameBoardId, memberId);
        GameBoard gameBoard = gameBoardReader.read(gameBoardId);

        List<GameBoardMember> candidates = findAvailableCandidates(gameBoard);
        Map<GameMatchType, List<GameBoardMember>> candidatePools = findCandidatePools(candidates);

        GameMatchType matchType = matchTypeSelector.selectFrom(List.copyOf(candidatePools.keySet()));
        List<Long> matchedMemberIds = selectBestMatch(
                gameBoard, candidatePools.get(matchType), matchType);

        return new GameRandomMatchResult(matchedMemberIds);
    }

    private List<GameBoardMember> findAvailableCandidates(GameBoard gameBoard) {
        List<GameBoardMember> members = gameBoardMemberReader.readAllByGameBoard(gameBoard.getId());
        List<Game> activeGames = gameReader.readAllByGameBoardAndStatuses(gameBoard.getId(), ACTIVE_STATUSES);
        List<GameBoardMember> candidates = availabilityPolicy
                .filterAvailable(members, activeGames).stream()
                .filter(member -> member.getLevel() != Level.NONE)
                .toList();
        if (candidates.size() < 4) {
            throw new GameException(GameErrorCode.INSUFFICIENT_AVAILABLE_PLAYERS);
        }
        return candidates;
    }

    private Map<GameMatchType, List<GameBoardMember>> findCandidatePools(
            List<GameBoardMember> candidates) {
        Map<GameMatchType, List<GameBoardMember>> candidatePools = new LinkedHashMap<>();
        for (GameMatchType availableType : matchTypeSelector.findAvailableTypes(candidates)) {
            candidatePoolSelector.find(candidates, availableType)
                    .ifPresent(candidatePool -> candidatePools.put(availableType, candidatePool));
        }
        if (candidatePools.isEmpty()) {
            throw new GameException(GameErrorCode.RANDOM_MATCH_NOT_FOUND);
        }
        return candidatePools;
    }

    private List<Long> selectBestMatch(
            GameBoard gameBoard,
            List<GameBoardMember> candidatePool,
            GameMatchType matchType) {
        List<Long> candidatePoolIds = candidatePool.stream()
                .map(GameBoardMember::getId)
                .toList();
        List<GamePairCount> pairCounts = gameReader.readCompletedPairCounts(
                gameBoard.getId(), candidatePoolIds);
        GamePairHistory pairHistory = pairHistoryCalculator.fromCounts(pairCounts, List.of());
        return bestMatchSelector.select(candidatePool, matchType, pairHistory);
    }
}
