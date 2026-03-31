package umc.cockple.demo.domain.notification.fcm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.dto.FcmTokenRequestDTO;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.MemberFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FcmIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired ObjectMapper objectMapper;

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

}
