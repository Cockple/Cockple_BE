package umc.cockple.demo.domain.notification.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmService")
class FcmServiceTest {

    @InjectMocks
    private FcmService fcmService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    private Member memberWithToken;
    private Member memberWithoutToken;

    @BeforeEach
    void setUp() {
        memberWithToken = MemberFixture.createMember("토큰있음", Gender.MALE, Level.C, 1001L);
        ReflectionTestUtils.setField(memberWithToken, "id", 1L);
        ReflectionTestUtils.setField(memberWithToken, "fcmToken", "valid-fcm-token");

        memberWithoutToken = MemberFixture.createMember("토큰없음", Gender.MALE, Level.C, 1002L);
        ReflectionTestUtils.setField(memberWithoutToken, "id", 2L);
    }

    @Nested
    @DisplayName("sendNotification")
    class SendNotification {

        @Nested
        @DisplayName("전송 생략")
        class Skip {

            @Test
            @DisplayName("fcmToken이_null이면_전송하지_않는다")
            void fcmToken이_null이면_전송하지_않는다() throws FirebaseMessagingException {
                // when
                fcmService.sendNotification(memberWithoutToken, "제목", "내용");

                // then
                then(firebaseMessaging).should(never()).send(any());
            }

            @Test
            @DisplayName("fcmToken이_빈_문자열이면_전송하지_않는다")
            void fcmToken이_빈_문자열이면_전송하지_않는다() throws FirebaseMessagingException {
                // given
                ReflectionTestUtils.setField(memberWithoutToken, "fcmToken", "");

                // when
                fcmService.sendNotification(memberWithoutToken, "제목", "내용");

                // then
                then(firebaseMessaging).should(never()).send(any());
            }
        }

        @Nested
        @DisplayName("전송 실패")
        class Failure {

            @Test
            @DisplayName("Firebase_전송_실패해도_예외를_던지지_않는다")
            void Firebase_전송_실패해도_예외를_던지지_않는다() throws FirebaseMessagingException {
                // given
                FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
                given(firebaseMessaging.send(any())).willThrow(exception);

                // when & then
                assertThatCode(() -> fcmService.sendNotification(memberWithToken, "제목", "내용"))
                        .doesNotThrowAnyException();
            }
        }
    }
}
