package umc.cockple.demo.domain.notification.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.exception.NotificationErrorCode;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.MemberFixture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired NotificationRepository notificationRepository;

    @MockitoBean
    FileService fileService;

    private Member member;
    private Notification notification;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L));

        notification = notificationRepository.save(Notification.builder()
                .member(member)
                .partyId(100L)
                .title("테스트 모임")
                .content("테스트 알림 내용")
                .type(NotificationType.INVITE)
                .isRead(false)
                .imageKey("test-image-key")
                .data("{\"invitationId\":1}")
                .build());
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }


    @Nested
    @DisplayName("GET /api/notifications/count - 안 읽은 알림 존재여부 조회")
    class CheckUnreadNotification {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 읽지 않은 알림이 있으면 existNewNotification이 true이다")
            void checkUnread_hasUnread() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/notifications/count"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.existNewNotification").value(true));
            }

            @Test
            @DisplayName("200 - 모든 알림을 읽으면 existNewNotification이 false이다")
            void checkUnread_allRead() throws Exception {
                notification.read();
                notificationRepository.save(notification);
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(get("/api/notifications/count"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.existNewNotification").value(false));
            }

            @Test
            @DisplayName("200 - 알림이 전혀 없으면 existNewNotification이 false이다")
            void checkUnread_noNotification() throws Exception {
                Member otherMember = memberRepository.save(
                        MemberFixture.createMember("다른멤버", Gender.FEMALE, Level.B, 2002L));
                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(get("/api/notifications/count"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.existNewNotification").value(false));
            }
        }
    }


    @Nested
    @DisplayName("PATCH /api/notifications/{notificationId} - 특정 알림 읽음 처리")
    class MarkAsReadNotification {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - INVITE 알림을 INVITE_ACCEPT로 읽음 처리하면 변경된 타입을 반환한다")
            void markAsRead_inviteAccept() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/notifications/{notificationId}", notification.getId())
                                .param("type", NotificationType.INVITE_ACCEPT.name()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.type").value("INVITE_ACCEPT"));
            }

            @Test
            @DisplayName("200 - INVITE 알림을 INVITE_REJECT로 읽음 처리하면 변경된 타입을 반환한다")
            void markAsRead_inviteReject() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/notifications/{notificationId}", notification.getId())
                                .param("type", NotificationType.INVITE_REJECT.name()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.type").value("INVITE_REJECT"));
            }

            @Test
            @DisplayName("200 - SIMPLE 알림을 읽음 처리하면 SIMPLE 타입을 반환한다")
            void markAsRead_simple() throws Exception {
                Notification simpleNotification = notificationRepository.save(Notification.builder()
                        .member(member)
                        .partyId(100L)
                        .title("테스트 모임")
                        .content("모임이 삭제되었어요!")
                        .type(NotificationType.SIMPLE)
                        .isRead(false)
                        .imageKey(null)
                        .data("{}")
                        .build());
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/notifications/{notificationId}", simpleNotification.getId())
                                .param("type", NotificationType.SIMPLE.name()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.type").value("SIMPLE"));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 알림 ID이면 에러를 반환한다")
            void notificationNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());

                mockMvc.perform(patch("/api/notifications/{notificationId}", 999L)
                                .param("type", NotificationType.SIMPLE.name()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(NotificationErrorCode.NOTIFICATION_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(NotificationErrorCode.NOTIFICATION_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("401 - 다른 사용자의 알림에 접근하면 에러를 반환한다")
            void notificationNotOwned() throws Exception {
                Member otherMember = memberRepository.save(
                        MemberFixture.createMember("다른멤버", Gender.FEMALE, Level.B, 2002L));
                SecurityContextHelper.setAuthentication(otherMember.getId(), otherMember.getNickname());

                mockMvc.perform(patch("/api/notifications/{notificationId}", notification.getId())
                                .param("type", NotificationType.SIMPLE.name()))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(NotificationErrorCode.NOTIFICATION_NOT_OWNED.getCode()))
                        .andExpect(jsonPath("$.message").value(NotificationErrorCode.NOTIFICATION_NOT_OWNED.getMessage()));
            }
        }
    }
}
