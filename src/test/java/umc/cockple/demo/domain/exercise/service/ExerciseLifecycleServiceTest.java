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
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDeleteDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseUpdateDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.command.internal.ExerciseLifecycleService;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
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
@DisplayName("ExerciseLifecycleService")
class ExerciseLifecycleServiceTest {

    // 인프라 의존성만 Mock
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private ImageService imageService;

    private ExerciseLifecycleService exerciseLifecycleService;

    private Member manager;
    private Party party;

    @BeforeEach
    void setUp() {
        ExerciseValidator exerciseValidator = new ExerciseValidator(memberPartyRepository, memberExerciseRepository);
        ExerciseConverter exerciseConverter = new ExerciseConverter(imageService);
        exerciseLifecycleService = new ExerciseLifecycleService(
                exerciseRepository, partyRepository, exerciseValidator, exerciseConverter);

        manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);
    }

    @Nested
    @DisplayName("createExercise")
    class CreateExercise {

        private ExerciseCreateDTO.Request validRequest;

        @BeforeEach
        void setUp() {
            validRequest = ExerciseCreateDTO.Request.builder()
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
            @DisplayName("모임장이 정상 요청하면 운동이 저장되고 Response를 반환한다")
            void ownerCreatesExercise_success() {
                // given: 모임장(ownerId 일치)이므로 권한 통과, 매니저 role stub은 불필요
                Exercise savedExercise = Exercise.builder()
                        .date(validRequest.toParsedDate())
                        .startTime(validRequest.toParsedStartTime())
                        .endTime(validRequest.toParsedEndTime())
                        .maxCapacity(10)
                        .partyGuestAccept(true)
                        .outsideGuestAccept(false)
                        .build();
                ReflectionTestUtils.setField(savedExercise, "id", 100L);

                given(exerciseRepository.save(any(Exercise.class))).willReturn(savedExercise);

                // when
                ExerciseCreateDTO.Response response = exerciseLifecycleService.createExercise(party, manager, validRequest);

                // then
                assertThat(response.exerciseId()).isEqualTo(100L);
                then(exerciseRepository).should().save(any(Exercise.class));
            }

            @Test
            @DisplayName("부모임장도 운동을 생성할 수 있다")
            void subManagerCreatesExercise_success() {
                // given
                Member subManager = MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1002L);
                ReflectionTestUtils.setField(subManager, "id", 2L);

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.party_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.party_SUBMANAGER))
                        .willReturn(true);

                Exercise savedExercise = Exercise.builder()
                        .date(validRequest.toParsedDate())
                        .startTime(validRequest.toParsedStartTime())
                        .endTime(validRequest.toParsedEndTime())
                        .maxCapacity(10)
                        .partyGuestAccept(true)
                        .outsideGuestAccept(false)
                        .build();
                ReflectionTestUtils.setField(savedExercise, "id", 200L);

                given(exerciseRepository.save(any(Exercise.class))).willReturn(savedExercise);

                // when
                ExerciseCreateDTO.Response response = exerciseLifecycleService.createExercise(party, subManager, validRequest);

                // then
                assertThat(response.exerciseId()).isEqualTo(200L);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("비활성화된 파티면 PartyException(PARTY_IS_DELETED)을 던진다")
            void inactiveParty_throwsException() {
                party.delete(); // 실제 도메인 메서드 호출

                assertThatThrownBy(() ->
                        exerciseLifecycleService.createExercise(party, manager, validRequest))
                        .isInstanceOf(PartyException.class)
                        .satisfies(e -> assertThat(((PartyException) e).getCode())
                                .isEqualTo(PartyErrorCode.PARTY_IS_DELETED));
            }

            @Test
            @DisplayName("일반 멤버가 생성 시 ExerciseException(INSUFFICIENT_PERMISSION)을 던진다")
            void normalMember_throwsException() {
                Member normalMember = MemberFixture.createMember("일반멤버", Gender.FEMALE, Level.B, 1002L);
                ReflectionTestUtils.setField(normalMember, "id", 2L);

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), normalMember.getId(), Role.party_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), normalMember.getId(), Role.party_SUBMANAGER))
                        .willReturn(false);

                assertThatThrownBy(() ->
                        exerciseLifecycleService.createExercise(party, normalMember, validRequest))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.INSUFFICIENT_PERMISSION));
            }

            @Test
            @DisplayName("시작 시간이 종료 시간 이후면 ExerciseException(INVALID_EXERCISE_TIME)을 던진다")
            void invalidExerciseTime_throwsException() {
                // given: 모임장 권한 통과 후 시간 검증에서 실패
                ExerciseCreateDTO.Request invalidTimeRequest = ExerciseCreateDTO.Request.builder()
                        .date("2099-12-31")
                        .buildingName("체육관")
                        .roadAddress("서울특별시 강남구 테헤란로 1")
                        .latitude(37.5).longitude(127.0)
                        .startTime("12:00").endTime("10:00") // 종료가 시작보다 빠름
                        .maxCapacity(10)
                        .allowMemberGuestsInvitation(true)
                        .allowExternalGuests(false)
                        .build();

                assertThatThrownBy(() ->
                        exerciseLifecycleService.createExercise(party, manager, invalidTimeRequest))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.INVALID_EXERCISE_TIME));
            }

            @Test
            @DisplayName("과거 시간이면 ExerciseException(PAST_TIME_NOT_ALLOWED)을 던진다")
            void pastTime_throwsException() {
                // given: 모임장 권한 통과 후 시간 검증에서 실패
                ExerciseCreateDTO.Request pastRequest = ExerciseCreateDTO.Request.builder()
                        .date("2000-01-01")
                        .buildingName("체육관")
                        .roadAddress("서울특별시 강남구 테헤란로 1")
                        .latitude(37.5).longitude(127.0)
                        .startTime("10:00").endTime("12:00")
                        .maxCapacity(10)
                        .allowMemberGuestsInvitation(true)
                        .allowExternalGuests(false)
                        .build();

                assertThatThrownBy(() ->
                        exerciseLifecycleService.createExercise(party, manager, pastRequest))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.PAST_TIME_NOT_ALLOWED));
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
            exercise.setParty(party);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("모임장이 운동을 삭제하면 deletedExerciseId를 반환한다")
            void ownerDeletesExercise_success() {
                // given: party.ownerId == manager.id 이므로 권한 통과

                // when
                ExerciseDeleteDTO.Response response = exerciseLifecycleService.deleteExercise(exercise, manager);

                // then
                assertThat(response.deletedExerciseId()).isEqualTo(100L);
                then(exerciseRepository).should().delete(exercise);
                then(partyRepository).should().save(party);
            }

            @Test
            @DisplayName("부모임장도 운동을 삭제할 수 있다")
            void subManagerDeletesExercise_success() {
                // given
                Member subManager = MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1002L);
                ReflectionTestUtils.setField(subManager, "id", 2L);

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.party_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.party_SUBMANAGER))
                        .willReturn(true);

                // when
                ExerciseDeleteDTO.Response response = exerciseLifecycleService.deleteExercise(exercise, subManager);

                // then
                assertThat(response.deletedExerciseId()).isEqualTo(100L);
                then(exerciseRepository).should().delete(exercise);
                then(partyRepository).should().save(party);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("일반 멤버가 삭제 시 ExerciseException(INSUFFICIENT_PERMISSION)을 던진다")
            void normalMember_throwsException() {
                Member normalMember = MemberFixture.createMember("일반멤버", Gender.FEMALE, Level.B, 1002L);
                ReflectionTestUtils.setField(normalMember, "id", 2L);

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), normalMember.getId(), Role.party_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), normalMember.getId(), Role.party_SUBMANAGER))
                        .willReturn(false);

                assertThatThrownBy(() ->
                        exerciseLifecycleService.deleteExercise(exercise, normalMember))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.INSUFFICIENT_PERMISSION));
            }
        }
    }

    @Nested
    @DisplayName("updateExercise")
    class UpdateExercise {

        private Exercise exercise;
        private ExerciseUpdateDTO.Request validRequest;

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
            exercise.setParty(party);

            validRequest = new ExerciseUpdateDTO.Request(
                    "2099-12-31",
                    "수정된 체육관",
                    "서울특별시 강남구 테헤란로 2",
                    37.6,
                    127.1,
                    "11:00",
                    "13:00",
                    12,
                    "공지사항"
            );
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("모임장이 운동을 수정하면 Response를 반환한다")
            void ownerUpdatesExercise_success() {
                // given: party.ownerId == manager.id 이므로 권한 통과
                Exercise savedExercise = Exercise.builder()
                        .date(LocalDate.of(2099, 12, 31))
                        .startTime(LocalTime.of(11, 0))
                        .endTime(LocalTime.of(13, 0))
                        .maxCapacity(12)
                        .partyGuestAccept(true)
                        .outsideGuestAccept(false)
                        .build();
                ReflectionTestUtils.setField(savedExercise, "id", 100L);
                given(exerciseRepository.save(any(Exercise.class))).willReturn(savedExercise);

                // when
                ExerciseUpdateDTO.Response response = exerciseLifecycleService.updateExercise(exercise, manager, validRequest);

                // then
                assertThat(response.exerciseId()).isEqualTo(100L);
                then(exerciseRepository).should().save(exercise);
            }

            @Test
            @DisplayName("부모임장도 운동을 수정할 수 있다")
            void subManagerUpdatesExercise_success() {
                // given
                Member subManager = MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1002L);
                ReflectionTestUtils.setField(subManager, "id", 2L);

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.party_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), subManager.getId(), Role.party_SUBMANAGER))
                        .willReturn(true);

                Exercise savedExercise = Exercise.builder()
                        .date(LocalDate.of(2099, 12, 31))
                        .startTime(LocalTime.of(11, 0))
                        .endTime(LocalTime.of(13, 0))
                        .maxCapacity(12)
                        .partyGuestAccept(true)
                        .outsideGuestAccept(false)
                        .build();
                ReflectionTestUtils.setField(savedExercise, "id", 100L);
                given(exerciseRepository.save(any(Exercise.class))).willReturn(savedExercise);

                // when
                ExerciseUpdateDTO.Response response = exerciseLifecycleService.updateExercise(exercise, subManager, validRequest);

                // then
                assertThat(response.exerciseId()).isEqualTo(100L);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("일반 멤버가 수정 시 ExerciseException(INSUFFICIENT_PERMISSION)을 던진다")
            void normalMember_throwsException() {
                Member normalMember = MemberFixture.createMember("일반멤버", Gender.FEMALE, Level.B, 1002L);
                ReflectionTestUtils.setField(normalMember, "id", 2L);

                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), normalMember.getId(), Role.party_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(party.getId(), normalMember.getId(), Role.party_SUBMANAGER))
                        .willReturn(false);

                assertThatThrownBy(() ->
                        exerciseLifecycleService.updateExercise(exercise, normalMember, validRequest))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.INSUFFICIENT_PERMISSION));
            }

            @Test
            @DisplayName("이미 시작된 운동이면 ExerciseException(EXERCISE_ALREADY_STARTED_UPDATE)을 던진다")
            void alreadyStarted_throwsException() {
                // given: 과거 날짜로 설정된 운동 (이미 시작됨)
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

                assertThatThrownBy(() ->
                        exerciseLifecycleService.updateExercise(startedExercise, manager, validRequest))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_UPDATE));
            }

            @Test
            @DisplayName("시작 시간이 종료 시간 이후면 ExerciseException(INVALID_EXERCISE_TIME)을 던진다")
            void invalidTime_throwsException() {
                ExerciseUpdateDTO.Request invalidTimeRequest = new ExerciseUpdateDTO.Request(
                        "2099-12-31", null, null, null, null,
                        "13:00", "11:00", null, null
                );

                assertThatThrownBy(() ->
                        exerciseLifecycleService.updateExercise(exercise, manager, invalidTimeRequest))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.INVALID_EXERCISE_TIME));
            }

            @Test
            @DisplayName("과거 날짜로 수정 시 ExerciseException(PAST_TIME_NOT_ALLOWED)을 던진다")
            void pastDate_throwsException() {
                ExerciseUpdateDTO.Request pastDateRequest = new ExerciseUpdateDTO.Request(
                        "2000-01-01", null, null, null, null,
                        "10:00", "12:00", null, null
                );

                assertThatThrownBy(() ->
                        exerciseLifecycleService.updateExercise(exercise, manager, pastDateRequest))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.PAST_TIME_NOT_ALLOWED));
            }
        }
    }
}