package umc.cockple.demo.domain.game.service.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.query.result.GameDuplicateCheckResult;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 게임 중복 체크
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameDuplicateCheckQueryService {

    private static final List<GameStatus> COMPLETED_ONLY = List.of(GameStatus.COMPLETED);

    private final GameBoardReader gameBoardReader;
    private final GameRepository gameRepository;
    private final GameBoardMemberRepository gameBoardMemberRepository;

    /**
     * @param memberId 요청자(조회는 인증된 회원이면 누구나 가능 — 권한 제한 없음)
     */
    public GameDuplicateCheckResult checkDuplicates(Long memberId, Long gameBoardId, List<Long> gameBoardMemberIds) {
        GameBoard gameBoard = gameBoardReader.read(gameBoardId);

        List<Long> targetMemberIds = gameBoardMemberIds.stream().distinct().toList();
        validateMembersBelongToBoard(gameBoard.getId(), targetMemberIds);

        List<Game> completedGames = gameRepository
                .findByGameBoardIdAndStatusInWithPlayers(gameBoard.getId(), COMPLETED_ONLY);
        List<Set<Long>> gameMemberSets = completedGames.stream()
                .map(this::memberIdsOf)
                .toList();
        Set<Long> lastGameMemberIds = completedGames.stream()
                .max(Comparator.comparing(Game::getCompletedAt))
                .map(this::memberIdsOf)
                .orElse(Set.of());

        List<GameDuplicateCheckResult.PairView> pairs = new ArrayList<>();
        for (int i = 0; i < targetMemberIds.size(); i++) {
            for (int j = i + 1; j < targetMemberIds.size(); j++) {
                Long memberIdA = targetMemberIds.get(i);
                Long memberIdB = targetMemberIds.get(j);
                int count = (int) gameMemberSets.stream()
                        .filter(members -> members.contains(memberIdA) && members.contains(memberIdB))
                        .count();
                boolean playedInLastGame = lastGameMemberIds.contains(memberIdA)
                        && lastGameMemberIds.contains(memberIdB);
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

    private Set<Long> memberIdsOf(Game game) {
        return game.getPlayers().stream()
                .map(player -> player.getGameBoardMember().getId())
                .collect(Collectors.toSet());
    }
}
