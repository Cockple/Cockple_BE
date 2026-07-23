package umc.cockple.demo.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.notification.domain.Notification;
import umc.cockple.demo.domain.notification.dto.CreateNotificationRequestDTO;
import umc.cockple.demo.domain.notification.enums.NotificationTarget;
import umc.cockple.demo.domain.notification.enums.NotificationType;
import umc.cockple.demo.domain.notification.events.NotificationEvent;
import umc.cockple.demo.domain.notification.exception.NotificationErrorCode;
import umc.cockple.demo.domain.notification.exception.NotificationException;
import umc.cockple.demo.domain.notification.repository.NotificationRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static umc.cockple.demo.domain.notification.dto.MarkAsReadDTO.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationCommandService")
class NotificationCommandServiceTest {

    @InjectMocks
    private NotificationCommandService notificationCommandService;

    @Mock private NotificationRepository notificationRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private NotificationMessageGenerator notificationMessageGenerator;
    @Mock private ObjectMapper objectMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    private Member member;
    private Party party;
    private Notification notification;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createMember("테스터", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(member, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", member.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        notification = Notification.builder()
                .member(member)
                .partyId(party.getId())
                .title("테스트 모임")
                .content("테스트 알림 내용")
                .type(NotificationType.INVITE)
                .isRead(false)
                .imageKey(null)
                .data("{\"invitationId\":1}")
                .build();
        ReflectionTestUtils.setField(notification, "id", 100L);
    }


    @Nested
    @DisplayName("markAsReadNotification - 알림 읽음 처리")
    class MarkAsReadNotification {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("INVITE 알림을 INVITE_ACCEPT로 읽음 처리하면 변경된 타입과 읽음 상태를 반환한다")
            void markAsRead_inviteAccept_returnsChangedType() {
                // given
                given(notificationRepository.findById(notification.getId()))
                        .willReturn(Optional.of(notification));

                // when
                Response result = notificationCommandService.markAsReadNotification(
                        member.getId(), notification.getId(), NotificationType.INVITE_ACCEPT);

                // then
                assertThat(result.type()).isEqualTo(NotificationType.INVITE_ACCEPT);
                assertThat(notification.getIsRead()).isTrue();
            }

            @Test
            @DisplayName("INVITE 알림을 INVITE_REJECT로 읽음 처리하면 변경된 타입과 읽음 상태를 반환한다")
            void markAsRead_inviteReject_returnsChangedType() {
                // given
                given(notificationRepository.findById(notification.getId()))
                        .willReturn(Optional.of(notification));

                // when
                Response result = notificationCommandService.markAsReadNotification(
                        member.getId(), notification.getId(), NotificationType.INVITE_REJECT);

                // then
                assertThat(result.type()).isEqualTo(NotificationType.INVITE_REJECT);
                assertThat(notification.getIsRead()).isTrue();
            }

            @Test
            @DisplayName("SIMPLE 알림을 읽음 처리하면 SIMPLE 타입과 읽음 상태를 반환한다")
            void markAsRead_simple_returnsSameType() {
                // given
                Notification simpleNotification = Notification.builder()
                        .member(member)
                        .partyId(100L)
                        .title("단순 알림")
                        .content("모임이 삭제되었어요!")
                        .type(NotificationType.SIMPLE)
                        .isRead(false)
                        .imageKey(null)
                        .data("{}")
                        .build();
                ReflectionTestUtils.setField(simpleNotification, "id", 200L);
                given(notificationRepository.findById(200L)).willReturn(Optional.of(simpleNotification));

                // when
                Response result = notificationCommandService.markAsReadNotification(
                        member.getId(), 200L, NotificationType.SIMPLE);

                // then
                assertThat(result.type()).isEqualTo(NotificationType.SIMPLE);
                assertThat(simpleNotification.getIsRead()).isTrue();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 알림이면 NotificationException(NOTIFICATION_NOT_FOUND)을 던진다")
            void notificationNotFound_throwsNotificationException() {
                // given
                given(notificationRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> notificationCommandService.markAsReadNotification(
                        member.getId(), 999L, NotificationType.SIMPLE))
                        .isInstanceOf(NotificationException.class)
                        .satisfies(e -> assertThat(((NotificationException) e).getCode())
                                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
            }

            @Test
            @DisplayName("다른 사용자의 알림이면 NotificationException(NOTIFICATION_NOT_OWNED)을 던진다")
            void notificationNotOwned_throwsNotificationException() {
                // given
                given(notificationRepository.findById(notification.getId()))
                        .willReturn(Optional.of(notification));

                // when & then
                assertThatThrownBy(() -> notificationCommandService.markAsReadNotification(
                        999L, notification.getId(), NotificationType.SIMPLE))
                        .isInstanceOf(NotificationException.class)
                        .satisfies(e -> assertThat(((NotificationException) e).getCode())
                                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_OWNED));
            }
        }
    }


    @Nested
    @DisplayName("createNotification - 알림 생성")
    class CreateNotification {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("PARTY_DELETE 알림을 생성하면 저장 및 FCM 전송이 호출된다")
            void createNotification_partyDelete_savesAndSendsFcm() throws Exception {
                // given
                CreateNotificationRequestDTO dto = CreateNotificationRequestDTO.builder()
                        .member(member)
                        .partyId(party.getId())
                        .target(NotificationTarget.PARTY_DELETE)
                        .build();

                given(notificationRepository.countByMember(member)).willReturn(0L);
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(notificationMessageGenerator.generatePartyDeletedMessage())
                        .willReturn("모임이 삭제되었어요!");
                given(objectMapper.writeValueAsString(any())).willReturn("{}");

                // when
                notificationCommandService.createNotification(dto);

                // then
                then(notificationRepository).should().save(any(Notification.class));
                then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
            }

            @Test
            @DisplayName("PARTY_INVITE 알림을 생성하면 title이 '새로운 모임'으로 이벤트가 발행된다")
            void createNotification_partyInvite_usesTitleNewParty() throws Exception {
                // given
                CreateNotificationRequestDTO dto = CreateNotificationRequestDTO.builder()
                        .member(member)
                        .partyId(party.getId())
                        .invitationId(1L)
                        .target(NotificationTarget.PARTY_INVITE)
                        .build();

                given(notificationRepository.countByMember(member)).willReturn(0L);
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(notificationMessageGenerator.generateInviteMessage(party.getPartyName()))
                        .willReturn("'테스트 모임' 모임에 초대를 받았습니다.");
                given(objectMapper.writeValueAsString(any())).willReturn("{\"invitationId\":1}");

                // when
                notificationCommandService.createNotification(dto);

                // then
                then(notificationRepository).should().save(any(Notification.class));
                then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
            }

            @Test
            @DisplayName("EXERCISE_DELETE 알림을 생성하면 날짜 포맷을 포함한 메시지로 저장된다")
            void createNotification_exerciseDelete_savesWithFormattedDate() throws Exception {
                // given
                LocalDate exerciseDate = LocalDate.of(2025, 3, 15);
                CreateNotificationRequestDTO dto = CreateNotificationRequestDTO.builder()
                        .member(member)
                        .partyId(party.getId())
                        .exerciseDate(exerciseDate)
                        .target(NotificationTarget.EXERCISE_DELETE)
                        .build();

                given(notificationRepository.countByMember(member)).willReturn(0L);
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(notificationMessageGenerator.generateExerciseDeletedMessage("03.15(토)"))
                        .willReturn("03.15(토) 운동이 삭제되었어요!");
                given(objectMapper.writeValueAsString(any())).willReturn("{}");

                // when
                notificationCommandService.createNotification(dto);

                // then
                then(notificationRepository).should().save(any(Notification.class));
                then(eventPublisher).should().publishEvent(any(NotificationEvent.class));
            }

            @Test
            @DisplayName("알림이 50개 이상이면 INVITE가 아닌 가장 오래된 알림을 삭제 후 저장한다")
            void createNotification_over50_deletesOldestNonInvite() throws Exception {
                // given
                Notification oldestNonInvite = Notification.builder()
                        .member(member).partyId(100L).title("오래된 알림").content("c")
                        .type(NotificationType.SIMPLE).isRead(true).imageKey(null).data("{}").build();
                ReflectionTestUtils.setField(oldestNonInvite, "id", 50L);

                CreateNotificationRequestDTO dto = CreateNotificationRequestDTO.builder()
                        .member(member)
                        .partyId(party.getId())
                        .target(NotificationTarget.PARTY_DELETE)
                        .build();

                given(notificationRepository.countByMember(member)).willReturn(50L);
                given(notificationRepository.findFirstByMemberAndTypeNotOrderByCreatedAtAsc(
                        member, NotificationType.INVITE))
                        .willReturn(Optional.of(oldestNonInvite));
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(notificationMessageGenerator.generatePartyDeletedMessage())
                        .willReturn("모임이 삭제되었어요!");
                given(objectMapper.writeValueAsString(any())).willReturn("{}");

                // when
                notificationCommandService.createNotification(dto);

                // then
                then(notificationRepository).should().delete(oldestNonInvite);
                then(notificationRepository).should().save(any(Notification.class));
            }

            @Test
            @DisplayName("알림이 49개이면 오래된 알림 삭제 없이 바로 저장한다")
            void createNotification_under50_savesWithoutDelete() throws Exception {
                // given
                CreateNotificationRequestDTO dto = CreateNotificationRequestDTO.builder()
                        .member(member)
                        .partyId(party.getId())
                        .target(NotificationTarget.PARTY_DELETE)
                        .build();

                given(notificationRepository.countByMember(member)).willReturn(49L);
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(notificationMessageGenerator.generatePartyDeletedMessage())
                        .willReturn("모임이 삭제되었어요!");
                given(objectMapper.writeValueAsString(any())).willReturn("{}");

                // when
                notificationCommandService.createNotification(dto);

                // then
                then(notificationRepository).should(never()).delete(any(Notification.class));
                then(notificationRepository).should().save(any(Notification.class));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 모임이면 PartyException(PARTY_NOT_FOUND)을 던지고 저장하지 않는다")
            void partyNotFound_throwsPartyException() {
                // given
                CreateNotificationRequestDTO dto = CreateNotificationRequestDTO.builder()
                        .member(member)
                        .partyId(999L)
                        .target(NotificationTarget.PARTY_DELETE)
                        .build();

                given(notificationRepository.countByMember(member)).willReturn(0L);
                given(partyRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> notificationCommandService.createNotification(dto))
                        .isInstanceOf(PartyException.class)
                        .satisfies(e -> assertThat(((PartyException) e).getCode())
                                .isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));

                then(notificationRepository).should(never()).save(any());
            }
        }
    }
}
