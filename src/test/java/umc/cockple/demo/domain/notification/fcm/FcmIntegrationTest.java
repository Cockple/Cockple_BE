package umc.cockple.demo.domain.notification.fcm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.dto.FcmTokenRequestDTO;
import umc.cockple.demo.domain.notification.fcm.FcmService;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.MemberFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FcmIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired FcmService fcmService;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean
    FirebaseMessaging firebaseMessaging;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(
                MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L));
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }


    @Nested
    @DisplayName("PATCH /api/notifications/fcm-token - FCM 토큰 등록/갱신")
    class RegisterFcmToken {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - FCM 토큰이 DB에 저장된다")
            void registerFcmToken_savedInDb() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/notifications/fcm-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new FcmTokenRequestDTO("new-fcm-token"))))
                        .andExpect(status().isOk());

                Member updated = memberRepository.findById(member.getId()).orElseThrow();
                assertThat(updated.getFcmToken()).isEqualTo("new-fcm-token");
            }

            @Test
            @DisplayName("200 - 기존 토큰이 새 토큰으로 교체된다")
            void registerFcmToken_updatesExistingToken() throws Exception {
                ReflectionTestUtils.setField(member, "fcmToken", "old-fcm-token");
                memberRepository.save(member);
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/notifications/fcm-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new FcmTokenRequestDTO("updated-fcm-token"))))
                        .andExpect(status().isOk());

                Member updated = memberRepository.findById(member.getId()).orElseThrow();
                assertThat(updated.getFcmToken()).isEqualTo("updated-fcm-token");
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("400 - 빈 문자열 토큰은 @NotBlank 검증에서 거부된다")
            void registerFcmToken_blankToken_returns400() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/notifications/fcm-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new FcmTokenRequestDTO(""))))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("400 - null 토큰은 @NotBlank 검증에서 거부된다")
            void registerFcmToken_nullToken_returns400() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/notifications/fcm-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new FcmTokenRequestDTO(null))))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("400 - 공백 문자열 토큰은 @NotBlank 검증에서 거부된다")
            void registerFcmToken_whitespaceToken_returns400() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/notifications/fcm-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new FcmTokenRequestDTO("   "))))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("400 - fcmToken 필드 누락 시 @NotBlank 검증에서 거부된다")
            void registerFcmToken_missingField_returns400() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/notifications/fcm-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isBadRequest());
            }
        }
    }


    @Nested
    @DisplayName("sendNotification - FCM 푸시 알림 전송")
    class SendNotification {

        @Nested
        @DisplayName("전송 성공")
        class Success {

            @Test
            @DisplayName("FCM 토큰이 있는 회원에게 알림 전송 시 firebaseMessaging.send()가 호출된다")
            void sendNotification_withToken_callsFirebase() throws Exception {
                ReflectionTestUtils.setField(member, "fcmToken", "valid-fcm-token");

                fcmService.sendNotification(member, "테스트 제목", "테스트 내용");

                then(firebaseMessaging).should().send(any());
            }
        }

        @Nested
        @DisplayName("전송 생략")
        class Skip {

            @Test
            @DisplayName("FCM 토큰이 null이면 firebaseMessaging.send()가 호출되지 않는다")
            void sendNotification_nullToken_skipsFirebase() {
                // member의 fcmToken은 기본값 null

                fcmService.sendNotification(member, "제목", "내용");

                try {
                    then(firebaseMessaging).should(never()).send(any());
                } catch (FirebaseMessagingException e) {
                    System.out.println("FirebaseMessagingException이 발생했지만, sendNotification 메서드는 예외를 전파하지 않아야 합니다.");
                }
            }

            @Test
            @DisplayName("FCM 토큰이 빈 문자열이면 firebaseMessaging.send()가 호출되지 않는다")
            void sendNotification_blankToken_skipsFirebase() {
                ReflectionTestUtils.setField(member, "fcmToken", "");

                fcmService.sendNotification(member, "제목", "내용");

                try {
                    then(firebaseMessaging).should(never()).send(any());
                } catch (FirebaseMessagingException e) {
                    System.out.println("FirebaseMessagingException이 발생했지만, sendNotification 메서드는 예외를 전파하지 않아야 합니다.");
                }
            }
        }

        @Nested
        @DisplayName("전송 실패")
        class Failure {

            @Test
            @DisplayName("Firebase 전송 실패 시 예외를 전파하지 않는다")
            void sendNotification_firebaseException_doesNotThrow() throws FirebaseMessagingException {
                ReflectionTestUtils.setField(member, "fcmToken", "valid-fcm-token");
                given(firebaseMessaging.send(any())).willThrow(mock(FirebaseMessagingException.class));

                assertThatCode(() -> fcmService.sendNotification(member, "제목", "내용"))
                        .doesNotThrowAnyException();
            }
        }
    }
}
