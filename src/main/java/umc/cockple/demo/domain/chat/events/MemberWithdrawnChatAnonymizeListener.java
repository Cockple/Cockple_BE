package umc.cockple.demo.domain.chat.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.chat.service.ChatMemberAnonymizationService;
import umc.cockple.demo.domain.member.events.MemberWithdrawnEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberWithdrawnChatAnonymizeListener {

    private final ChatMemberAnonymizationService chatMemberAnonymizationService;

    @EventListener
    public void handleMemberWithdrawn(MemberWithdrawnEvent event) {
        int anonymizedCount = chatMemberAnonymizationService.anonymizeDirectDisplayNames(event.memberId());
        log.info("탈퇴 회원 direct 채팅 표시명 익명화 완료 - memberId: {}, count: {}",
                event.memberId(), anonymizedCount);
    }
}
