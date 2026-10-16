package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.hibernate.exception.ConstraintViolationException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.exercise.events.ExerciseAttendanceChangedEvent;
import umc.cockple.demo.domain.exercise.events.ExerciseJoinedEvent;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.repository.GamePlayerRepository;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseParticipationCommandService;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCancelByManagerCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseCancelResult;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseJoinResult;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseParticipationReader;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.calculator.ExerciseParticipantPositionCalculator;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.exercise.domain.ExerciseParticipation;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.exercise.repository.ExerciseParticipationRepository;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
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
import java.util.List;
import java.time.LocalTime;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseParticipationCommandService")
class ExerciseParticipationCommandServiceTest {

    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private ExerciseParticipationRepository exerciseParticipationRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private ExerciseReader exerciseReader;
    @Mock private GuestReader guestReader;
    @Mock private ExerciseParticipationReader exerciseParticipationReader;
    @Mock private MemberLookupService memberLookupService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private GamePlayerRepository gamePlayerRepository;

    private ExerciseParticipationCommandService exerciseParticipationCommandService;

    private Member manager;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        MemberPartyLookupService memberPartyLookupService = new MemberPartyLookupService(memberPartyRepository);
        ExerciseValidator exerciseValidator = new ExerciseValidator(
                memberPartyLookupService, exerciseParticipationRepository);
        exerciseParticipationCommandService = new ExerciseParticipationCommandService(
                exerciseParticipationRepository,
                guestRepository,
                exerciseReader,
                guestReader,
                exerciseParticipationReader,
                memberLookupService,
                memberPartyLookupService,
                eventPublisher,
                exerciseValidator,
                new ExerciseGameAssignmentValidator(gamePlayerRepository),
                new ExerciseWaitingPromotionService(
                        new ExerciseParticipantPositionCalculator(),
                        eventPublisher));

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
                given(exerciseParticipationRepository.existsByExerciseAndMember(exercise, participant)).willReturn(false);
                given(memberPartyRepository.existsByPartyAndMember(party, participant)).willReturn(true);
                given(exerciseParticipationRepository.saveAndFlush(any(ExerciseParticipation.class)))
                        .willAnswer(invocation -> {
                            ExerciseParticipation me = invocation.getArgument(0);
                            ReflectionTestUtils.setField(me, "id", 50L);
                            return me;
                        });

                // when
                ExerciseJoinResult response = exerciseParticipationCommandService.joinExercise(exercise.getId(), participant.getId());

                // then
                ArgumentCaptor<ExerciseParticipation> participationCaptor = ArgumentCaptor.forClass(ExerciseParticipation.class);
                then(exerciseParticipationRepository).should().saveAndFlush(participationCaptor.capture());

                ExerciseParticipation savedParticipation = participationCaptor.getValue();
                assertThat(response.participantId()).isEqualTo(50L);
                assertThat(response.currentParticipants()).isEqualTo(1);
                assertThat(savedParticipation.getMember()).isSameAs(participant);
                assertThat(savedParticipation.getExercise()).isSameAs(exercise);
                assertThat(savedParticipation.getExerciseMemberShipStatus())
                        .isEqualTo(ExerciseMemberShipStatus.PARTY_MEMBER);
                assertThat(exercise.getGameBoard().getGameBoardMembers())
                        .singleElement()
                        .satisfies(gameBoardMember -> {
                            assertThat(gameBoardMember.getMember()).isSameAs(participant);
                            assertThat(gameBoardMember.getName()).isEqualTo(participant.getMemberName());
                            assertThat(gameBoardMember.getGender()).isEqualTo(participant.getGender());
                            assertThat(gameBoardMember.getLevel()).isEqualTo(participant.getLevel());
                        });
                then(eventPublisher).should().publishEvent(
                        GameBoardMembersChangedEvent.membersOnly(
                                exercise.getGameBoard().getId(), participant.getId()));
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
                given(exerciseParticipationRepository.existsByExerciseAndMember(outsideAcceptExercise, outsideMember)).willReturn(false);
                given(memberPartyRepository.existsByPartyAndMember(party, outsideMember)).willReturn(false);
                given(exerciseParticipationRepository.saveAndFlush(any(ExerciseParticipation.class)))
                        .willAnswer(invocation -> {
                            ExerciseParticipation me = invocation.getArgument(0);
                            ReflectionTestUtils.setField(me, "id", 51L);
                            return me;
                        });

                // when
                ExerciseJoinResult response = exerciseParticipationCommandService
                        .joinExercise(outsideAcceptExercise.getId(), outsideMember.getId());

                // then
                ArgumentCaptor<ExerciseParticipation> participationCaptor = ArgumentCaptor.forClass(ExerciseParticipation.class);
                then(exerciseParticipationRepository).should().saveAndFlush(participationCaptor.capture());

                ExerciseParticipation savedParticipation = participationCaptor.getValue();
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
            @DisplayName("동시 참가로 유니크 제약이 충돌하면 이미 참여한 운동 오류로 변환한다")
            void duplicateConstraint_throwsAlreadyJoinedExercise() {
                Member participant = MemberFixture.createMember(
                        "참여자", Gender.MALE, Level.B, 2001L, LocalDate.of(2000, 1, 1));
                ReflectionTestUtils.setField(participant, "id", 2L);

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(exerciseParticipationRepository.existsByExerciseAndMember(exercise, participant)).willReturn(false);
                given(memberPartyRepository.existsByPartyAndMember(party, participant)).willReturn(true);
                given(exerciseParticipationRepository.saveAndFlush(any(ExerciseParticipation.class)))
                        .willThrow(uniqueConflict("uk_member_exercise_exercise_member"));

                assertThatThrownBy(() -> exerciseParticipationCommandService
                        .joinExercise(exercise.getId(), participant.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                                .isEqualTo(ExerciseErrorCode.ALREADY_JOINED_EXERCISE));

                then(eventPublisher).should(never())
                        .publishEvent(any(ExerciseAttendanceChangedEvent.class));
                then(eventPublisher).should(never()).publishEvent(any(ExerciseJoinedEvent.class));
            }

            @Test
            @DisplayName("참가 중복과 무관한 무결성 위반은 원래 예외를 전파한다")
            void otherConstraint_rethrowsOriginalException() {
                Member participant = MemberFixture.createMember(
                        "참여자", Gender.MALE, Level.B, 2001L, LocalDate.of(2000, 1, 1));
                ReflectionTestUtils.setField(participant, "id", 2L);
                DataIntegrityViolationException conflict = uniqueConflict("other_constraint");

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(exerciseParticipationRepository.existsByExerciseAndMember(exercise, participant)).willReturn(false);
                given(memberPartyRepository.existsByPartyAndMember(party, participant)).willReturn(true);
                given(exerciseParticipationRepository.saveAndFlush(any(ExerciseParticipation.class)))
                        .willThrow(conflict);

                assertThatThrownBy(() -> exerciseParticipationCommandService
                        .joinExercise(exercise.getId(), participant.getId()))
                        .isSameAs(conflict);
            }

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
                given(exerciseParticipationRepository.existsByExerciseAndMember(exercise, participant)).willReturn(true);

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
                given(exerciseParticipationRepository.existsByExerciseAndMember(exercise, outsideMember)).willReturn(false);
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
                given(exerciseParticipationRepository.existsByExerciseAndMember(exercise, youngMember)).willReturn(false);
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

                ExerciseParticipation exerciseParticipation = MemberFixture.createExerciseParticipation(participant, exercise);
                ReflectionTestUtils.setField(exerciseParticipation, "id", 50L);
                exercise.getGameBoard().addGameBoardMember(
                        GameBoardMember.createFromMember(participant, exercise.getDate()));

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(exerciseParticipationReader.findExerciseParticipationOrThrow(exercise, participant))
                        .willReturn(exerciseParticipation);

                // when
                ExerciseCancelResult response = exerciseParticipationCommandService
                        .cancelParticipation(exercise.getId(), participant.getId());

                // then
                assertThat(response.memberName()).isEqualTo(participant.getMemberName());
                assertThat(response.currentParticipants()).isNotNull();
                then(exerciseParticipationRepository).should().delete(exerciseParticipation);
                assertThat(exercise.getGameBoard().getGameBoardMembers()).isEmpty();
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

                ExerciseParticipation exerciseParticipation = MemberFixture.createExerciseParticipation(participant, startedExercise);
                ReflectionTestUtils.setField(exerciseParticipation, "id", 50L);

                given(exerciseReader.findByIdOrThrow(startedExercise.getId())).willReturn(startedExercise);
                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(exerciseParticipationReader.findExerciseParticipationOrThrow(startedExercise, participant))
                        .willReturn(exerciseParticipation);

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.cancelParticipation(startedExercise.getId(), participant.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL));
            }

            @Test
            @DisplayName("참여 기록이 없으면 ExerciseException(EXERCISE_PARTICIPATION_NOT_FOUND)을 던진다")
            void exerciseParticipationNotFound_throwsException() {
                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
                ReflectionTestUtils.setField(participant, "id", 2L);

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(exerciseParticipationReader.findExerciseParticipationOrThrow(exercise, participant))
                        .willThrow(new ExerciseException(ExerciseErrorCode.EXERCISE_PARTICIPATION_NOT_FOUND));

                assertThatThrownBy(() ->
                        exerciseParticipationCommandService.cancelParticipation(exercise.getId(), participant.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_PARTICIPATION_NOT_FOUND));
            }

            @Test
            @DisplayName("게임에 편성된 참여자는 본인 참여를 취소할 수 없다")
            void assignedPlayer_throwsException() {
                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
                ReflectionTestUtils.setField(participant, "id", 2L);
                ExerciseParticipation exerciseParticipation = MemberFixture.createExerciseParticipation(participant, exercise);

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(exerciseParticipationReader.findExerciseParticipationOrThrow(exercise, participant))
                        .willReturn(exerciseParticipation);
                given(gamePlayerRepository.existsByMemberSource(
                        exercise.getGameBoard().getId(), participant.getId()))
                        .willReturn(true);

                assertThatThrownBy(() -> exerciseParticipationCommandService
                        .cancelParticipation(exercise.getId(), participant.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                                .isEqualTo(ExerciseErrorCode.ASSIGNED_PLAYER_CANNOT_CANCEL));

                then(exerciseParticipationRepository).should(never()).delete(any(ExerciseParticipation.class));
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

                ExerciseParticipation exerciseParticipation = MemberFixture.createExerciseParticipation(participant, exercise);
                ReflectionTestUtils.setField(exerciseParticipation, "id", 50L);
                exercise.getGameBoard().addGameBoardMember(
                        GameBoardMember.createFromMember(participant, exercise.getDate()));

                ExerciseCancelByManagerCommand request = new ExerciseCancelByManagerCommand(false);

                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(exerciseParticipationReader.findExerciseParticipationOrThrow(exercise, participant))
                        .willReturn(exerciseParticipation);

                // when
                ExerciseCancelResult response = exerciseParticipationCommandService
                        .cancelParticipationByManager(exercise.getId(), participant.getId(), manager.getId(), request);

                // then
                assertThat(response.memberName()).isEqualTo(participant.getMemberName());
                then(exerciseParticipationRepository).should().delete(exerciseParticipation);
                assertThat(exercise.getGameBoard().getGameBoardMembers()).isEmpty();
            }

            @Test
            @DisplayName("부모임장도 일반 멤버 참여를 취소할 수 있다")
            void subManagerCancelsMemberParticipation_success() {
                // given
                Member subManager = MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1002L);
                ReflectionTestUtils.setField(subManager, "id", 2L);

                Member participant = MemberFixture.createMember("참여자", Gender.MALE, Level.B, 2001L);
                ReflectionTestUtils.setField(participant, "id", 3L);

                ExerciseParticipation exerciseParticipation = MemberFixture.createExerciseParticipation(participant, exercise);
                ReflectionTestUtils.setField(exerciseParticipation, "id", 50L);

                ExerciseCancelByManagerCommand request = new ExerciseCancelByManagerCommand(false);

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.PARTY_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.PARTY_SUBMANAGER))
                        .willReturn(true);
                given(memberLookupService.findByIdOrThrow(subManager.getId())).willReturn(subManager);
                given(memberLookupService.findByIdOrThrow(participant.getId())).willReturn(participant);
                given(exerciseParticipationReader.findExerciseParticipationOrThrow(exercise, participant))
                        .willReturn(exerciseParticipation);

                // when
                ExerciseCancelResult response = exerciseParticipationCommandService
                        .cancelParticipationByManager(exercise.getId(), participant.getId(), subManager.getId(), request);

                // then
                assertThat(response.memberName()).isEqualTo(participant.getMemberName());
                then(exerciseParticipationRepository).should().delete(exerciseParticipation);
            }

            @Test
            @DisplayName("모임장이 게스트 참여를 취소하면 Response를 반환한다")
            void ownerCancelsGuestParticipation_success() {
                // given
                Guest guest = GuestFixture.createGuest(exercise, manager.getId());
                ReflectionTestUtils.setField(guest, "id", 60L);
                exercise.getGameBoard().addGameBoardMember(GameBoardMember.createFromGuest(guest));

                ExerciseCancelByManagerCommand request = new ExerciseCancelByManagerCommand(true);

                given(guestReader.findByIdOrThrow(guest.getId())).willReturn(guest);

                // when
                ExerciseCancelResult response = exerciseParticipationCommandService
                        .cancelParticipationByManager(exercise.getId(), guest.getId(), manager.getId(), request);

                // then
                assertThat(response.memberName()).isEqualTo("게스트");
                then(guestRepository).should().delete(guest);
                assertThat(exercise.getGameBoard().getGameBoardMembers()).isEmpty();
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

                ExerciseCancelByManagerCommand request = new ExerciseCancelByManagerCommand(false);

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

                ExerciseCancelByManagerCommand request = new ExerciseCancelByManagerCommand(false);
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
                ExerciseCancelByManagerCommand request = new ExerciseCancelByManagerCommand(false);

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
                ExerciseCancelByManagerCommand request = new ExerciseCancelByManagerCommand(true);

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

    @Nested
    @DisplayName("확정 참여 신청 알림")
    class JoinedEventPublishing {

        private Member applicant;

        @BeforeEach
        void setUpApplicant() {
            applicant = MemberFixture.createMember("신청자", Gender.MALE, Level.B, 4001L,
                    LocalDate.of(2000, 1, 1));
            ReflectionTestUtils.setField(applicant, "id", 4L);
            lenient().when(memberLookupService.findByIdOrThrow(applicant.getId())).thenReturn(applicant);
            lenient().when(exerciseParticipationRepository.saveAndFlush(any(ExerciseParticipation.class)))
                    .thenAnswer(invocation -> {
                        ExerciseParticipation participation = invocation.getArgument(0);
                        ReflectionTestUtils.setField(participation, "createdAt", LocalDate.of(2099, 1, 10).atStartOfDay());
                        return participation;
                    });
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("정원의 마지막 자리로 확정 신청하면 기존 운동 모임원 전원에게 알린다")
        void confirmedJoinNotifiesOnlyExistingPartyParticipants(boolean applicantIsPartyMember) {
            ReflectionTestUtils.setField(exercise, "maxCapacity", 5);
            ReflectionTestUtils.setField(exercise, "outsideGuestAccept", true);
            given(memberPartyRepository.existsByPartyAndMember(party, applicant)).willReturn(applicantIsPartyMember);
            exercise.addParticipation(manager, ExerciseMemberShipStatus.PARTY_MEMBER);
            Member regularMember = MemberFixture.createMember("일반회원", Gender.FEMALE, Level.B, 2001L);
            ReflectionTestUtils.setField(regularMember, "id", 2L);
            exercise.addParticipation(regularMember, ExerciseMemberShipStatus.PARTY_MEMBER);
            Member externalMember = MemberFixture.createMember("외부회원", Gender.MALE, Level.B, 3001L);
            ReflectionTestUtils.setField(externalMember, "id", 3L);
            exercise.addParticipation(externalMember, ExerciseMemberShipStatus.EXTERNAL_PARTICIPANT);
            GuestFixture.createGuest(exercise, manager.getId());

            exerciseParticipationCommandService.joinExercise(exercise.getId(), applicant.getId());

            ArgumentCaptor<ExerciseJoinedEvent> captor = ArgumentCaptor.forClass(ExerciseJoinedEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());
            ExerciseJoinedEvent event = captor.getValue();
            assertThat(event.recipientMemberIds()).containsExactlyInAnyOrder(manager.getId(), regularMember.getId());
            assertThat(event.participantName()).isEqualTo(applicant.getDisplayName());
            assertThat(event.exerciseId()).isEqualTo(exercise.getId());
            assertThat(event.exerciseDate()).isEqualTo(exercise.getDate());
            assertThat(event.partyId()).isEqualTo(party.getId());
            then(eventPublisher).should(never()).publishEvent(any(ExerciseAttendanceChangedEvent.class));
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("회원 또는 게스트로 정원이 찼다면 신규 대기 신청 알림을 보내지 않는다")
        void waitingJoinDoesNotNotify(boolean fullWithGuest) {
            ReflectionTestUtils.setField(exercise, "maxCapacity", 1);
            given(memberPartyRepository.existsByPartyAndMember(party, applicant)).willReturn(true);
            if (fullWithGuest) {
                Guest confirmed = GuestFixture.createGuest(exercise, manager.getId());
                ReflectionTestUtils.setField(confirmed, "id", 60L);
                ReflectionTestUtils.setField(confirmed, "createdAt", LocalDate.of(2099, 1, 1).atStartOfDay());
            } else {
                ExerciseParticipation confirmed = exercise.addParticipation(manager, ExerciseMemberShipStatus.PARTY_MEMBER);
                ReflectionTestUtils.setField(confirmed, "createdAt", LocalDate.of(2099, 1, 1).atStartOfDay());
            }

            exerciseParticipationCommandService.joinExercise(exercise.getId(), applicant.getId());

            then(eventPublisher).should(never()).publishEvent(any(ExerciseJoinedEvent.class));
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("확정 신청이어도 수신할 기존 운동 모임원이 없으면 알림을 보내지 않는다")
        void noPartyRecipientsDoesNotNotify(boolean externalParticipantExists) {
            given(memberPartyRepository.existsByPartyAndMember(party, applicant)).willReturn(true);
            if (externalParticipantExists) {
                exercise.addParticipation(manager, ExerciseMemberShipStatus.EXTERNAL_PARTICIPANT);
            }

            exerciseParticipationCommandService.joinExercise(exercise.getId(), applicant.getId());

            then(eventPublisher).should(never()).publishEvent(any(ExerciseJoinedEvent.class));
        }
    }

    @Nested
    @DisplayName("대기자 확정 전환 알림")
    class AttendanceChangedEventPublishing {

        private Member subManager;
        private Member normalMember;

        @BeforeEach
        void setUpPartyRoles() {
            party.addMember(MemberParty.createOwner(manager, party));
            subManager = MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 5001L,
                    LocalDate.of(2000, 1, 1));
            ReflectionTestUtils.setField(subManager, "id", 5L);
            normalMember = MemberFixture.createMember("일반멤버", Gender.MALE, Level.C, 6001L,
                    LocalDate.of(2000, 1, 1));
            ReflectionTestUtils.setField(normalMember, "id", 6L);
            ReflectionTestUtils.setField(exercise, "maxCapacity", 1);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("신규 신청은 확정·대기 여부와 관계없이 확정 전환 알림을 보내지 않는다")
        void joinDoesNotPublishPromotion(boolean full) {
            if (full) {
                ExerciseParticipation confirmed = exercise.addParticipation(manager, ExerciseMemberShipStatus.PARTY_MEMBER);
                ReflectionTestUtils.setField(confirmed, "createdAt", LocalDate.of(2099, 1, 1).atStartOfDay());
            }
            given(memberLookupService.findByIdOrThrow(normalMember.getId())).willReturn(normalMember);
            given(memberPartyRepository.existsByPartyAndMember(party, normalMember)).willReturn(true);
            given(exerciseParticipationRepository.saveAndFlush(any(ExerciseParticipation.class)))
                    .willAnswer(invocation -> {
                        ExerciseParticipation participation = invocation.getArgument(0);
                        ReflectionTestUtils.setField(participation, "createdAt", LocalDate.of(2099, 1, 10).atStartOfDay());
                        return participation;
                    });

            exerciseParticipationCommandService.joinExercise(exercise.getId(), normalMember.getId());

            then(eventPublisher).should(never()).publishEvent(any(ExerciseAttendanceChangedEvent.class));
        }

        @Test
        @DisplayName("확정 회원이 본인 참여를 취소하면 첫 대기 회원 본인에게만 알린다")
        void selfCancellationPromotesFirstWaitingMember() {
            ExerciseParticipation confirmed = exercise.addParticipation(manager, ExerciseMemberShipStatus.PARTY_MEMBER);
            ExerciseParticipation firstWaiting = exercise.addParticipation(subManager, ExerciseMemberShipStatus.PARTY_MEMBER);
            ExerciseParticipation secondWaiting = exercise.addParticipation(normalMember, ExerciseMemberShipStatus.PARTY_MEMBER);
            ReflectionTestUtils.setField(confirmed, "createdAt", LocalDate.of(2099, 1, 1).atStartOfDay());
            ReflectionTestUtils.setField(firstWaiting, "createdAt", LocalDate.of(2099, 1, 2).atStartOfDay());
            ReflectionTestUtils.setField(secondWaiting, "createdAt", LocalDate.of(2099, 1, 3).atStartOfDay());
            given(exerciseParticipationReader.findExerciseParticipationOrThrow(exercise, manager)).willReturn(confirmed);

            exerciseParticipationCommandService.cancelParticipation(exercise.getId(), manager.getId());

            ExerciseAttendanceChangedEvent event = captureAttendanceEvent();
            assertThat(event.exerciseId()).isEqualTo(exercise.getId());
            assertThat(event.subjectMemberId()).isEqualTo(subManager.getId());
            assertThat(event.recipientMemberIds()).containsExactly(subManager.getId());
            assertThat(event.guestName()).isNull();
        }

        @Test
        @DisplayName("대기 회원의 취소는 확정 전환 알림을 보내지 않는다")
        void waitingCancellationDoesNotNotify() {
            ExerciseParticipation confirmed = exercise.addParticipation(manager, ExerciseMemberShipStatus.PARTY_MEMBER);
            ExerciseParticipation waiting = exercise.addParticipation(normalMember, ExerciseMemberShipStatus.PARTY_MEMBER);
            ReflectionTestUtils.setField(confirmed, "createdAt", LocalDate.of(2099, 1, 1).atStartOfDay());
            ReflectionTestUtils.setField(waiting, "createdAt", LocalDate.of(2099, 1, 2).atStartOfDay());
            given(memberLookupService.findByIdOrThrow(normalMember.getId())).willReturn(normalMember);
            given(exerciseParticipationReader.findExerciseParticipationOrThrow(exercise, normalMember)).willReturn(waiting);

            exerciseParticipationCommandService.cancelParticipation(exercise.getId(), normalMember.getId());

            then(eventPublisher).should(never()).publishEvent(any(ExerciseAttendanceChangedEvent.class));
        }

        @Test
        @DisplayName("대기자가 없으면 확정 회원 취소에도 알림을 보내지 않는다")
        void cancellationWithoutWaitingDoesNotNotify() {
            ExerciseParticipation confirmed = exercise.addParticipation(manager, ExerciseMemberShipStatus.PARTY_MEMBER);
            given(exerciseParticipationReader.findExerciseParticipationOrThrow(exercise, manager)).willReturn(confirmed);

            exerciseParticipationCommandService.cancelParticipation(exercise.getId(), manager.getId());

            then(eventPublisher).should(never()).publishEvent(any(ExerciseAttendanceChangedEvent.class));
        }

        @Test
        @DisplayName("운영진이 회원을 취소하면 대기 게스트의 등록자에게 게스트 이름과 함께 알린다")
        void managerCancellationPromotesGuest() {
            ExerciseParticipation confirmed = exercise.addParticipation(normalMember, ExerciseMemberShipStatus.PARTY_MEMBER);
            Guest guest = GuestFixture.createGuest(exercise, subManager.getId(), "초대손님", Gender.MALE);
            ReflectionTestUtils.setField(guest, "id", normalMember.getId());
            ReflectionTestUtils.setField(confirmed, "createdAt", LocalDate.of(2099, 1, 1).atStartOfDay());
            ReflectionTestUtils.setField(guest, "createdAt", LocalDate.of(2099, 1, 2).atStartOfDay());
            given(memberLookupService.findByIdOrThrow(normalMember.getId())).willReturn(normalMember);
            given(exerciseParticipationReader.findExerciseParticipationOrThrow(exercise, normalMember)).willReturn(confirmed);

            exerciseParticipationCommandService.cancelParticipationByManager(
                    exercise.getId(), normalMember.getId(), manager.getId(), new ExerciseCancelByManagerCommand(false));

            ExerciseAttendanceChangedEvent event = captureAttendanceEvent();
            assertThat(event.recipientMemberIds()).containsExactly(subManager.getId());
            assertThat(event.guestName()).isEqualTo("초대손님");
            assertThat(event.subjectMemberId()).isNull();
        }

        @Test
        @DisplayName("운영진이 확정 게스트를 취소하면 대기 회원에게 알린다")
        void managerGuestCancellationPromotesMember() {
            Guest guest = GuestFixture.createGuest(exercise, manager.getId());
            ReflectionTestUtils.setField(guest, "id", normalMember.getId());
            ExerciseParticipation waiting = exercise.addParticipation(normalMember, ExerciseMemberShipStatus.PARTY_MEMBER);
            ReflectionTestUtils.setField(guest, "createdAt", LocalDate.of(2099, 1, 1).atStartOfDay());
            ReflectionTestUtils.setField(waiting, "createdAt", LocalDate.of(2099, 1, 2).atStartOfDay());
            given(guestReader.findByIdOrThrow(guest.getId())).willReturn(guest);

            exerciseParticipationCommandService.cancelParticipationByManager(
                    exercise.getId(), guest.getId(), manager.getId(), new ExerciseCancelByManagerCommand(true));

            ExerciseAttendanceChangedEvent event = captureAttendanceEvent();
            assertThat(event.subjectMemberId()).isEqualTo(normalMember.getId());
            assertThat(event.recipientMemberIds()).containsExactly(normalMember.getId());
        }

        @Test
        @DisplayName("운영진이 대기 게스트를 취소하면 알림을 보내지 않는다")
        void managerWaitingGuestCancellationDoesNotNotify() {
            ExerciseParticipation confirmed = exercise.addParticipation(normalMember, ExerciseMemberShipStatus.PARTY_MEMBER);
            Guest guest = GuestFixture.createGuest(exercise, manager.getId());
            ReflectionTestUtils.setField(guest, "id", 80L);
            ReflectionTestUtils.setField(confirmed, "createdAt", LocalDate.of(2099, 1, 1).atStartOfDay());
            ReflectionTestUtils.setField(guest, "createdAt", LocalDate.of(2099, 1, 2).atStartOfDay());
            given(guestReader.findByIdOrThrow(guest.getId())).willReturn(guest);

            exerciseParticipationCommandService.cancelParticipationByManager(
                    exercise.getId(), guest.getId(), manager.getId(), new ExerciseCancelByManagerCommand(true));

            then(eventPublisher).should(never()).publishEvent(any(ExerciseAttendanceChangedEvent.class));
        }

        private ExerciseAttendanceChangedEvent captureAttendanceEvent() {
            then(eventPublisher).should(never()).publishEvent(any(ExerciseJoinedEvent.class));
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            then(eventPublisher).should(atLeastOnce()).publishEvent(captor.capture());
            List<ExerciseAttendanceChangedEvent> events = captor.getAllValues().stream()
                    .filter(ExerciseAttendanceChangedEvent.class::isInstance)
                    .map(ExerciseAttendanceChangedEvent.class::cast).toList();
            assertThat(events).hasSize(1);
            return events.get(0);
        }
    }

    private DataIntegrityViolationException uniqueConflict(String constraintName) {
        ConstraintViolationException constraintViolation = new ConstraintViolationException(
                "could not execute statement",
                new SQLException("Duplicate entry", "23000", 1062),
                constraintName);
        return new DataIntegrityViolationException("duplicate", constraintViolation);
    }
}
