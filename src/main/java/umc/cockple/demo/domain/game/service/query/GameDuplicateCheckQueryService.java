package umc.cockple.demo.domain.game.service.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.service.GamePairCount;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.query.result.GameDuplicateCheckResult;
import umc.cockple.demo.domain.game.domain.service.GamePairHistoryCalculator;
import umc.cockple.demo.domain.game.domain.service.GamePairHistoryCalculator.GamePairHistory;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임 중복 체크
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameDuplicateCheckQueryService {

    private final GameBoardReader gameBoardReader;
    private final GameRepository gameRepository;
    private final GameBoardMemberRepository gameBoardMemberRepository;
    private final GamePairHistoryCalculator gamePairHistoryCalculator;

    /**
     * @param memberId 요청자(조회는 인증된 회원이면 누구나 가능 — 권한 제한 없음)
     */
    public GameDuplicateCheckResult checkDuplicates(Long memberId, Long gameBoardId, List<Long> gameBoardMemberIds) {
        GameBoard gameBoard = gameBoardReader.read(gameBoardId);

        List<Long> targetMemberIds = gameBoardMemberIds.stream().distinct().toList();
        validateMembersBelongToBoard(gameBoard.getId(), targetMemberIds);

        List<GamePairCount> pairCounts = gameRepository.countCompletedGamePairs(
                gameBoard.getId(), targetMemberIds);
        List<Long> lastGameMemberIds = gameRepository
                .findLatestCompletedGameMemberIds(gameBoard.getId());
        GamePairHistory pairHistory = gamePairHistoryCalculator.fromCounts(
                pairCounts, lastGameMemberIds);

        List<GameDuplicateCheckResult.PairView> pairs = new ArrayList<>();
        for (int i = 0; i < targetMemberIds.size(); i++) {
            for (int j = i + 1; j < targetMemberIds.size(); j++) {
                Long memberIdA = targetMemberIds.get(i);
                Long memberIdB = targetMemberIds.get(j);
                int count = pairHistory.count(memberIdA, memberIdB);
                boolean playedInLastGame = pairHistory.playedInLastGame(memberIdA, memberIdB);
                pairs.add(new GameDuplicateCheckResult.PairView(memberIdA, memberIdB, count, playedInLastGame));
            }
        }
        return new GameDuplicateCheckResult(pairs);
    }

    private void validateMembersBelongToBoard(Long gameBoardId, List<Long> memberIds) {
        long foundCount = gameBoardMemberRepository.findByGameBoardIdAndIdIn(gameBoardId, memberIds).size();
        if (foundCount != memberIds.size()) {
            throw new GameException(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND);
        }
    }

}
