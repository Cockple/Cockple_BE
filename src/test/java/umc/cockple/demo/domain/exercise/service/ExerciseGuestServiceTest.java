package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.exercise.converter.command.ExerciseGuestCommandMapper;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.guest.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.command.internal.ExerciseGuestService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.GuestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseGuestService")
class ExerciseGuestServiceTest {

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;

    private ExerciseGuestService exerciseGuestService;

    private Member manager;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        ExerciseValidator exerciseValidator = new ExerciseValidator(
                new MemberPartyLookupService(memberPartyRepository), memberExerciseRepository);
        ExerciseGuestCommandMapper exerciseGuestCommandMapper = new ExerciseGuestCommandMapper();
        exerciseGuestService = new ExerciseGuestService(
                exerciseRepository, guestRepository, exerciseValidator, exerciseGuestCommandMapper);

        manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        exercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                LocalTime.of(12, 0), true, false);
        ReflectionTestUtils.setField(exercise, "id", 100L);
    }

    @Nested
    @DisplayName("inviteGuest")
    class InviteGuest {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("파티 멤버가 게스트를 초대하면 Response를 반환한다")
            void partyMember_inviteGuest_success() {
                // given
                ExerciseGuestInviteDTO.Request request = new ExerciseGuestInviteDTO.Request(
                        "테스트게스트", "남성", "B조");

                given(memberPartyRepository.existsByPartyAndMember(party, manager)).willReturn(true);
                given(guestRepository.save(any(Guest.class)))
                        .willAnswer(invocation -> {
                            Guest g = invocation.getArgument(0);
                            ReflectionTestUtils.setField(g, "id", 200L);
                            return g;
                        });

                // when
                ExerciseGuestInviteDTO.Response response = exerciseGuestService.inviteGuest(
                        exercise, manager, request);

                // then
                assertThat(response.guestId()).isEqualTo(200L);
                assertThat(response.currentParticipants()).isNotNull();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("이미 시작된 운동이면 ExerciseException(EXERCISE_ALREADY_STARTED_INVITATION)을 던진다")
            void alreadyStarted_throwsException() {
                Exercise startedExercise = ExerciseFixture.createExercise(party, LocalDate.of(2000, 1, 1),
                        LocalTime.of(12, 0), true, false);
                ReflectionTestUtils.setField(startedExercise, "id", 200L);

                ExerciseGuestInviteDTO.Request request = new ExerciseGuestInviteDTO.Request(
                        "테스트게스트", "남성", "B조");

                assertThatThrownBy(() ->
                        exerciseGuestService.inviteGuest(startedExercise, manager, request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_INVITATION));
            }

            @Test
            @DisplayName("파티 멤버가 아닌 사람이 초대하면 ExerciseException(NOT_PARTY_MEMBER_FOR_GUEST_INVITE)을 던진다")
            void notPartyMember_throwsException() {
                Member outsider = MemberFixture.createMember("외부인", Gender.MALE, Level.B, 3001L);
                ReflectionTestUtils.setField(outsider, "id", 3L);

                ExerciseGuestInviteDTO.Request request = new ExerciseGuestInviteDTO.Request(
                        "테스트게스트", "남성", "B조");

                given(memberPartyRepository.existsByPartyAndMember(party, outsider)).willReturn(false);

                assertThatThrownBy(() ->
                        exerciseGuestService.inviteGuest(exercise, outsider, request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.NOT_PARTY_MEMBER_FOR_GUEST_INVITE));
            }

            @Test
            @DisplayName("게스트 초대 정책 비허용이면 ExerciseException(GUEST_INVITATION_NOT_ALLOWED)을 던진다")
            void guestPolicyNotAllowed_throwsException() {
                Exercise noGuestExercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                        LocalTime.of(12, 0), false, false);
                ReflectionTestUtils.setField(noGuestExercise, "id", 201L);

                ExerciseGuestInviteDTO.Request request = new ExerciseGuestInviteDTO.Request(
                        "테스트게스트", "남성", "B조");

                given(memberPartyRepository.existsByPartyAndMember(party, manager)).willReturn(true);

                assertThatThrownBy(() ->
                        exerciseGuestService.inviteGuest(noGuestExercise, manager, request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.GUEST_INVITATION_NOT_ALLOWED));
            }
        }
    }

    @Nested
    @DisplayName("cancelGuestInvitation")
    class CancelGuestInvitation {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("초대자가 본인 게스트를 취소하면 Response를 반환한다")
            void cancelGuestInvitation_success() {
                // given
                Guest guest = GuestFixture.createGuest(exercise, manager.getId());
                ReflectionTestUtils.setField(guest, "id", 60L);

                // when
                ExerciseCancelDTO.Response response = exerciseGuestService
                        .cancelGuestInvitation(exercise, guest, manager);

                // then
                assertThat(response.memberName()).isEqualTo("게스트");
                assertThat(response.currentParticipants()).isNotNull();
                then(guestRepository).should().delete(guest);
                then(exerciseRepository).should().save(exercise);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("이미 시작된 운동이면 ExerciseException(EXERCISE_ALREADY_STARTED_CANCEL)을 던진다")
            void alreadyStarted_throwsException() {
                Exercise startedExercise = ExerciseFixture.createExercise(party, LocalDate.of(2000, 1, 1),
                        LocalTime.of(12, 0), true, false);
                ReflectionTestUtils.setField(startedExercise, "id", 200L);

                Guest guest = GuestFixture.createGuest(startedExercise, manager.getId());
                ReflectionTestUtils.setField(guest, "id", 60L);

                assertThatThrownBy(() ->
                        exerciseGuestService.cancelGuestInvitation(startedExercise, guest, manager))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL));
            }

            @Test
            @DisplayName("게스트가 해당 운동에 속하지 않으면 ExerciseException(GUEST_IS_NOT_PARTICIPATED_IN_EXERCISE)을 던진다")
            void guestNotInExercise_throwsException() {
                Exercise otherExercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                        LocalTime.of(12, 0), true, false);
                ReflectionTestUtils.setField(otherExercise, "id", 201L);

                Guest guest = GuestFixture.createGuest(otherExercise, manager.getId());
                ReflectionTestUtils.setField(guest, "id", 60L);

                assertThatThrownBy(() ->
                        exerciseGuestService.cancelGuestInvitation(exercise, guest, manager))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.GUEST_IS_NOT_PARTICIPATED_IN_EXERCISE));
            }

            @Test
            @DisplayName("본인이 초대하지 않은 게스트면 ExerciseException(GUEST_NOT_INVITED_BY_MEMBER)을 던진다")
            void guestNotInvitedByMember_throwsException() {
                Guest guest = GuestFixture.createGuest(exercise, 999L);
                ReflectionTestUtils.setField(guest, "id", 60L);

                assertThatThrownBy(() ->
                        exerciseGuestService.cancelGuestInvitation(exercise, guest, manager))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.GUEST_NOT_INVITED_BY_MEMBER));
            }
        }
    }
}
