package umc.cockple.demo.domain.notification.listener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.fcm.FcmService;
import umc.cockple.demo.domain.notification.event.NotificationEvent;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.MemberFixture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@DisplayName("NotificationEventListener")
// 알림 @Async 풀(notificationExecutor)을 SyncTaskExecutor로 덮어써 동기 실행하려면 빈 오버라이드 허용 필요
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class NotificationEventListenerTest extends IntegrationTestBase {

    // handleNotification의 @Async("notificationExecutor")를 동기로 실행해 테스트 타이밍 문제를 제거
    @TestConfiguration
    static class SyncAsyncConfig {
        @Bean
        public TaskExecutor notificationExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // FcmService를 Mock으로 대체해 실제 FCM 전송 없이 호출 여부만 검증
    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private FcmService fcmService;

    private Member member;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        Member unsaved = MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(unsaved, "fcmToken", "valid-fcm-token");
        member = memberRepository.save(unsaved);
    }

    @Test
    @DisplayName("트랜잭션이 커밋되면 FCM 전송이 실행된다")
    void 트랜잭션_커밋_후_FCM_전송이_실행된다() {
        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new NotificationEvent(member.getId(), "제목", "내용"));
            return null;
        });

        // then
        then(fcmService).should().sendNotification(any(Member.class), any(), any());
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 FCM 전송이 실행되지 않는다")
    void 트랜잭션_롤백_시_FCM_전송이_실행되지_않는다() {
        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new NotificationEvent(member.getId(), "제목", "내용"));
            status.setRollbackOnly();
            return null;
        });

        // then
        then(fcmService).should(never()).sendNotification(any(), any(), any());
    }

    @Test
    @DisplayName("탈퇴 회원에게는 FCM 전송이 실행되지 않는다")
    void 탈퇴_회원에게는_FCM_전송이_실행되지_않는다() {
        // given - FCM 토큰이 있더라도 탈퇴 상태면 전송하지 않아야 함
        Member withdrawnMember = MemberFixture.createWithdrawnMember("탈퇴자", "탈퇴자", 2002L);
        ReflectionTestUtils.setField(withdrawnMember, "fcmToken", "valid-fcm-token");
        Member saved = memberRepository.save(withdrawnMember);

        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new NotificationEvent(saved.getId(), "제목", "내용"));
            return null;
        });

        // then
        then(fcmService).should(never()).sendNotification(any(), any(), any());
    }
}
