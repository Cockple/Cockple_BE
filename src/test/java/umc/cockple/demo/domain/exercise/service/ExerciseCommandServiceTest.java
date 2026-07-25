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
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.participation.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.guest.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseCommandService;
import umc.cockple.demo.domain.exercise.service.command.internal.ExerciseGuestService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ExerciseFixture;
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
@DisplayName("ExerciseCommandService")
class ExerciseCommandServiceTest {

    @InjectMocks
    private ExerciseCommandService exerciseCommandService;

    @Mock private ExerciseGuestService exerciseGuestService;
    
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
    @DisplayName("inviteGuest")
    class InviteGuest {

        private Exercise exercise;
        private ExerciseGuestInviteDTO.Request request;

        @BeforeEach
        void setUp() {
            exercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                    LocalTime.of(12, 0), true, false);
            ReflectionTestUtils.setField(exercise, "id", 100L);

            request = new ExerciseGuestInviteDTO.Request("테스트게스트", "남성", "B조");
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("Exercise, Member 조회 후 ExerciseGuestService에 위임한다")
            void delegatesToGuestService() {
                // given
                ExerciseGuestInviteDTO.Response expectedResponse = ExerciseGuestInviteDTO.Response.builder()
                        .guestId(200L)
                        .currentParticipants(1)
                        .build();

                given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId())).willReturn(Optional.of(manager));
                given(exerciseGuestService.inviteGuest(exercise, manager, request)).willReturn(expectedResponse);

                // when
                ExerciseGuestInviteDTO.Response response = exerciseCommandService.inviteGuest(
                        exercise.getId(), manager.getId(), request);

                // then
                assertThat(response.guestId()).isEqualTo(200L);
                assertThat(response.currentParticipants()).isEqualTo(1);
                then(exerciseGuestService).should().inviteGuest(exercise, manager, request);
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
                        exerciseCommandService.inviteGuest(999L, manager.getId(), request))
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
                        exerciseCommandService.inviteGuest(exercise.getId(), 999L, request))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.MEMBER_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("cancelGuestInvitation")
    class CancelGuestInvitation {

        private Exercise exercise;
        private Guest guest;

        @BeforeEach
        void setUp() {
            exercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                    LocalTime.of(12, 0), true, false);
            ReflectionTestUtils.setField(exercise, "id", 100L);

            guest = GuestFixture.createGuest(exercise, manager.getId());
            ReflectionTestUtils.setField(guest, "id", 60L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("Exercise, Member, Guest 조회 후 ExerciseGuestService에 위임한다")
            void delegatesToGuestService() {
                // given
                ExerciseCancelDTO.Response expectedResponse = ExerciseCancelDTO.Response.builder()
                        .memberName("게스트")
                        .currentParticipants(0)
                        .build();

                given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId())).willReturn(Optional.of(manager));
                given(guestRepository.findById(guest.getId())).willReturn(Optional.of(guest));
                given(exerciseGuestService.cancelGuestInvitation(exercise, guest, manager))
                        .willReturn(expectedResponse);

                // when
                ExerciseCancelDTO.Response response = exerciseCommandService.cancelGuestInvitation(
                        exercise.getId(), guest.getId(), manager.getId());

                // then
                assertThat(response.memberName()).isEqualTo("게스트");
                assertThat(response.currentParticipants()).isEqualTo(0);
                then(exerciseGuestService).should().cancelGuestInvitation(exercise, guest, manager);
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
                        exerciseCommandService.cancelGuestInvitation(999L, guest.getId(), manager.getId()))
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
                        exerciseCommandService.cancelGuestInvitation(exercise.getId(), guest.getId(), 999L))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.MEMBER_NOT_FOUND));
            }

            @Test
            @DisplayName("존재하지 않는 게스트면 ExerciseException(GUEST_NOT_FOUND)을 던진다")
            void guestNotFound_throwsException() {
                given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId())).willReturn(Optional.of(manager));
                given(guestRepository.findById(999L)).willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        exerciseCommandService.cancelGuestInvitation(exercise.getId(), 999L, manager.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.GUEST_NOT_FOUND));
            }
        }
    }
}
