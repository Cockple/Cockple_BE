package umc.cockple.demo.domain.game.service.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.GameMatchType;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.query.result.GameRandomMatchResult;
import umc.cockple.demo.domain.game.domain.service.GamePairHistoryCalculator;
import umc.cockple.demo.domain.game.domain.service.GamePairHistoryCalculator.GamePairHistory;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.domain.service.matching.GameBestMatchSelector;
import umc.cockple.demo.domain.game.domain.service.matching.GameCandidatePoolSelector;
import umc.cockple.demo.domain.game.domain.service.matching.GameMatchTypeSelector;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;
import umc.cockple.demo.domain.game.domain.service.GameBoardMemberAvailabilityPolicy;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameRandomMatchQueryService {

    private static final List<GameStatus> ACTIVE_STATUSES =
            List.of(GameStatus.WAITING, GameStatus.PLAYING);
    private static final List<GameStatus> COMPLETED_ONLY = List.of(GameStatus.COMPLETED);

    private final GameBoardReader gameBoardReader;
    private final GameBoardMemberRepository gameBoardMemberRepository;
    private final GameRepository gameRepository;
    private final GameBoardAccessValidator gameBoardAccessValidator;
    private final GameBoardMemberAvailabilityPolicy availabilityPolicy;
    private final GameMatchTypeSelector matchTypeSelector;
    private final GameCandidatePoolSelector candidatePoolSelector;
    private final GamePairHistoryCalculator pairHistoryCalculator;
    private final GameBestMatchSelector bestMatchSelector;

    public GameRandomMatchResult match(Long memberId, Long gameBoardId) {
        gameBoardAccessValidator.validateGameHost(gameBoardId, memberId);
        GameBoard gameBoard = gameBoardReader.read(gameBoardId);
        LocalDateTime now = LocalDateTime.now();

        List<GameBoardMember> members = gameBoardMemberRepository
                .findByGameBoardIdOrderByIdAsc(gameBoard.getId());
        List<Game> activeGames = gameRepository.findByGameBoardIdAndStatusInWithPlayers(
                gameBoard.getId(), ACTIVE_STATUSES);
        List<GameBoardMember> candidates = availabilityPolicy
                .filterAvailable(members, activeGames, now).stream()
                .filter(member -> member.getLevel() != Level.NONE)
                .toList();
        if (candidates.size() < 4) {
            throw new GameException(GameErrorCode.INSUFFICIENT_AVAILABLE_PLAYERS);
        }

        List<GameMatchType> availableTypes = matchTypeSelector.findAvailableTypes(candidates);
        List<GameMatchType> feasibleTypes = new ArrayList<>();
        Map<GameMatchType, List<GameBoardMember>> candidatePools =
                new EnumMap<>(GameMatchType.class);
        for (GameMatchType availableType : availableTypes) {
            candidatePoolSelector.find(candidates, availableType).ifPresent(candidatePool -> {
                feasibleTypes.add(availableType);
                candidatePools.put(availableType, candidatePool);
            });
        }
        if (feasibleTypes.isEmpty()) {
            throw new GameException(GameErrorCode.RANDOM_MATCH_NOT_FOUND);
        }

        GameMatchType matchType = matchTypeSelector.selectFrom(feasibleTypes);
        List<GameBoardMember> candidatePool = candidatePools.get(matchType);
        List<Game> completedGames = gameRepository.findByGameBoardIdAndStatusInWithPlayers(
                gameBoard.getId(), COMPLETED_ONLY);
        GamePairHistory pairHistory = pairHistoryCalculator.calculate(completedGames);
        List<Long> matchedMemberIds = bestMatchSelector.select(
                candidatePool, matchType, pairHistory);

        return new GameRandomMatchResult(matchedMemberIds);
    }
}
