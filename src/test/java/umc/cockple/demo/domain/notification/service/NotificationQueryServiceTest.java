package umc.cockple.demo.domain.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.ExistNewNotificationResponseDTO;
import umc.cockple.demo.domain.notification.dto.NotificationListResponseDTO;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationQueryService")
class NotificationQueryServiceTest {

    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @Mock private NotificationRepository notificationRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private FileService fileService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);
    }

    @Nested
    @DisplayName("getAllNotifications - 커서 페이지네이션 조회")
    class GetAllNotifications {

        @Test
        @DisplayName("size보다 많이 조회되면 hasNext=true, size개만 반환하고 nextCursor는 마지막 알림 id이다")
        void hasNext_trimsToSize_andSetsNextCursor() {
            // given: size=2인데 size+1(=3)건 조회됨 → 다음 페이지 존재
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(notificationRepository.findPageByMember(eq(member), isNull(), any(Pageable.class)))
                    .willReturn(List.of(notification(5L), notification(4L), notification(3L)));
            given(notificationRepository.countByMember(member)).willReturn(10L);
            given(fileService.getUrlFromKey(null)).willReturn(null);

            // when
            NotificationListResponseDTO result = notificationQueryService.getAllNotifications(1L, null, 2);

            // then
            assertThat(result.notifications()).hasSize(2);
            assertThat(result.notifications().get(0).notificationId()).isEqualTo(5L);
            assertThat(result.notifications().get(1).notificationId()).isEqualTo(4L);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(4L);   // 이번 페이지의 가장 오래된 id
            assertThat(result.totalElements()).isEqualTo(10);
        }

        @Test
        @DisplayName("size 이하로 조회되면 hasNext=false, nextCursor=null이다")
        void noNext_returnsAllWithNullCursor() {
            // given: cursor 전달(다음 페이지 요청), size=5인데 2건만 조회됨
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(notificationRepository.findPageByMember(eq(member), eq(100L), any(Pageable.class)))
                    .willReturn(List.of(notification(9L), notification(8L)));
            given(notificationRepository.countByMember(member)).willReturn(2L);
            given(fileService.getUrlFromKey(null)).willReturn(null);

            // when
            NotificationListResponseDTO result = notificationQueryService.getAllNotifications(1L, 100L, 5);

            // then
            assertThat(result.notifications()).hasSize(2);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("hasNext 판정을 위해 size+1건을 조회한다")
        void fetchesSizePlusOne() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(notificationRepository.findPageByMember(eq(member), isNull(), any(Pageable.class)))
                    .willReturn(List.of());
            given(notificationRepository.countByMember(member)).willReturn(0L);

            // when
            notificationQueryService.getAllNotifications(1L, null, 20);

            // then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            then(notificationRepository).should().findPageByMember(eq(member), isNull(), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(21);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MemberException(MEMBER_NOT_FOUND)을 던진다")
        void memberNotFound_throwsMemberException() {
            // given
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationQueryService.getAllNotifications(999L, null, 20))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getCode())
                            .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }

        private Notification notification(long id) {
            Notification n = Notification.builder()
                    .member(member).partyId(100L).title("알림").content("내용")
                    .type(NotificationType.SIMPLE).isRead(false).imageKey(null).data("{}").build();
            ReflectionTestUtils.setField(n, "id", id);
            return n;
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
                given(notificationRepository.existsByMember_IdAndIsReadFalse(member.getId())).willReturn(true);

                // when
                ExistNewNotificationResponseDTO result = notificationQueryService.checkUnreadNotification(member.getId());

                // then
                assertThat(result.existNewNotification()).isTrue();
            }

            @Test
            @DisplayName("읽지 않은 알림이 없으면 existNewNotification이 false이다")
            void noUnreadNotification_returnsFalse() {
                // given
                given(notificationRepository.existsByMember_IdAndIsReadFalse(member.getId())).willReturn(false);

                // when
                ExistNewNotificationResponseDTO result = notificationQueryService.checkUnreadNotification(member.getId());

                // then
                assertThat(result.existNewNotification()).isFalse();
            }
        }
    }
}
