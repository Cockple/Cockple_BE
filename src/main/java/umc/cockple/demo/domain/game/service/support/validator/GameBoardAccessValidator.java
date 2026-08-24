package umc.cockple.demo.domain.game.service.support.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameBoardAccessValidator {

    private final ExerciseRepository exerciseRepository;

    public void validateGameHost(Long gameBoardId, Long memberId) {
        Exercise exercise = exerciseRepository.findByGameBoardId(gameBoardId)
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_BOARD_NOT_FOUND));

        if (!Objects.equals(exercise.getGameHostId(), memberId)) {
            throw new GameException(GameErrorCode.GAME_BOARD_ACCESS_DENIED);
        }
    }

    /**
     * 요청자가 해당 게임판(운동)의 게임 진행자인지 여부 판단
     */
    public boolean isGameHost(Long gameBoardId, Long memberId) {
        return exerciseRepository.findByGameBoardId(gameBoardId)
                .map(exercise -> Objects.equals(exercise.getGameHostId(), memberId))
                .orElse(false);
    }
}
