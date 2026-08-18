package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.repository.GamePlayerRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGuestCommandService;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGuestInviteCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCancelResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseGuestInviteResult;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseGuestCommandService")
class ExerciseGuestCommandServiceTest {

    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private ExerciseReader exerciseReader;
    @Mock private GuestReader guestReader;
    @Mock private MemberLookupService memberLookupService;
    @Mock private GamePlayerRepository gamePlayerRepository;

    private ExerciseGuestCommandService exerciseGuestCommandService;

    private Member manager;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        ExerciseValidator exerciseValidator = new ExerciseValidator(
                new MemberPartyLookupService(memberPartyRepository), memberExerciseRepository);
        exerciseGuestCommandService = new ExerciseGuestCommandService(
                guestRepository,
                exerciseReader,
                guestReader,
                memberLookupService,
                exerciseValidator,
                new ExerciseGameAssignmentValidator(gamePlayerRepository));

        manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        exercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                LocalTime.of(12, 0), true, false);
        ReflectionTestUtils.setField(exercise, "id", 100L);
        ReflectionTestUtils.setField(exercise.getGameBoard(), "id", 1000L);

        lenient().when(exerciseReader.findByIdOrThrow(exercise.getId())).thenReturn(exercise);
        lenient().when(memberLookupService.findByIdOrThrow(manager.getId())).thenReturn(manager);
    }

    @Nested
    @DisplayName("inviteGuest")
    class InviteGuest {

        private ExerciseGuestInviteCommand command;

        @BeforeEach
        void setUp() {
            command = guestInviteCommand(manager.getId());
        }

        @Test
        @DisplayName("파티 멤버가 게스트를 초대하면 Response를 반환한다")
        void partyMember_inviteGuest_success() {
            given(memberPartyRepository.existsByPartyAndMember(party, manager)).willReturn(true);
            given(guestRepository.save(any(Guest.class)))
                    .willAnswer(invocation -> {
                        Guest guest = invocation.getArgument(0);
                        ReflectionTestUtils.setField(guest, "id", 200L);
                        return guest;
                    });

            ExerciseGuestInviteResult result = exerciseGuestCommandService
                    .inviteGuest(exercise.getId(), command);

            assertThat(result.guestId()).isEqualTo(200L);
            assertThat(result.currentParticipants()).isNotNull();
            assertThat(exercise.getGameBoard().getGameBoardMembers())
                    .singleElement()
                    .satisfies(gameBoardMember -> {
                        assertThat(gameBoardMember.getGuest().getId()).isEqualTo(200L);
                        assertThat(gameBoardMember.getName()).isEqualTo(command.guestName());
                        assertThat(gameBoardMember.getGender()).isEqualTo(command.gender());
                        assertThat(gameBoardMember.getLevel()).isEqualTo(command.level());
                        assertThat(gameBoardMember.getAgeGroup()).isNull();
                    });
        }

        @Test
        @DisplayName("이미 시작된 운동이면 ExerciseException(EXERCISE_ALREADY_STARTED_INVITATION)을 던진다")
        void alreadyStarted_throwsException() {
            Exercise startedExercise = ExerciseFixture.createExercise(party, LocalDate.of(2000, 1, 1),
                    LocalTime.of(12, 0), true, false);
            ReflectionTestUtils.setField(startedExercise, "id", 200L);
            given(exerciseReader.findByIdOrThrow(startedExercise.getId())).willReturn(startedExercise);

            assertThatThrownBy(() -> exerciseGuestCommandService
                    .inviteGuest(startedExercise.getId(), command))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_INVITATION));
        }

        @Test
        @DisplayName("파티 멤버가 아닌 사람이 초대하면 ExerciseException(NOT_PARTY_MEMBER_FOR_GUEST_INVITE)을 던진다")
        void notPartyMember_throwsException() {
            Member outsider = MemberFixture.createMember("외부인", Gender.MALE, Level.B, 3001L);
            ReflectionTestUtils.setField(outsider, "id", 3L);
            given(memberLookupService.findByIdOrThrow(outsider.getId())).willReturn(outsider);
            given(memberPartyRepository.existsByPartyAndMember(party, outsider)).willReturn(false);

            assertThatThrownBy(() -> exerciseGuestCommandService
                    .inviteGuest(exercise.getId(), guestInviteCommand(outsider.getId())))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.NOT_PARTY_MEMBER_FOR_GUEST_INVITE));
        }

        @Test
        @DisplayName("게스트 초대 정책 비허용이면 ExerciseException(GUEST_INVITATION_NOT_ALLOWED)을 던진다")
        void guestPolicyNotAllowed_throwsException() {
            Exercise noGuestExercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                    LocalTime.of(12, 0), false, false);
            ReflectionTestUtils.setField(noGuestExercise, "id", 201L);
            given(exerciseReader.findByIdOrThrow(noGuestExercise.getId())).willReturn(noGuestExercise);
            given(memberPartyRepository.existsByPartyAndMember(party, manager)).willReturn(true);

            assertThatThrownBy(() -> exerciseGuestCommandService
                    .inviteGuest(noGuestExercise.getId(), command))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.GUEST_INVITATION_NOT_ALLOWED));
        }

        @Test
        @DisplayName("운동이 없으면 ExerciseException(EXERCISE_NOT_FOUND)을 던진다")
        void exerciseNotFound_throwsException() {
            given(exerciseReader.findByIdOrThrow(999L))
                    .willThrow(new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));

            assertThatThrownBy(() -> exerciseGuestCommandService.inviteGuest(999L, command))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.EXERCISE_NOT_FOUND));
        }

        @Test
        @DisplayName("멤버가 없으면 MemberException(MEMBER_NOT_FOUND)을 던진다")
        void memberNotFound_throwsException() {
            given(memberLookupService.findByIdOrThrow(999L))
                    .willThrow(new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

            assertThatThrownBy(() -> exerciseGuestCommandService.inviteGuest(
                    exercise.getId(), guestInviteCommand(999L)))
                    .isInstanceOf(MemberException.class)
                    .satisfies(exception -> assertThat(((MemberException) exception).getCode())
                            .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }

        private ExerciseGuestInviteCommand guestInviteCommand(Long inviterId) {
            return ExerciseGuestInviteCommand.builder()
                    .guestName("테스트게스트")
                    .gender(Gender.MALE)
                    .level(Level.B)
                    .inviterId(inviterId)
                    .build();
        }
    }

    @Nested
    @DisplayName("cancelGuestInvitation")
    class CancelGuestInvitation {

        @Test
        @DisplayName("초대자가 본인 게스트를 취소하면 Response를 반환한다")
        void cancelGuestInvitation_success() {
            Guest guest = createGuest(exercise, manager.getId(), 60L);
            exercise.getGameBoard().addGameBoardMember(GameBoardMember.createFromGuest(guest));
            given(guestReader.findByIdOrThrow(guest.getId())).willReturn(guest);

            ExerciseCancelResult result = exerciseGuestCommandService
                    .cancelGuestInvitation(exercise.getId(), guest.getId(), manager.getId());

            assertThat(result.memberName()).isEqualTo("게스트");
            assertThat(result.currentParticipants()).isNotNull();
            then(guestRepository).should().delete(guest);
            assertThat(exercise.getGameBoard().getGameBoardMembers()).isEmpty();
        }

        @Test
        @DisplayName("이미 시작된 운동이면 ExerciseException(EXERCISE_ALREADY_STARTED_CANCEL)을 던진다")
        void alreadyStarted_throwsException() {
            Exercise startedExercise = ExerciseFixture.createExercise(party, LocalDate.of(2000, 1, 1),
                    LocalTime.of(12, 0), true, false);
            ReflectionTestUtils.setField(startedExercise, "id", 200L);
            Guest guest = createGuest(startedExercise, manager.getId(), 60L);
            given(exerciseReader.findByIdOrThrow(startedExercise.getId())).willReturn(startedExercise);
            given(guestReader.findByIdOrThrow(guest.getId())).willReturn(guest);

            assertThatThrownBy(() -> exerciseGuestCommandService
                    .cancelGuestInvitation(startedExercise.getId(), guest.getId(), manager.getId()))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL));
        }

        @Test
        @DisplayName("게스트가 해당 운동에 속하지 않으면 ExerciseException(GUEST_IS_NOT_PARTICIPATED_IN_EXERCISE)을 던진다")
        void guestNotInExercise_throwsException() {
            Exercise otherExercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                    LocalTime.of(12, 0), true, false);
            ReflectionTestUtils.setField(otherExercise, "id", 201L);
            Guest guest = createGuest(otherExercise, manager.getId(), 60L);
            given(guestReader.findByIdOrThrow(guest.getId())).willReturn(guest);

            assertThatThrownBy(() -> exerciseGuestCommandService
                    .cancelGuestInvitation(exercise.getId(), guest.getId(), manager.getId()))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.GUEST_IS_NOT_PARTICIPATED_IN_EXERCISE));
        }

        @Test
        @DisplayName("본인이 초대하지 않은 게스트면 ExerciseException(GUEST_NOT_INVITED_BY_MEMBER)을 던진다")
        void guestNotInvitedByMember_throwsException() {
            Guest guest = createGuest(exercise, 999L, 60L);
            given(guestReader.findByIdOrThrow(guest.getId())).willReturn(guest);

            assertThatThrownBy(() -> exerciseGuestCommandService
                    .cancelGuestInvitation(exercise.getId(), guest.getId(), manager.getId()))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.GUEST_NOT_INVITED_BY_MEMBER));
        }

        @Test
        @DisplayName("운동이 없으면 ExerciseException(EXERCISE_NOT_FOUND)을 던진다")
        void exerciseNotFound_throwsException() {
            given(exerciseReader.findByIdOrThrow(999L))
                    .willThrow(new ExerciseException(ExerciseErrorCode.EXERCISE_NOT_FOUND));

            assertThatThrownBy(() -> exerciseGuestCommandService
                    .cancelGuestInvitation(999L, 60L, manager.getId()))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.EXERCISE_NOT_FOUND));
        }

        @Test
        @DisplayName("멤버가 없으면 MemberException(MEMBER_NOT_FOUND)을 던진다")
        void memberNotFound_throwsException() {
            given(memberLookupService.findByIdOrThrow(999L))
                    .willThrow(new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

            assertThatThrownBy(() -> exerciseGuestCommandService
                    .cancelGuestInvitation(exercise.getId(), 60L, 999L))
                    .isInstanceOf(MemberException.class)
                    .satisfies(exception -> assertThat(((MemberException) exception).getCode())
                            .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("게스트가 없으면 ExerciseException(GUEST_NOT_FOUND)을 던진다")
        void guestNotFound_throwsException() {
            given(guestReader.findByIdOrThrow(999L))
                    .willThrow(new ExerciseException(ExerciseErrorCode.GUEST_NOT_FOUND));

            assertThatThrownBy(() -> exerciseGuestCommandService
                    .cancelGuestInvitation(exercise.getId(), 999L, manager.getId()))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.GUEST_NOT_FOUND));
        }

        @Test
        @DisplayName("게임에 편성된 게스트는 초대를 취소할 수 없다")
        void assignedGuest_throwsException() {
            Guest guest = createGuest(exercise, manager.getId(), 60L);
            given(guestReader.findByIdOrThrow(guest.getId())).willReturn(guest);
            given(gamePlayerRepository.existsByGuestSource(
                    exercise.getGameBoard().getId(), guest.getId()))
                    .willReturn(true);

            assertThatThrownBy(() -> exerciseGuestCommandService
                    .cancelGuestInvitation(exercise.getId(), guest.getId(), manager.getId()))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.ASSIGNED_PLAYER_CANNOT_CANCEL));

            then(guestRepository).should(never()).delete(guest);
        }

        private Guest createGuest(Exercise guestExercise, Long inviterId, Long guestId) {
            Guest guest = GuestFixture.createGuest(guestExercise, inviterId);
            ReflectionTestUtils.setField(guest, "id", guestId);
            return guest;
        }
    }
}
