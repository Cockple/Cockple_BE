package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.game.repository.GamePlayerRepository;

@Component
@RequiredArgsConstructor
public class ExerciseGameAssignmentValidator {

    private final GamePlayerRepository gamePlayerRepository;

    public void validateMemberCancellation(Long gameBoardId, Long memberId) {
        if (gamePlayerRepository.existsByMemberSource(gameBoardId, memberId)) {
            throw assignedPlayerCancellationException();
        }
    }

    public void validateGuestCancellation(Long gameBoardId, Long guestId) {
        if (gamePlayerRepository.existsByGuestSource(gameBoardId, guestId)) {
            throw assignedPlayerCancellationException();
        }
    }

    private ExerciseException assignedPlayerCancellationException() {
        return new ExerciseException(ExerciseErrorCode.ASSIGNED_PLAYER_CANNOT_CANCEL);
    }
}
