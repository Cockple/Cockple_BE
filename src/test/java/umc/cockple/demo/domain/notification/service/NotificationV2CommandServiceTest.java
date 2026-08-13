package umc.cockple.demo.domain.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.domain.NotificationDestination;
import umc.cockple.demo.domain.notification.domain.NotificationLegacyCompatibility;
import umc.cockple.demo.domain.notification.command.NotificationCreateCommand;
import umc.cockple.demo.domain.notification.enums.NotificationAction;
import umc.cockple.demo.domain.notification.enums.NotificationResourceType;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.notification.service.outbox.NotificationPushOutboxService;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;

@ExtendWith(MockitoExtension.class)
class NotificationV2CommandServiceTest {

    @InjectMocks
    private NotificationV2CommandService notificationV2CommandService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NotificationPushOutboxService notificationPushOutboxService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);
        given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
        given(notificationRepository.countByMember(member)).willReturn(0L);
        willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 100L);
            return notification;
        }).given(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotification_storesDestinationAndLegacyCompatibilityFields() {
        NotificationCreateCommand command = new NotificationCreateCommand(
                member.getId(),
                "새로운 모임",
                "초대가 왔습니다.",
                null,
                "{\"invitationId\":20}",
                new NotificationDestination(
                        NotificationResourceType.PARTY_INVITATION,
                        20L,
                        NotificationAction.RESPOND
                )
        );

        notificationV2CommandService.createNotification(
                command,
                new NotificationLegacyCompatibility(10L, NotificationType.INVITE)
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        verify(notificationPushOutboxService).enqueue(100L);

        Notification saved = captor.getValue();
        assertThat(saved.getPartyId()).isEqualTo(10L);
        assertThat(saved.getType()).isEqualTo(NotificationType.INVITE);
        assertThat(saved.getResourceType()).isEqualTo(NotificationResourceType.PARTY_INVITATION);
        assertThat(saved.getResourceId()).isEqualTo(20L);
        assertThat(saved.getAction()).isEqualTo(NotificationAction.RESPOND);
        assertThat(saved.getData()).isEqualTo("{\"invitationId\":20}");
    }
}
