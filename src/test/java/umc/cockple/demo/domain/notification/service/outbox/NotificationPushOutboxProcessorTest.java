package umc.cockple.demo.domain.notification.service.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.command.outbox.NotificationPushOutboxPayload;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.domain.outbox.NotificationPushOutbox;
import umc.cockple.demo.domain.notification.enums.outbox.NotificationPushChannel;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.notification.repository.outbox.NotificationPushOutboxRepository;
import umc.cockple.demo.domain.notification.fcm.FcmService;
import umc.cockple.demo.domain.notification.service.ChatPushNotificationService;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.willThrow;

@ExtendWith(MockitoExtension.class)
class NotificationPushOutboxProcessorTest {

    @Mock private NotificationPushOutboxRepository repository;
    @Mock private NotificationPushOutboxClaimService claimService;
    @Mock private NotificationRepository notificationRepository;
    @Mock private FcmService fcmService;
    @Mock private ChatPushNotificationService chatPushNotificationService;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private NotificationPushOutboxProcessor processor;
    private NotificationPushOutbox outbox;
    private ClaimedNotificationPushOutbox claimed;
    private Member member;

    @BeforeEach
    void setUp() {
        processor = new NotificationPushOutboxProcessor(
                repository, claimService, notificationRepository, fcmService,
                chatPushNotificationService, objectMapper);
        ReflectionTestUtils.setField(processor, "maxRetryCount", 5);
        ReflectionTestUtils.setField(processor, "processingTimeoutMinutes", 10L);

        outbox = NotificationPushOutbox.pending(new NotificationPushOutboxPayload(
                100L, NotificationPushChannel.FCM));
        ReflectionTestUtils.setField(outbox, "id", 1L);
        claimed = new ClaimedNotificationPushOutbox(outbox, "claim-token");

        member = MemberFixture.createMember("수신자", Gender.MALE, Level.C, 1001L);
        ReflectionTestUtils.setField(member, "id", 10L);
    }

    @Test
    void FCM_전송에_성공하면_DONE으로_변경한다() {
        Notification notification = Notification.builder()
                .member(member).title("제목").content("내용").isRead(false).build();
        given(claimService.claim(any(), any(Integer.class), any())).willReturn(Optional.of(claimed));
        given(notificationRepository.findByIdWithMember(100L)).willReturn(Optional.of(notification));
        given(claimService.markDone(claimed)).willReturn(true);

        assertThat(processor.processOne(1L)).isTrue();

        verify(fcmService).sendNotificationWithRetry(member, "제목", "내용");
        verify(claimService).markDone(claimed);
    }

    @Test
    void FCM_전송에_실패하면_FAILED로_변경한다() {
        Notification notification = Notification.builder()
                .member(member).title("제목").content("내용").isRead(false).build();
        given(claimService.claim(any(), any(Integer.class), any())).willReturn(Optional.of(claimed));
        given(notificationRepository.findByIdWithMember(100L)).willReturn(Optional.of(notification));
        willThrow(new IllegalStateException("FCM 장애"))
                .given(fcmService).sendNotificationWithRetry(member, "제목", "내용");

        assertThat(processor.processOne(1L)).isTrue();

        verify(claimService).markFailed(claimed, "FCM 장애");
    }
}
