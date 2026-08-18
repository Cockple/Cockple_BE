package umc.cockple.demo.domain.exercise.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGameHostRecoveryService;
import umc.cockple.demo.domain.member.events.MemberWithdrawnEvent;
import umc.cockple.demo.domain.party.enums.PartyMemberAction;
import umc.cockple.demo.domain.party.events.PartyMemberJoinedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExerciseGameHostRecoveryListener {

    private final ExerciseGameHostRecoveryService exerciseGameHostRecoveryService;

    @EventListener
    public void handlePartyMemberChanged(PartyMemberJoinedEvent event) {
        if (event.action() != PartyMemberAction.LEFT) {
            return;
        }

        int recoveredCount = exerciseGameHostRecoveryService
                .recoverAfterPartyMemberLeft(event.partyId(), event.memberId());
        log.info("모임 이탈 회원의 게임 진행자 복구 완료 - partyId: {}, memberId: {}, count: {}",
                event.partyId(), event.memberId(), recoveredCount);
    }

    @EventListener
    public void handleMemberWithdrawn(MemberWithdrawnEvent event) {
        int recoveredCount = exerciseGameHostRecoveryService
                .recoverAfterMemberWithdrawn(event.memberId());
        log.info("탈퇴 회원의 게임 진행자 복구 완료 - memberId: {}, count: {}",
                event.memberId(), recoveredCount);
    }
}
