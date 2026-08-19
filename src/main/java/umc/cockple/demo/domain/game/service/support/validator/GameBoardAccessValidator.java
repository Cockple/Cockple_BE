package umc.cockple.demo.domain.game.service.support.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameBoardAccessValidator {

    private final ExerciseRepository exerciseRepository;
    private final GameBoardMemberRepository gameBoardMemberRepository;

    public void validateGameHost(Long gameBoardId, Long memberId) {
        Exercise exercise = exerciseRepository.findByGameBoardId(gameBoardId)
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_BOARD_NOT_FOUND));

        if (!Objects.equals(exercise.getGameHostId(), memberId)) {
            throw new GameException(GameErrorCode.GAME_BOARD_ACCESS_DENIED);
        }
    }

    public void validateViewer(Long gameBoardId, Long memberId) {
        if (gameBoardMemberRepository.existsByGameBoardIdAndMemberId(gameBoardId, memberId)) {
            return;
        }

        Exercise exercise = exerciseRepository.findByGameBoardId(gameBoardId)
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_BOARD_NOT_FOUND));
        if (!Objects.equals(exercise.getGameHostId(), memberId)) {
            throw new GameException(GameErrorCode.GAME_BOARD_VIEW_ACCESS_DENIED);
        }
    }
}
