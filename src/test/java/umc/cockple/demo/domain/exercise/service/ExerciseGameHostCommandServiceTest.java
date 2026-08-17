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
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGameHostCommandService;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGameHostChangeCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseGameHostChangeResult;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.service.query.lookup.MemberPartyLookupService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseGameHostCommandService")
class ExerciseGameHostCommandServiceTest {

    private ExerciseGameHostCommandService exerciseGameHostCommandService;

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;

    private Member manager;
    private Member subManager;
    private Member normalMember;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        MemberPartyLookupService memberPartyLookupService =
                new MemberPartyLookupService(memberPartyRepository);
        exerciseGameHostCommandService = new ExerciseGameHostCommandService(
                new ExerciseReader(exerciseRepository),
                new ExerciseValidator(memberPartyLookupService, memberExerciseRepository),
                memberPartyLookupService
        );

        manager = member(1L, "모임장", Gender.MALE, Level.A);
        subManager = member(2L, "부모임장", Gender.FEMALE, Level.B);
        normalMember = member(3L, "일반 멤버", Gender.MALE, Level.C);
        party = PartyFixture.createParty(
                "테스트 모임", manager.getId(), PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);
        exercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31));
        ReflectionTestUtils.setField(exercise, "id", 100L);
    }

    @Nested
    @DisplayName("changeGameHost")
    class ChangeGameHost {

        @Test
        @DisplayName("모임장은 활성 일반 멤버를 게임 진행자로 변경할 수 있다")
        void managerChangesGameHostToActiveMember() {
            given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
            givenManagementPermission(manager);
            given(memberPartyRepository.findByPartyIdAndMemberIdAndStatusForUpdate(
                    party.getId(), normalMember.getId(), MemberPartyStatus.ACTIVE))
                    .willReturn(Optional.of(activeMembership(normalMember)));

            ExerciseGameHostChangeResult result = exerciseGameHostCommandService.changeGameHost(
                    exercise.getId(),
                    manager.getId(),
                    new ExerciseGameHostChangeCommand(normalMember.getId()));

            assertThat(exercise.getGameHostId()).isEqualTo(normalMember.getId());
            assertThat(result.exerciseId()).isEqualTo(exercise.getId());
            assertThat(result.participantId()).isEqualTo(normalMember.getId());
        }

        @Test
        @DisplayName("부모임장도 게임 진행자를 변경할 수 있다")
        void subManagerChangesGameHost() {
            given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
            givenManagementPermission(subManager);
            given(memberPartyRepository.findByPartyIdAndMemberIdAndStatusForUpdate(
                    party.getId(), normalMember.getId(), MemberPartyStatus.ACTIVE))
                    .willReturn(Optional.of(activeMembership(normalMember)));

            ExerciseGameHostChangeResult result = exerciseGameHostCommandService.changeGameHost(
                    exercise.getId(),
                    subManager.getId(),
                    new ExerciseGameHostChangeCommand(normalMember.getId()));

            assertThat(result.participantId()).isEqualTo(normalMember.getId());
            assertThat(exercise.getGameHostId()).isEqualTo(normalMember.getId());
        }

        @Test
        @DisplayName("현재 게임 진행자를 다시 지정해도 성공한다")
        void sameGameHostSucceeds() {
            given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
            givenManagementPermission(manager);
            given(memberPartyRepository.findByPartyIdAndMemberIdAndStatusForUpdate(
                    party.getId(), manager.getId(), MemberPartyStatus.ACTIVE))
                    .willReturn(Optional.of(activeMembership(manager)));

            ExerciseGameHostChangeResult result = exerciseGameHostCommandService.changeGameHost(
                    exercise.getId(),
                    manager.getId(),
                    new ExerciseGameHostChangeCommand(manager.getId()));

            assertThat(result.participantId()).isEqualTo(manager.getId());
            assertThat(exercise.getGameHostId()).isEqualTo(manager.getId());
        }

        @Test
        @DisplayName("일반 멤버는 게임 진행자를 변경할 수 없다")
        void normalMemberCannotChangeGameHost() {
            given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));

            assertThatThrownBy(() -> exerciseGameHostCommandService.changeGameHost(
                    exercise.getId(),
                    normalMember.getId(),
                    new ExerciseGameHostChangeCommand(subManager.getId())))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.GAME_HOST_MANAGEMENT_PERMISSION_DENIED));

            verify(memberPartyRepository, never()).findByPartyIdAndMemberIdAndStatusForUpdate(
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("비활성 모임원은 게임 진행자로 지정할 수 없다")
        void inactivePartyMemberCannotBecomeGameHost() {
            given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
            givenManagementPermission(manager);
            given(memberPartyRepository.findByPartyIdAndMemberIdAndStatusForUpdate(
                    party.getId(), normalMember.getId(), MemberPartyStatus.ACTIVE))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> exerciseGameHostCommandService.changeGameHost(
                    exercise.getId(),
                    manager.getId(),
                    new ExerciseGameHostChangeCommand(normalMember.getId())))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.INVALID_GAME_HOST_CANDIDATE));

            assertThat(exercise.getGameHostId()).isEqualTo(manager.getId());
        }

        @Test
        @DisplayName("존재하지 않는 운동은 게임 진행자를 변경할 수 없다")
        void exerciseNotFound() {
            given(exerciseRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> exerciseGameHostCommandService.changeGameHost(
                    999L,
                    manager.getId(),
                    new ExerciseGameHostChangeCommand(normalMember.getId())))
                    .isInstanceOf(ExerciseException.class)
                    .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                            .isEqualTo(ExerciseErrorCode.EXERCISE_NOT_FOUND));
        }
    }

    private Member member(Long id, String name, Gender gender, Level level) {
        Member member = MemberFixture.createMember(name, gender, level, 1000L + id);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private MemberParty activeMembership(Member member) {
        return MemberFixture.createMemberParty(party, member, Role.PARTY_MEMBER);
    }

    private void givenManagementPermission(Member member) {
        given(memberPartyRepository.existsByPartyIdAndMemberIdAndStatusAndRoleIn(
                eq(party.getId()),
                eq(member.getId()),
                eq(MemberPartyStatus.ACTIVE),
                argThat(roles -> roles.contains(Role.PARTY_MANAGER)
                        && roles.contains(Role.PARTY_SUBMANAGER))))
                .willReturn(true);
    }
}
