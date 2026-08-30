package umc.cockple.demo.domain.push.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.push.command.NotificationPushOutboxPayload;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.push.domain.NotificationPushOutbox;
import umc.cockple.demo.domain.push.enums.NotificationPushChannel;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.push.repository.NotificationPushOutboxRepository;
import umc.cockple.demo.domain.notification.fcm.FcmService;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.willThrow;

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

        verify(claimService).markFailed(claimed, "FCM 장애", 5);
    }

    @Test
    void claim에_실패하면_처리하지_않는다() {
        given(claimService.claim(any(), any(Integer.class), any())).willReturn(Optional.empty());

        assertThat(processor.processOne(1L)).isFalse();
    }

    @Test
    void 탈퇴_회원은_푸시하지_않고_DONE_처리한다() {
        member.withdraw();
        Notification notification = Notification.builder()
                .member(member).title("제목").content("내용").isRead(false).build();
        given(claimService.claim(any(), any(Integer.class), any())).willReturn(Optional.of(claimed));
        given(notificationRepository.findByIdWithMember(100L)).willReturn(Optional.of(notification));
        given(claimService.markDone(claimed)).willReturn(true);

        assertThat(processor.processOne(1L)).isTrue();

        org.mockito.Mockito.verifyNoInteractions(fcmService);
        verify(claimService).markDone(claimed);
    }

    @Test
    void 알림이_없으면_DEAD_처리한다() {
        given(claimService.claim(any(), any(Integer.class), any())).willReturn(Optional.of(claimed));
        given(notificationRepository.findByIdWithMember(100L)).willReturn(Optional.empty());

        assertThat(processor.processOne(1L)).isTrue();

        verify(claimService).markDead(claimed, "알림을 찾을 수 없습니다. id=100");
    }

    @Test
    void 최대_재시도에_도달한_실패는_DEAD로_전환한다() {
        ReflectionTestUtils.setField(outbox, "retryCount", 5);
        given(claimService.claim(any(), any(Integer.class), any())).willReturn(Optional.of(claimed));
        given(notificationRepository.findByIdWithMember(100L)).willReturn(Optional.empty());

        processor.processOne(1L);

        verify(claimService).markDead(claimed, "알림을 찾을 수 없습니다. id=100");
    }
}
