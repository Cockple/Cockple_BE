package umc.cockple.demo.domain.exercise.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GuestReader {

    private final GuestRepository guestRepository;

    public Guest findByIdOrThrow(Long guestId) {
        return guestRepository.findById(guestId)
                .orElseThrow(() -> new ExerciseException(ExerciseErrorCode.GUEST_NOT_FOUND));
    }

    public List<Guest> findByExerciseId(Long exerciseId) {
        return guestRepository.findByExerciseId(exerciseId);
    }

    public List<Guest> findByExerciseIdAndInviterId(Long exerciseId, Long inviterId) {
        return guestRepository.findByExerciseIdAndInviterId(exerciseId, inviterId);
    }
}
