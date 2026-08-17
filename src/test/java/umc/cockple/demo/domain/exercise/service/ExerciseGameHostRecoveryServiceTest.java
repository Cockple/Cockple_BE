package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGameHostRecoveryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseGameHostRecoveryService")
class ExerciseGameHostRecoveryServiceTest {

    @Mock private ExerciseRepository exerciseRepository;
    @InjectMocks private ExerciseGameHostRecoveryService exerciseGameHostRecoveryService;

    @Test
    @DisplayName("모임 이탈 시 해당 모임에서 이탈 회원이 진행자인 운동만 모임장으로 복구한다")
    void recoversPartyMemberDeparture() {
        given(exerciseRepository.restoreGameHostToPartyOwner(10L, 20L)).willReturn(2);

        int recoveredCount = exerciseGameHostRecoveryService
                .recoverAfterPartyMemberLeft(10L, 20L);

        assertThat(recoveredCount).isEqualTo(2);
        verify(exerciseRepository).restoreGameHostToPartyOwner(10L, 20L);
    }

    @Test
    @DisplayName("회원 탈퇴 시 모든 모임에서 탈퇴 회원이 진행자인 운동을 각 모임장으로 복구한다")
    void recoversMemberWithdrawal() {
        given(exerciseRepository.restoreGameHostsToPartyOwners(20L)).willReturn(3);

        int recoveredCount = exerciseGameHostRecoveryService
                .recoverAfterMemberWithdrawn(20L);

        assertThat(recoveredCount).isEqualTo(3);
        verify(exerciseRepository).restoreGameHostsToPartyOwners(20L);
    }
}
