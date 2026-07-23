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
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.notification.dto.ExistNewNotificationResponseDTO;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;

import static org.assertj.core.api.Assertions.assertThat;
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

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);
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
