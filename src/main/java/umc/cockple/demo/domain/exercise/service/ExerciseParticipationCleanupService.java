package umc.cockple.demo.domain.exercise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.repository.ExerciseParticipationRepository;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class ExerciseParticipationCleanupService {

    private final ExerciseParticipationRepository exerciseParticipationRepository;

    public int deleteFutureParticipationsForWithdrawal(
            Long memberId,
            LocalDateTime withdrawalTime) {
        return exerciseParticipationRepository.deleteFutureParticipationsByMemberId(
                memberId,
                withdrawalTime.toLocalDate(),
                withdrawalTime.toLocalTime());
    }

    /**
     * Member hard delete 전에 호출해 해당 회원의 운동 참여 데이터를 모두 정리한다.
     * 회원 soft delete 흐름에서는 호출하지 않는다.
     * hard delete 오케스트레이터는 같은 삭제 흐름에서 이 메서드를 먼저 호출한 뒤
     * Member 엔티티를 삭제해야 한다.
     */
    public int prepareMemberHardDelete(Long memberId) {
        return exerciseParticipationRepository.deleteAllByMemberId(memberId);
    }
}
