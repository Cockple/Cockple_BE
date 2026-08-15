package umc.cockple.demo.domain.game.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.model.GameCourtManageCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCourtMoveCommand;
import umc.cockple.demo.domain.game.service.command.result.GameCourtManageResult;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameCourtCommandService {

    private final GameBoardReader gameBoardReader;
    private final CourtRepository courtRepository;
    private final GameRepository gameRepository;

    /**
     * 게임 코트 관리
     * 전달된 courts 목록을 "관리 후 최종 상태"로 간주
     *
     * @param memberId 요청자(추후 게임판 관리 권한 검증에 사용 예정)
     */
    public GameCourtManageResult manageCourts(Long memberId, GameCourtManageCommand command) {
        if (command.gameBoardId() == null) {
            throw new GameException(GameErrorCode.GAME_BOARD_ID_REQUIRED);
        }

        GameBoard gameBoard = gameBoardReader.read(command.gameBoardId());
        List<GameCourtManageCommand.CourtCommand> requestedCourts = command.courts();

        Map<Long, Court> existingCourtsById = courtRepository.findByGameBoardId(gameBoard.getId()).stream()
                .collect(Collectors.toMap(Court::getId, Function.identity()));

        validateRequestedCourtsBelongToBoard(requestedCourts, existingCourtsById);
        deleteRemovedCourts(requestedCourts, existingCourtsById);

        List<Court> finalCourts = upsertCourtsInOrder(gameBoard, requestedCourts, existingCourtsById);

        log.info("게임 코트 관리 완료 - gameBoardId: {}, 코트 수: {}", gameBoard.getId(), finalCourts.size());
        return toResult(gameBoard.getId(), finalCourts);
    }

    /**
     * 코트 위치 변경
     * 목적지 코트에 이미 게임이 있으면 두 게임의 코트를 SWAP
     *
     * @param memberId 요청자(추후 게임판 관리 권한 검증에 사용 예정)
     */
    public void moveCourt(Long memberId, GameCourtMoveCommand command) {
        GameBoard gameBoard = gameBoardReader.read(command.gameBoardId());

        Game sourceGame = gameRepository.findByCourtIdAndStatus(command.courtId(), GameStatus.PLAYING)
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_NOT_ON_COURT));
        if (!sourceGame.getGameBoard().getId().equals(gameBoard.getId())) {
            throw new GameException(GameErrorCode.COURT_NOT_FOUND);
        }

        Court sourceCourt = sourceGame.getCourt();
        Court targetCourt = courtRepository
                .findByGameBoardIdAndCourtNo(gameBoard.getId(), command.targetCourtNo())
                .orElseThrow(() -> new GameException(GameErrorCode.COURT_NOT_FOUND));

        if (targetCourt.getId().equals(sourceCourt.getId())) {
            return; // 같은 코트로의 이동은 무시
        }

        Optional<Game> targetGame = gameRepository.findByCourtIdAndStatus(targetCourt.getId(), GameStatus.PLAYING);

        sourceGame.changeCourt(targetCourt);
        targetGame.ifPresent(game -> game.changeCourt(sourceCourt));

        log.info("게임 코트 위치 변경 완료 - gameBoardId: {}, {}번코트 → {}번코트, swap: {}",
                gameBoard.getId(), sourceCourt.getCourtNo(), targetCourt.getCourtNo(), targetGame.isPresent());
    }

    private void validateRequestedCourtsBelongToBoard(
            List<GameCourtManageCommand.CourtCommand> requestedCourts,
            Map<Long, Court> existingCourtsById) {
        boolean hasForeignCourt = requestedCourts.stream()
                .map(GameCourtManageCommand.CourtCommand::courtId)
                .filter(Objects::nonNull)
                .anyMatch(courtId -> !existingCourtsById.containsKey(courtId));
        if (hasForeignCourt) {
            throw new GameException(GameErrorCode.COURT_NOT_FOUND);
        }
    }

    private void deleteRemovedCourts(
            List<GameCourtManageCommand.CourtCommand> requestedCourts,
            Map<Long, Court> existingCourtsById) {
        Set<Long> keepCourtIds = requestedCourts.stream()
                .map(GameCourtManageCommand.CourtCommand::courtId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Court> removedCourts = existingCourtsById.values().stream()
                .filter(court -> !keepCourtIds.contains(court.getId()))
                .toList();
        if (!removedCourts.isEmpty()) {
            courtRepository.deleteAll(removedCourts);
        }
    }

    private List<Court> upsertCourtsInOrder(
            GameBoard gameBoard,
            List<GameCourtManageCommand.CourtCommand> requestedCourts,
            Map<Long, Court> existingCourtsById) {
        List<Court> finalCourts = new ArrayList<>();
        int courtNo = 1;
        for (GameCourtManageCommand.CourtCommand requested : requestedCourts) {
            String courtName = resolveCourtName(requested.courtName(), courtNo);
            if (requested.courtId() != null) {
                Court court = existingCourtsById.get(requested.courtId());
                court.update(courtNo, courtName);
                finalCourts.add(court);
            } else {
                Court court = Court.create(gameBoard, courtNo, courtName);
                finalCourts.add(courtRepository.save(court));
            }
            courtNo++;
        }
        return finalCourts;
    }

    private String resolveCourtName(String requestedName, int courtNo) {
        return StringUtils.hasText(requestedName) ? requestedName : courtNo + "번 코트";
    }

    private GameCourtManageResult toResult(Long gameBoardId, List<Court> courts) {
        List<GameCourtManageResult.CourtResult> courtResults = courts.stream()
                .map(court -> new GameCourtManageResult.CourtResult(
                        court.getId(), court.getCourtNo(), court.getCourtName()))
                .toList();
        return new GameCourtManageResult(gameBoardId, courtResults);
    }
}
