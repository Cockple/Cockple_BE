package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDeleteDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseCommandService;
import umc.cockple.demo.domain.exercise.service.command.internal.ExerciseGuestService;
import umc.cockple.demo.domain.exercise.service.command.internal.ExerciseLifecycleService;
import umc.cockple.demo.domain.exercise.service.command.internal.ExerciseParticipationService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
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
@DisplayName("ExerciseCommandService")
class ExerciseCommandServiceTest {

    @InjectMocks
    private ExerciseCommandService exerciseCommandService;

    @Mock private ExerciseLifecycleService exerciseLifecycleService;
    @Mock private ExerciseParticipationService exerciseParticipationService;
    @Mock private ExerciseGuestService exerciseGuestService;
    
    @Mock private PartyRepository partyRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private GuestRepository guestRepository;

    private Member manager;
    private Party party;

    @BeforeEach
    void setUp() {
        manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);
    }

    @Nested
    @DisplayName("createExercise")
    class CreateExercise {

        private ExerciseCreateDTO.Request request;

        @BeforeEach
        void setUp() {
            request = ExerciseCreateDTO.Request.builder()
                    .date("2099-12-31")
                    .buildingName("테스트 체육관")
                    .roadAddress("서울특별시 강남구 테헤란로 1")
                    .latitude(37.5)
                    .longitude(127.0)
                    .startTime("10:00")
                    .endTime("12:00")
                    .maxCapacity(10)
                    .allowMemberGuestsInvitation(true)
                    .allowExternalGuests(false)
                    .build();
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("Party, Member 조회 후 ExerciseLifecycleService에 위임한다")
            void delegatesToLifecycleService() {
                // given
                ExerciseCreateDTO.Response expectedResponse = ExerciseCreateDTO.Response.builder()
                        .exerciseId(100L)
                        .build();

                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(memberRepository.findById(manager.getId())).willReturn(Optional.of(manager));
                given(exerciseLifecycleService.createExercise(party, manager, request)).willReturn(expectedResponse);

                // when
                ExerciseCreateDTO.Response response = exerciseCommandService.createExercise(
                        party.getId(), manager.getId(), request);

                // then
                assertThat(response.exerciseId()).isEqualTo(100L);
                then(exerciseLifecycleService).should().createExercise(party, manager, request);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 파티면 ExerciseException(PARTY_NOT_FOUND)을 던진다")
            void partyNotFound_throwsException() {
                given(partyRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        exerciseCommandService.createExercise(999L, manager.getId(), request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.PARTY_NOT_FOUND));
            }

            @Test
            @DisplayName("존재하지 않는 멤버면 ExerciseException(MEMBER_NOT_FOUND)을 던진다")
            void memberNotFound_throwsException() {
                given(partyRepository.findById(party.getId())).willReturn(Optional.of(party));
                given(memberRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        exerciseCommandService.createExercise(party.getId(), 999L, request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.MEMBER_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("deleteExercise")
    class DeleteExercise {

        private Exercise exercise;

        @BeforeEach
        void setUp() {
            exercise = Exercise.builder()
                    .date(LocalDate.of(2099, 12, 31))
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(12, 0))
                    .maxCapacity(10)
                    .partyGuestAccept(true)
                    .outsideGuestAccept(false)
                    .build();
            ReflectionTestUtils.setField(exercise, "id", 100L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("Exercise, Member 조회 후 ExerciseLifecycleService에 위임한다")
            void delegatesToLifecycleService() {
                // given
                ExerciseDeleteDTO.Response expectedResponse = ExerciseDeleteDTO.Response.builder()
                        .deletedExerciseId(100L)
                        .build();

                given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId())).willReturn(Optional.of(manager));
                given(exerciseLifecycleService.deleteExercise(exercise, manager)).willReturn(expectedResponse);

                // when
                ExerciseDeleteDTO.Response response = exerciseCommandService.deleteExercise(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.deletedExerciseId()).isEqualTo(100L);
                then(exerciseLifecycleService).should().deleteExercise(exercise, manager);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 운동이면 ExerciseException(EXERCISE_NOT_FOUND)을 던진다")
            void exerciseNotFound_throwsException() {
                given(exerciseRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        exerciseCommandService.deleteExercise(999L, manager.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_NOT_FOUND));
            }

            @Test
            @DisplayName("존재하지 않는 멤버면 ExerciseException(MEMBER_NOT_FOUND)을 던진다")
            void memberNotFound_throwsException() {
                given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
                given(memberRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        exerciseCommandService.deleteExercise(exercise.getId(), 999L))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.MEMBER_NOT_FOUND));
            }
        }
    }
}