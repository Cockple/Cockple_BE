package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.exercise.repository.ExerciseParticipationRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseParticipationCleanupService")
class ExerciseParticipationCleanupServiceTest {

    @InjectMocks
    private ExerciseParticipationCleanupService cleanupService;

    @Mock
    private ExerciseParticipationRepository exerciseParticipationRepository;

    @Test
    @DisplayName("회원 탈퇴 시 기준 시각 이후의 운동 참여만 삭제한다")
    void deleteFutureParticipationsForWithdrawal_deletesOnlyFutureParticipations() {
        Long memberId = 1L;
        LocalDateTime withdrawalTime = LocalDateTime.of(2026, 9, 13, 14, 30);
        given(exerciseParticipationRepository.deleteFutureParticipationsByMemberId(
                memberId, withdrawalTime.toLocalDate(), withdrawalTime.toLocalTime()))
                .willReturn(2);

        int deletedCount = cleanupService.deleteFutureParticipationForWithdrawal(
                memberId, withdrawalTime);

        assertThat(deletedCount).isEqualTo(2);
        then(exerciseParticipationRepository).should()
                .deleteFutureParticipationsByMemberId(
                        memberId, withdrawalTime.toLocalDate(), withdrawalTime.toLocalTime());
    }

    @Test
    @DisplayName("회원 hard delete 전 해당 회원의 모든 운동 참여를 삭제한다")
    void prepareMemberHardDelete_deletesAllParticipations() {
        Long memberId = 1L;
        given(exerciseParticipationRepository.deleteAllByMemberId(memberId)).willReturn(3);

        int deletedCount = cleanupService.prepareMemberHardDelete(memberId);

        assertThat(deletedCount).isEqualTo(3);
        then(exerciseParticipationRepository).should().deleteAllByMemberId(memberId);
    }
}
