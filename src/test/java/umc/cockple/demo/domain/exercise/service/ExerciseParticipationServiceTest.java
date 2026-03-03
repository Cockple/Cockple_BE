package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.command.internal.ExerciseParticipationService;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.fixture.GuestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseParticipationService")
class ExerciseParticipationServiceTest {

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private ImageService imageService;

    private ExerciseParticipationService exerciseParticipationService;

    private Member manager;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        ExerciseValidator exerciseValidator = new ExerciseValidator(memberPartyRepository, memberExerciseRepository);
        ExerciseConverter exerciseConverter = new ExerciseConverter(imageService);
        exerciseParticipationService = new ExerciseParticipationService(
                exerciseRepository, memberRepository, memberPartyRepository,
                memberExerciseRepository, guestRepository, exerciseValidator, exerciseConverter);

        manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        exercise = Exercise.builder()
                .date(LocalDate.of(2099, 12, 31))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .maxCapacity(10)
                .partyGuestAccept(true)
                .outsideGuestAccept(false)
                .build();
        ReflectionTestUtils.setField(exercise, "id", 100L);
        exercise.setParty(party);
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

                given(memberRepository.findById(participant.getId())).willReturn(Optional.of(participant));
                given(memberExerciseRepository.findByExerciseAndMember(exercise, participant))
                        .willReturn(Optional.of(memberExercise));

                // when
                ExerciseCancelDTO.Response response = exerciseParticipationService
                        .cancelParticipationByManager(exercise, participant.getId(), manager, request);

                // then
                assertThat(response.memberName()).isEqualTo(participant.getMemberName());
                then(memberExerciseRepository).should().delete(memberExercise);
                then(exerciseRepository).should().save(exercise);
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

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.party_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.party_SUBMANAGER))
                        .willReturn(true);
                given(memberRepository.findById(participant.getId())).willReturn(Optional.of(participant));
                given(memberExerciseRepository.findByExerciseAndMember(exercise, participant))
                        .willReturn(Optional.of(memberExercise));

                // when
                ExerciseCancelDTO.Response response = exerciseParticipationService
                        .cancelParticipationByManager(exercise, participant.getId(), subManager, request);

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

                given(guestRepository.findById(guest.getId())).willReturn(Optional.of(guest));

                // when
                ExerciseCancelDTO.Response response = exerciseParticipationService
                        .cancelParticipationByManager(exercise, guest.getId(), manager, request);

                // then
                assertThat(response.memberName()).isEqualTo("게스트");
                then(guestRepository).should().delete(guest);
                then(exerciseRepository).should().save(exercise);
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

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), normalMember.getId(), Role.party_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), normalMember.getId(), Role.party_SUBMANAGER))
                        .willReturn(false);

                assertThatThrownBy(() ->
                        exerciseParticipationService.cancelParticipationByManager(exercise, 2L, normalMember, request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.INSUFFICIENT_PERMISSION));
            }

            @Test
            @DisplayName("이미 시작된 운동이면 ExerciseException(EXERCISE_ALREADY_STARTED_CANCEL)을 던진다")
            void alreadyStarted_throwsException() {
                Exercise startedExercise = Exercise.builder()
                        .date(LocalDate.of(2000, 1, 1))
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(12, 0))
                        .maxCapacity(10)
                        .partyGuestAccept(true)
                        .outsideGuestAccept(false)
                        .build();
                ReflectionTestUtils.setField(startedExercise, "id", 200L);
                startedExercise.setParty(party);

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                assertThatThrownBy(() ->
                        exerciseParticipationService.cancelParticipationByManager(startedExercise, 2L, manager, request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL));
            }

            @Test
            @DisplayName("존재하지 않는 멤버 참여자면 ExerciseException(MEMBER_NOT_FOUND)을 던진다")
            void memberParticipantNotFound_throwsException() {
                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                given(memberRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        exerciseParticipationService.cancelParticipationByManager(exercise, 999L, manager, request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.MEMBER_NOT_FOUND));
            }

            @Test
            @DisplayName("존재하지 않는 게스트면 ExerciseException(GUEST_NOT_FOUND)을 던진다")
            void guestNotFound_throwsException() {
                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(true);

                given(guestRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        exerciseParticipationService.cancelParticipationByManager(exercise, 999L, manager, request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.GUEST_NOT_FOUND));
            }
        }
    }
}