package umc.cockple.demo.domain.game.service.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.game.service.support.assembler.GameBoardResultAssembler;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameBoardQueryService {

    private static final List<GameStatus> ACTIVE_STATUSES = List.of(GameStatus.PLAYING, GameStatus.WAITING);

    private final GameBoardReader gameBoardReader;
    private final CourtRepository courtRepository;
    private final GameRepository gameRepository;
    private final GameBoardAccessValidator gameBoardAccessValidator;
    private final GameBoardResultAssembler gameBoardResultAssembler;

    /**
     * 코트 보드 조회. 조회 자체는 인증된 회원이면 누구나 가능
     *
     * @param memberId 요청자
     */
    public GameBoardResult getBoard(Long memberId, Long gameBoardId) {
        GameBoard gameBoard = gameBoardReader.read(gameBoardId);
        boolean isGameHost = gameBoardAccessValidator.isGameHost(gameBoard.getId(), memberId);

        List<Court> courts = courtRepository.findByGameBoardIdOrderByCourtNoAsc(gameBoard.getId());
        List<Game> activeGames = gameRepository.findByGameBoardIdAndStatusInWithPlayers(
                gameBoard.getId(), ACTIVE_STATUSES);

        return gameBoardResultAssembler.assemble(isGameHost, courts, activeGames);
    }
}
