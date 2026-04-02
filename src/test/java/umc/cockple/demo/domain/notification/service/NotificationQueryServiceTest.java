package umc.cockple.demo.domain.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.AllNotificationsResponseDTO;
import umc.cockple.demo.domain.notification.dto.ExistNewNotificationResponseDTO;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationQueryService")
class NotificationQueryServiceTest {

    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @Mock private NotificationRepository notificationRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private FileService fileService;

    private Member member;
    private Notification notification;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);

        notification = Notification.builder()
                .member(member)
                .partyId(100L)
                .title("테스트 모임")
                .content("테스트 알림 내용")
                .type(NotificationType.INVITE)
                .isRead(false)
                .imageKey("test-image-key")
                .data("{\"invitationId\":1}")
                .build();
        ReflectionTestUtils.setField(notification, "id", 10L);
    }


    @Nested
    @DisplayName("getAllNotifications - 내 알림 전체 조회")
    class GetAllNotifications {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("알림 목록을 DTO로 변환하여 반환한다")
            void getAllNotifications_returnsDtoList() {
                // given
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(notificationRepository.findAllByMemberOrderByCreatedAtDesc(member))
                        .willReturn(List.of(notification));
                given(fileService.getUrlFromKey("test-image-key"))
                        .willReturn("https://test-storage.com/test-image-key");

                // when
                List<AllNotificationsResponseDTO> result = notificationQueryService.getAllNotifications(member.getId());

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).notificationId()).isEqualTo(10L);
                assertThat(result.get(0).partyId()).isEqualTo(100L);
                assertThat(result.get(0).title()).isEqualTo("테스트 모임");
                assertThat(result.get(0).content()).isEqualTo("테스트 알림 내용");
                assertThat(result.get(0).type()).isEqualTo(NotificationType.INVITE);
                assertThat(result.get(0).isRead()).isFalse();
                assertThat(result.get(0).imgUrl()).isEqualTo("https://test-storage.com/test-image-key");
                assertThat(result.get(0).data()).isEqualTo("{\"invitationId\":1}");
            }

            @Test
            @DisplayName("imageKey가 없는 알림은 imgUrl이 null로 반환된다")
            void getAllNotifications_nullImageKey_returnsNullImgUrl() {
                // given
                Notification noImageNotification = Notification.builder()
                        .member(member)
                        .partyId(200L)
                        .title("이미지 없는 알림")
                        .content("모임이 삭제되었어요!")
                        .type(NotificationType.SIMPLE)
                        .isRead(false)
                        .imageKey(null)
                        .data("{}")
                        .build();
                ReflectionTestUtils.setField(noImageNotification, "id", 20L);

                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(notificationRepository.findAllByMemberOrderByCreatedAtDesc(member))
                        .willReturn(List.of(noImageNotification));
                given(fileService.getUrlFromKey(null)).willReturn(null);

                // when
                List<AllNotificationsResponseDTO> result = notificationQueryService.getAllNotifications(member.getId());

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).imgUrl()).isNull();
            }

            @Test
            @DisplayName("레포지토리가 반환한 createdAt 내림차순 순서를 그대로 유지하여 반환한다")
            void getAllNotifications_preservesDescOrderFromRepository() {
                // given
                Notification oldest = Notification.builder()
                        .member(member).partyId(100L).title("오래된 알림").content("c1")
                        .type(NotificationType.SIMPLE).isRead(true).imageKey(null).data("{}").build();
                ReflectionTestUtils.setField(oldest, "id", 1L);
                ReflectionTestUtils.setField(oldest, "createdAt", LocalDateTime.of(2025, 1, 1, 9, 0));

                Notification middle = Notification.builder()
                        .member(member).partyId(100L).title("중간 알림").content("c2")
                        .type(NotificationType.CHANGE).isRead(false).imageKey(null).data("{}").build();
                ReflectionTestUtils.setField(middle, "id", 2L);
                ReflectionTestUtils.setField(middle, "createdAt", LocalDateTime.of(2025, 6, 1, 9, 0));

                Notification newest = Notification.builder()
                        .member(member).partyId(100L).title("최신 알림").content("c3")
                        .type(NotificationType.INVITE).isRead(false).imageKey(null).data("{}").build();
                ReflectionTestUtils.setField(newest, "id", 3L);
                ReflectionTestUtils.setField(newest, "createdAt", LocalDateTime.of(2025, 12, 1, 9, 0));

                // 레포지토리는 createdAt DESC 순으로 반환 (newest → middle → oldest)
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(notificationRepository.findAllByMemberOrderByCreatedAtDesc(member))
                        .willReturn(List.of(newest, middle, oldest));
                given(fileService.getUrlFromKey(null)).willReturn(null);

                // when
                List<AllNotificationsResponseDTO> result = notificationQueryService.getAllNotifications(member.getId());

                // then
                assertThat(result).hasSize(3);
                assertThat(result.get(0).notificationId()).isEqualTo(3L); // newest
                assertThat(result.get(1).notificationId()).isEqualTo(2L); // middle
                assertThat(result.get(2).notificationId()).isEqualTo(1L); // oldest
            }

            @Test
            @DisplayName("알림이 없으면 빈 리스트를 반환한다")
            void getAllNotifications_noNotifications_returnsEmptyList() {
                // given
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
                given(notificationRepository.findAllByMemberOrderByCreatedAtDesc(member))
                        .willReturn(List.of());

                // when
                List<AllNotificationsResponseDTO> result = notificationQueryService.getAllNotifications(member.getId());

                // then
                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 회원이면 MemberException(MEMBER_NOT_FOUND)을 던진다")
            void memberNotFound_throwsMemberException() {
                // given
                given(memberRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> notificationQueryService.getAllNotifications(999L))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
            }
        }
    }


    @Nested
    @DisplayName("checkUnreadNotification - 읽지 않은 알림 존재여부 조회")
    class CheckUnreadNotification {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("읽지 않은 알림이 있으면 existNewNotification이 true이다")
            void hasUnreadNotification_returnsTrue() {
                // given
                ReflectionTestUtils.setField(member, "notifications", List.of(notification));
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when
                ExistNewNotificationResponseDTO result = notificationQueryService.checkUnreadNotification(member.getId());

                // then
                assertThat(result.existNewNotification()).isTrue();
            }

            @Test
            @DisplayName("모든 알림이 읽힌 상태이면 existNewNotification이 false이다")
            void allNotificationsRead_returnsFalse() {
                // given
                notification.read();
                ReflectionTestUtils.setField(member, "notifications", List.of(notification));
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when
                ExistNewNotificationResponseDTO result = notificationQueryService.checkUnreadNotification(member.getId());

                // then
                assertThat(result.existNewNotification()).isFalse();
            }

            @Test
            @DisplayName("알림이 없으면 existNewNotification이 false이다")
            void noNotifications_returnsFalse() {
                // given
                ReflectionTestUtils.setField(member, "notifications", List.of());
                given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));

                // when
                ExistNewNotificationResponseDTO result = notificationQueryService.checkUnreadNotification(member.getId());

                // then
                assertThat(result.existNewNotification()).isFalse();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 회원이면 MemberException(MEMBER_NOT_FOUND)을 던진다")
            void memberNotFound_throwsMemberException() {
                // given
                given(memberRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> notificationQueryService.checkUnreadNotification(999L))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
            }
        }
    }
}
