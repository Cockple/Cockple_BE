package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.exercise.converter.command.ExerciseParticipationCommandMapper;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseJoinDTO;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseParticipationCommandService;
import umc.cockple.demo.domain.exercise.service.support.reader.MemberExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.exercise.domain.MemberExercise;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseParticipationCommandService")
class ExerciseParticipationCommandServiceTest {

    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private ExerciseReader exerciseReader;
    @Mock private GuestReader guestReader;
    @Mock private MemberExerciseReader memberExerciseReader;
    @Mock private MemberLookupService memberLookupService;

    private ExerciseParticipationCommandService exerciseParticipationCommandService;

    private Member manager;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        MemberPartyLookupService memberPartyLookupService = new MemberPartyLookupService(memberPartyRepository);
        ExerciseValidator exerciseValidator = new ExerciseValidator(
                memberPartyLookupService, memberExerciseRepository);
        ExerciseParticipationCommandMapper exerciseParticipationMapper = new ExerciseParticipationCommandMapper();
        exerciseParticipationCommandService = new ExerciseParticipationCommandService(
                memberExerciseRepository,
                guestRepository,
                exerciseReader,
                guestReader,
                memberExerciseReader,
                memberLookupService,
                memberPartyLookupService,
                exerciseValidator,
                exerciseParticipationMapper);

        manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        exercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                LocalTime.of(12, 0), true, false);
        ReflectionTestUtils.setField(exercise, "id", 100L);

        lenient().when(exerciseReader.findByIdOrThrow(exercise.getId())).thenReturn(exercise);
        lenient().when(exerciseReader.findByIdWithPartyLevelsOrThrow(exercise.getId())).thenReturn(exercise);
        lenient().when(memberLookupService.findByIdOrThrow(manager.getId())).thenReturn(manager);
    }

    @Nested
    @DisplayName("joinExercise")
    class JoinExercise {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("파티 멤버가 운동 신청하면 Response를 반환한다")
            void partyMember_joinExercise_success() {
                // given
                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L, LocalDate.of(2000, 1, 1));
                ReflectionTestUtils.setField(participant, "id", 2L);

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(memberExerciseRepository.existsByExerciseAndMember(exercise, participant)).willReturn(false);
                given(memberPartyRepository.existsByPartyAndMember(party, participant)).willReturn(true);
                given(memberExerciseRepository.save(any(MemberExercise.class)))
                        .willAnswer(invocation -> {
                            MemberExercise me = invocation.getArgument(0);
                            ReflectionTestUtils.setField(me, "id", 50L);
                            return me;
                        });

                // when
                ExerciseJoinDTO.Response response = exerciseParticipationCommandService.joinExercise(exercise.getId(), participant.getId());

                // then
                ArgumentCaptor<MemberExercise> participationCaptor = ArgumentCaptor.forClass(MemberExercise.class);
                then(memberExerciseRepository).should().save(participationCaptor.capture());

                MemberExercise savedParticipation = participationCaptor.getValue();
                assertThat(response.participantId()).isEqualTo(50L);
                assertThat(response.currentParticipants()).isEqualTo(1);
                assertThat(savedParticipation.getMember()).isSameAs(participant);
                assertThat(savedParticipation.getExercise()).isSameAs(exercise);
                assertThat(savedParticipation.getExerciseMemberShipStatus())
                        .isEqualTo(ExerciseMemberShipStatus.PARTY_MEMBER);
            }

            @Test
            @DisplayName("외부 참여자가 outsideGuestAccept=true 운동 신청하면 Response를 반환한다")
            void outsideMember_joinExercise_success() {
                // given
                Exercise outsideAcceptExercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                        LocalTime.of(12, 0), true, true);
                ReflectionTestUtils.setField(outsideAcceptExercise, "id", 101L);

                Member outsideMember = MemberFixture.createMember("외부참여자", Gender.FEMALE, Level.C, 3001L, LocalDate.of(2000, 1, 1));
                ReflectionTestUtils.setField(outsideMember, "id", 3L);

                given(exerciseReader.findByIdWithPartyLevelsOrThrow(outsideAcceptExercise.getId()))
                        .willReturn(outsideAcceptExercise);
                given(memberLookupService.findByIdOrThrow(outsideMember.getId())).willReturn(outsideMember);
                given(memberExerciseRepository.existsByExerciseAndMember(outsideAcceptExercise, outsideMember)).willReturn(false);
                given(memberPartyRepository.existsByPartyAndMember(party, outsideMember)).willReturn(false);
                given(memberExerciseRepository.save(any(MemberExercise.class)))
                        .willAnswer(invocation -> {
                            MemberExercise me = invocation.getArgument(0);
                            ReflectionTestUtils.setField(me, "id", 51L);
                            return me;
                        });

                // when
                ExerciseJoinDTO.Response response = exerciseParticipationCommandService
                        .joinExercise(outsideAcceptExercise.getId(), outsideMember.getId());

                // then
                ArgumentCaptor<MemberExercise> participationCaptor = ArgumentCaptor.forClass(MemberExercise.class);
                then(memberExerciseRepository).should().save(participationCaptor.capture());

                MemberExercise savedParticipation = participationCaptor.getValue();
                assertThat(response.participantId()).isEqualTo(51L);
                assertThat(response.currentParticipants()).isEqualTo(1);
                assertThat(savedParticipation.getMember()).isSameAs(outsideMember);
                assertThat(savedParticipation.getExercise()).isSameAs(outsideAcceptExercise);
                assertThat(savedParticipation.getExerciseMemberShipStatus())
                        .isEqualTo(ExerciseMemberShipStatus.EXTERNAL_PARTICIPANT);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("이미 시작된 운동이면 ExerciseException(EXERCISE_ALREADY_STARTED_PARTICIPATION)을 던진다")
            void alreadyStarted_throwsException() {
                Exercise startedExercise = ExerciseFixture.createExercise(party, LocalDate.of(2000, 1, 1),
                        LocalTime.of(12, 0), true, false);
                ReflectionTestUtils.setField(startedExercise, "id", 200L);

                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
                ReflectionTestUtils.setField(participant, "id", 2L);

                given(exerciseReader.findByIdWithPartyLevelsOrThrow(startedExercise.getId()))
                        .willReturn(startedExercise);
                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.joinExercise(startedExercise.getId(), participant.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_PARTICIPATION));
            }

            @Test
            @DisplayName("이미 참여 신청한 운동이면 ExerciseException(ALREADY_JOINED_EXERCISE)을 던진다")
            void alreadyJoined_throwsException() {
                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
                ReflectionTestUtils.setField(participant, "id", 2L);

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(memberExerciseRepository.existsByExerciseAndMember(exercise, participant)).willReturn(true);

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.joinExercise(exercise.getId(), participant.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.ALREADY_JOINED_EXERCISE));
            }

            @Test
            @DisplayName("파티 멤버가 아닌데 외부 참여 불가 운동이면 ExerciseException(NOT_PARTY_MEMBER)을 던진다")
            void notPartyMember_outsideNotAccepted_throwsException() {
                Member outsideMember = MemberFixture.createMember("외부인", Gender.MALE, Level.B, 3001L);
                ReflectionTestUtils.setField(outsideMember, "id", 3L);

                given(memberLookupService.findByIdOrThrow(outsideMember.getId())).willReturn(outsideMember);
                given(memberExerciseRepository.existsByExerciseAndMember(exercise, outsideMember)).willReturn(false);
                given(memberPartyRepository.existsByPartyAndMember(party, outsideMember)).willReturn(false);

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.joinExercise(exercise.getId(), outsideMember.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.NOT_PARTY_MEMBER));
            }

            @Test
            @DisplayName("나이 조건 불일치면 ExerciseException(MEMBER_AGE_NOT_ALLOWED)을 던진다")
            void ageNotAllowed_throwsException() {
                Member youngMember = MemberFixture.createMember("어린회원", Gender.MALE, Level.B, 4001L, LocalDate.of(2010, 1, 1));
                ReflectionTestUtils.setField(youngMember, "id", 4L);

                given(memberLookupService.findByIdOrThrow(youngMember.getId())).willReturn(youngMember);
                given(memberExerciseRepository.existsByExerciseAndMember(exercise, youngMember)).willReturn(false);
                given(memberPartyRepository.existsByPartyAndMember(party, youngMember)).willReturn(true);

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.joinExercise(exercise.getId(), youngMember.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.MEMBER_AGE_NOT_ALLOWED));
            }
        }
    }




    @Nested
    @DisplayName("cancelParticipation")
    class CancelParticipation {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("참여자가 본인 참여를 취소하면 Response를 반환한다")
            void cancelParticipation_success() {
                // given
                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
                ReflectionTestUtils.setField(participant, "id", 2L);

                MemberExercise memberExercise = MemberFixture.createMemberExercise(participant, exercise);
                ReflectionTestUtils.setField(memberExercise, "id", 50L);

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(memberExerciseReader.findMemberExerciseOrThrow(exercise, participant))
                        .willReturn(memberExercise);

                // when
                ExerciseCancelDTO.Response response = exerciseParticipationCommandService
                        .cancelParticipation(exercise.getId(), participant.getId());

                // then
                assertThat(response.memberName()).isEqualTo(participant.getMemberName());
                assertThat(response.currentParticipants()).isNotNull();
                then(memberExerciseRepository).should().delete(memberExercise);
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

                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
                ReflectionTestUtils.setField(participant, "id", 2L);

                MemberExercise memberExercise = MemberFixture.createMemberExercise(participant, startedExercise);
                ReflectionTestUtils.setField(memberExercise, "id", 50L);

                given(exerciseReader.findByIdOrThrow(startedExercise.getId())).willReturn(startedExercise);
                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(memberExerciseReader.findMemberExerciseOrThrow(startedExercise, participant))
                        .willReturn(memberExercise);

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.cancelParticipation(startedExercise.getId(), participant.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL));
            }

            @Test
            @DisplayName("참여 기록이 없으면 ExerciseException(MEMBER_EXERCISE_NOT_FOUND)을 던진다")
            void memberExerciseNotFound_throwsException() {
                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
                ReflectionTestUtils.setField(participant, "id", 2L);

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(memberExerciseReader.findMemberExerciseOrThrow(exercise, participant))
                        .willThrow(new ExerciseException(ExerciseErrorCode.MEMBER_EXERCISE_NOT_FOUND));

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.cancelParticipation(exercise.getId(), participant.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.MEMBER_EXERCISE_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("cancelParticipationByManager")
    class CancelParticipationByManager {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("모임장이 일반 멤버 참여를 취소하면 Response를 반환한다")
            void ownerCancelsMemberParticipation_success() {
                // given
                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
                ReflectionTestUtils.setField(participant, "id", 2L);

                MemberExercise memberExercise = MemberFixture.createMemberExercise(participant, exercise);
                ReflectionTestUtils.setField(memberExercise, "id", 50L);

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(memberExerciseReader.findMemberExerciseOrThrow(exercise, participant))
                        .willReturn(memberExercise);

                // when
                ExerciseCancelDTO.Response response = exerciseParticipationCommandService
                        .cancelParticipationByManager(exercise.getId(), participant.getId(), manager.getId(), request);

                // then
                assertThat(response.memberName()).isEqualTo(participant.getMemberName());
                then(memberExerciseRepository).should().delete(memberExercise);
            }

            @Test
            @DisplayName("부모임장도 일반 멤버 참여를 취소할 수 있다")
            void subManagerCancelsMemberParticipation_success() {
                // given
                Member subManager = MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1002L);
                ReflectionTestUtils.setField(subManager, "id", 2L);

                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
                ReflectionTestUtils.setField(participant, "id", 3L);

                MemberExercise memberExercise = MemberFixture.createMemberExercise(participant, exercise);
                ReflectionTestUtils.setField(memberExercise, "id", 50L);

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.PARTY_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.PARTY_SUBMANAGER))
                        .willReturn(true);
                given(memberLookupService.findByIdOrThrow(subManager.getId())).willReturn(subManager);
                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(memberExerciseReader.findMemberExerciseOrThrow(exercise, participant))
                        .willReturn(memberExercise);

                // when
                ExerciseCancelDTO.Response response = exerciseParticipationCommandService
                        .cancelParticipationByManager(exercise.getId(), participant.getId(), subManager.getId(), request);

                // then
                assertThat(response.memberName()).isEqualTo(participant.getMemberName());
                then(memberExerciseRepository).should().delete(memberExercise);
            }

            @Test
            @DisplayName("모임장이 게스트 참여를 취소하면 Response를 반환한다")
            void ownerCancelsGuestParticipation_success() {
                // given
                Guest guest = GuestFixture.createGuest(exercise, manager.getId());
                ReflectionTestUtils.setField(guest, "id", 60L);

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(true);

                given(guestReader.findByIdOrThrow(guest.getId())).willReturn(guest);

                // when
                ExerciseCancelDTO.Response response = exerciseParticipationCommandService
                        .cancelParticipationByManager(exercise.getId(), guest.getId(), manager.getId(), request);

                // then
                assertThat(response.memberName()).isEqualTo("게스트");
                then(guestRepository).should().delete(guest);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("일반 멤버가 취소 시 ExerciseException(INSUFFICIENT_PERMISSION)을 던진다")
            void normalMember_throwsException() {
                Member normalMember = MemberFixture.createMember("일반멤버", Gender.FEMALE, Level.B, 1002L);
                ReflectionTestUtils.setField(normalMember, "id", 2L);

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), normalMember.getId(), Role.PARTY_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), normalMember.getId(), Role.PARTY_SUBMANAGER))
                        .willReturn(false);
                given(memberLookupService.findByIdOrThrow(normalMember.getId())).willReturn(normalMember);

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.cancelParticipationByManager(
                                exercise.getId(), 2L, normalMember.getId(), request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.INSUFFICIENT_PERMISSION));
            }

            @Test
            @DisplayName("이미 시작된 운동이면 ExerciseException(EXERCISE_ALREADY_STARTED_CANCEL)을 던진다")
            void alreadyStarted_throwsException() {
                Exercise startedExercise = ExerciseFixture.createExercise(party, LocalDate.of(2000, 1, 1),
                        LocalTime.of(12, 0), true, false);
                ReflectionTestUtils.setField(startedExercise, "id", 200L);

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);
                given(exerciseReader.findByIdOrThrow(startedExercise.getId())).willReturn(startedExercise);

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.cancelParticipationByManager(
                                startedExercise.getId(), 2L, manager.getId(), request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL));
            }

            @Test
            @DisplayName("존재하지 않는 멤버 참여자면 MemberException(MEMBER_NOT_FOUND)을 던진다")
            void memberParticipantNotFound_throwsException() {
                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                given(memberLookupService.findByIdOrThrow(999L))
                        .willThrow(new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.cancelParticipationByManager(exercise.getId(), 999L, manager.getId(), request))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
            }

            @Test
            @DisplayName("존재하지 않는 게스트면 ExerciseException(GUEST_NOT_FOUND)을 던진다")
            void guestNotFound_throwsException() {
                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(true);

                given(guestReader.findByIdOrThrow(999L))
                        .willThrow(new ExerciseException(ExerciseErrorCode.GUEST_NOT_FOUND));

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.cancelParticipationByManager(exercise.getId(), 999L, manager.getId(), request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.GUEST_NOT_FOUND));
            }
        }
    }
}
