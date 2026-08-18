package umc.cockple.demo.domain.exercise.service.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGameHostRecoveryService;
import umc.cockple.demo.domain.member.events.MemberWithdrawnEvent;
import umc.cockple.demo.domain.party.enums.PartyMemberAction;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;

import java.time.LocalDateTime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseGameHostRecoveryListener")
class ExerciseGameHostRecoveryListenerTest {

    @Mock private ExerciseGameHostRecoveryService exerciseGameHostRecoveryService;
    @InjectMocks private ExerciseGameHostRecoveryListener exerciseGameHostRecoveryListener;

    @Test
    @DisplayName("모임원 이탈 이벤트면 해당 모임의 진행자를 복구한다")
    void handlesPartyMemberLeft() {
        PartyMemberJoinedEvent event = new PartyMemberJoinedEvent(
                10L, 20L, "이탈 회원", PartyMemberAction.LEFT,
                LocalDateTime.now());

        exerciseGameHostRecoveryListener.handlePartyMemberChanged(event);

        verify(exerciseGameHostRecoveryService).recoverAfterPartyMemberLeft(10L, 20L);
    }

    @Test
    @DisplayName("모임원 가입 이벤트는 진행자 복구 대상이 아니다")
    void ignoresPartyMemberJoined() {
        PartyMemberJoinedEvent event = new PartyMemberJoinedEvent(
                10L, 20L, "가입 회원", PartyMemberAction.JOINED,
                LocalDateTime.now());

        exerciseGameHostRecoveryListener.handlePartyMemberChanged(event);

        verify(exerciseGameHostRecoveryService, never())
                .recoverAfterPartyMemberLeft(10L, 20L);
    }

    @Test
    @DisplayName("회원 탈퇴 이벤트면 모든 모임의 진행자를 복구한다")
    void handlesMemberWithdrawn() {
        MemberWithdrawnEvent event = MemberWithdrawnEvent.withdrawn(20L);

        exerciseGameHostRecoveryListener.handleMemberWithdrawn(event);

        verify(exerciseGameHostRecoveryService).recoverAfterMemberWithdrawn(20L);
    }
}
