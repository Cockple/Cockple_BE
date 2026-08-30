package umc.cockple.demo.domain.game.service.query;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.query.result.GameCompletedGameResult;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 완료된 게임 조회
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameCompletedGameQueryService {

    private final GameBoardReader gameBoardReader;
    private final GameRepository gameRepository;

    /**
     * @param memberId 요청자(조회는 인증된 회원이면 누구나 가능 — 권한 제한 없음)
     * @param courtNo  코트 번호 필터(null 이면 전체)
     * @param cursor   이전 응답의 nextCursor(null/빈값이면 첫 페이지)
     */
    public GameCompletedGameResult getCompletedGames(
            Long memberId, Long gameBoardId, Integer courtNo, String cursor, int size) {
        GameBoard gameBoard = gameBoardReader.read(gameBoardId);
        Cursor parsedCursor = parseCursor(cursor);

        // 1단계: 페이징된 게임 ID (완료 시각 오름차순, hasNext 판정을 위해 size + 1개 조회)
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Long> pagedIds = gameRepository.findCompletedGameIds(
                gameBoard.getId(), GameStatus.COMPLETED, courtNo,
                parsedCursor.completedAt(), parsedCursor.gameId(), pageable);

        boolean hasNext = pagedIds.size() > size;
        List<Long> resultIds = hasNext ? pagedIds.subList(0, size) : pagedIds;

        // 완료 게임이 없으면 빈 IN 절(id in ()) fetch를 생략하고 조기 반환
        if (resultIds.isEmpty()) {
            return new GameCompletedGameResult(List.of(), null, false);
        }

        // 2단계: 플레이어/멤버 fetch 후 페이징 순서(완료 시각 오름차순) 복원
        Map<Long, Game> gamesById = gameRepository.findByIdInWithPlayers(resultIds).stream()
                .collect(Collectors.toMap(Game::getId, Function.identity()));
        List<GameCompletedGameResult.CompletedGameView> views = resultIds.stream()
                .map(gamesById::get)
                .map(this::toView)
                .toList();

        String nextCursor = hasNext && !resultIds.isEmpty()
                ? toCursor(gamesById.get(resultIds.get(resultIds.size() - 1)))
                : null;
        return new GameCompletedGameResult(views, nextCursor, hasNext);
    }

    /**
     * 커서 = "{완료시각 ISO}_{gameId}". 완료 시각 동시각 게임까지 안전하게 잇기 위한 복합 커서.
     */
    private Cursor parseCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return new Cursor(null, null);
        }
        // 조작된 커서(구분자 없음/날짜·id 파싱 실패)는 500이 아닌 400으로 감싼다.
        int separatorIndex = cursor.lastIndexOf('_');
        if (separatorIndex <= 0 || separatorIndex == cursor.length() - 1) {
            throw new GameException(GameErrorCode.INVALID_CURSOR);
        }
        try {
            LocalDateTime completedAt = LocalDateTime.parse(cursor.substring(0, separatorIndex));
            Long gameId = Long.parseLong(cursor.substring(separatorIndex + 1));
            return new Cursor(completedAt, gameId);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new GameException(GameErrorCode.INVALID_CURSOR);
        }
    }

    private String toCursor(Game game) {
        return game.getCompletedAt() + "_" + game.getId();
    }

    private GameCompletedGameResult.CompletedGameView toView(Game game) {
        int durationMin = (int) Duration.between(game.getStartedAt(), game.getCompletedAt()).toMinutes();
        int courtNo = game.getCourtNo() != null ? game.getCourtNo() : 0;
        List<GameCompletedGameResult.PlayerView> players = game.getPlayers().stream()
                .sorted(Comparator.comparingInt(GamePlayer::getPlayerOrder))
                .map(player -> new GameCompletedGameResult.PlayerView(
                        player.getGameBoardMember().getId(),
                        player.getGameBoardMember().getName(),
                        player.getGameBoardMember().getLevel(),
                        player.getPlayerOrder()))
                .toList();
        return new GameCompletedGameResult.CompletedGameView(
                game.getId(), courtNo, game.getCompletedAt(), durationMin, players);
    }

    private record Cursor(LocalDateTime completedAt, Long gameId) {
    }
}
