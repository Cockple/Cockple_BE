package umc.cockple.demo.domain.notification.fcm;

import com.google.firebase.messaging.BatchResponse;
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
import umc.cockple.demo.domain.chat.enums.ChatRoomType;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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

    @Nested
    @DisplayName("sendChatMulticast - 채팅 알림 fan-out")
    class SendChatMulticast {

        @Test
        @DisplayName("수신자가 여러 명이어도 멀티캐스트를 1회만 호출한다(fan-out N→1)")
        void 수신자_여러명이면_멀티캐스트_1회() throws FirebaseMessagingException {
            // given
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(mock(BatchResponse.class));
            List<Member> recipients = List.of(
                    memberWith(1L, "t1"), memberWith(2L, "t2"), memberWith(3L, "t3"));

            // when
            fcmService.sendChatMulticast(recipients, "제목", "내용", 10L, ChatRoomType.PARTY);

            // then: 개별 send가 아니라 멀티캐스트 1회
            then(firebaseMessaging).should(times(1)).sendEachForMulticast(any());
            then(firebaseMessaging).should(never()).send(any());
        }

        @Test
        @DisplayName("수신자가 500명을 초과하면 500씩 나눠 호출한다")
        void 오백명_초과시_청킹() throws FirebaseMessagingException {
            // given
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(mock(BatchResponse.class));
            List<Member> recipients = new ArrayList<>();
            for (int i = 0; i < 501; i++) {
                recipients.add(memberWith((long) i, "t" + i));
            }

            // when
            fcmService.sendChatMulticast(recipients, "제목", "내용", 10L, ChatRoomType.PARTY);

            // then: 501명 → 500 + 1 = 2회
            then(firebaseMessaging).should(times(2)).sendEachForMulticast(any());
        }

        @Test
        @DisplayName("유효한 토큰이 하나도 없으면 전송하지 않는다")
        void 토큰_전부_없으면_전송안함() throws FirebaseMessagingException {
            // given
            List<Member> recipients = List.of(memberWith(1L, null), memberWith(2L, ""));

            // when
            fcmService.sendChatMulticast(recipients, "제목", "내용", 10L, ChatRoomType.PARTY);

            // then
            then(firebaseMessaging).should(never()).sendEachForMulticast(any());
        }
    }

    private Member memberWith(long id, String token) {
        Member member = MemberFixture.createMember("m" + id, Gender.MALE, Level.C, 3000L + id);
        ReflectionTestUtils.setField(member, "id", id);
        if (token != null) {
            ReflectionTestUtils.setField(member, "fcmToken", token);
        }
        return member;
    }
}
