package umc.cockple.demo.domain.notification.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import umc.cockple.demo.domain.notification.command.NotificationRequest;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.service.NotificationMessageGenerator;
import umc.cockple.demo.domain.party.events.PartyDeletedEvent;
import umc.cockple.demo.domain.party.events.PartyInvitationCreatedEvent;
import umc.cockple.demo.domain.party.events.PartyRoleChangedEvent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartyNotificationStrategyTest {

    private final PartyNotificationStrategy strategy =
            new PartyNotificationStrategy(new NotificationMessageGenerator(), new ObjectMapper());

    @Test
    @DisplayName("모임 삭제 알림은 destination 없이 변환한다")
    void convertsPartyDeletedWithoutDestination() {
        List<NotificationRequest> requests = strategy.convert(
                PartyDeletedEvent.deleted(10L, 20L, "모임", "image-key"));

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).recipientMemberId()).isEqualTo(20L);
        assertThat(requests.get(0).destination()).isNull();
    }

    @Test
    @DisplayName("초대 알림은 초대 리소스와 응답 action으로 변환한다")
    void convertsInvitationWithRespondDestination() {
        List<NotificationRequest> requests = strategy.convert(
                PartyInvitationCreatedEvent.created(30L, 10L, 20L, "모임", "image-key"));

        assertThat(requests.get(0).destination().resourceType())
                .isEqualTo(NotificationResourceType.PARTY_INVITATION);
        assertThat(requests.get(0).destination().resourceId()).isEqualTo(30L);
        assertThat(requests.get(0).destination().action()).isEqualTo(NotificationAction.RESPOND);
    }

    @Test
    @DisplayName("역할 변경 이벤트는 수신자별 알림 요청으로 확장한다")
    void fansOutRoleChangedNotification() {
        List<NotificationRequest> requests = strategy.convert(
                PartyRoleChangedEvent.changed(
                        10L,
                        List.of(20L, 30L),
                        "모임",
                        "image-key",
                        "닉네임",
                        PartyRoleChangedEvent.RoleChangeAction.SUBOWNER_ASSIGNED));

        assertThat(requests).extracting(NotificationRequest::recipientMemberId)
                .containsExactly(20L, 30L);
    }
}
