package umc.cockple.demo.domain.notification.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.event.NotificationPushRequestedEvent;
import umc.cockple.demo.domain.notification.fcm.FcmService;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPushEventListener")
class NotificationPushEventListenerTest {

    @Mock private FcmService fcmService;
    @Mock private NotificationRepository notificationRepository;

    @InjectMocks private NotificationPushEventListener listener;

    private Member member;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("수신자", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);
    }

    private Notification notification(Long id, Member owner) {
        Notification notification = Notification.builder()
                .member(owner)
                .title("모임 제목")
                .content("알림 내용")
                .isRead(false)
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    @Test
    @DisplayName("notificationId로 알림을 조회해 활성 회원에게 FCM을 전송한다")
    void handle_sendsFcmForActiveMember() {
        // given
        Notification notification = notification(100L, member);
        given(notificationRepository.findByIdWithMember(100L)).willReturn(Optional.of(notification));

        // when
        listener.handle(new NotificationPushRequestedEvent(100L));

        // then: 조회한 알림의 제목·내용으로 수신 회원에게 전송
        then(notificationRepository).should().findByIdWithMember(100L);
        then(fcmService).should().sendNotification(member, "모임 제목", "알림 내용");
    }

    @Test
    @DisplayName("탈퇴 회원의 알림이면 FCM을 전송하지 않는다")
    void handle_skipsWithdrawnMember() {
        // given
        member.withdraw();
        Notification notification = notification(101L, member);
        given(notificationRepository.findByIdWithMember(101L)).willReturn(Optional.of(notification));

        // when
        listener.handle(new NotificationPushRequestedEvent(101L));

        // then
        then(fcmService).should(never()).sendNotification(any(), any(), any());
    }

    @Test
    @DisplayName("notificationId로 알림을 찾지 못하면 FCM을 전송하지 않고 예외도 던지지 않는다")
    void handle_missingNotification_doesNothing() {
        // given
        given(notificationRepository.findByIdWithMember(999L)).willReturn(Optional.empty());

        // when
        listener.handle(new NotificationPushRequestedEvent(999L));

        // then
        then(fcmService).should(never()).sendNotification(any(), any(), any());
    }

    @Test
    @DisplayName("FCM 전송 중 예외가 발생해도 리스너 밖으로 전파되지 않는다")
    void handle_swallowsFcmException() {
        // given
        Notification notification = notification(102L, member);
        given(notificationRepository.findByIdWithMember(102L)).willReturn(Optional.of(notification));
        willThrow(new RuntimeException("FCM 장애"))
                .given(fcmService).sendNotification(member, "모임 제목", "알림 내용");

        // when & then: 예외가 전파되지 않아야 한다
        assertThatCode(() -> listener.handle(new NotificationPushRequestedEvent(102L)))
                .doesNotThrowAnyException();
        then(fcmService).should().sendNotification(member, "모임 제목", "알림 내용");
    }
}
