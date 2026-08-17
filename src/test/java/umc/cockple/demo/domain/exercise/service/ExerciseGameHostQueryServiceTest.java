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
import umc.cockple.demo.domain.exercise.service.query.ExerciseGameHostQueryService;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseGameHostResult;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.MemberExerciseReader;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.domain.ProfileImg;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseGameHostQueryService")
class ExerciseGameHostQueryServiceTest {

    private ExerciseGameHostQueryService exerciseGameHostQueryService;

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private FileService fileService;

    private Member manager;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        MemberPartyLookupService memberPartyLookupService =
                new MemberPartyLookupService(memberPartyRepository);
        exerciseGameHostQueryService = new ExerciseGameHostQueryService(
                new ExerciseReader(exerciseRepository),
                memberPartyLookupService,
                new MemberExerciseReader(memberExerciseRepository),
                new ExerciseValidator(memberPartyLookupService, memberExerciseRepository),
                new ImageUrlResolver(fileService)
        );

        manager = member(1L, "모임장 실명", "모임장 닉네임", Gender.MALE, Level.A);
        party = PartyFixture.createParty(
                "테스트 모임", manager.getId(), PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        exercise = ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31));
        ReflectionTestUtils.setField(exercise, "id", 100L);
    }

    @Nested
    @DisplayName("getGameHost")
    class GetGameHost {

        @Test
        @DisplayName("활성 모임원을 역할과 가입일 순으로 반환하고 현재 진행자를 한 명만 표시한다")
        void returnsSortedActivePartyMembersAndCurrentGameHost() {
            Member subManager = member(2L, "부모임장 실명", "부모임장 닉네임", Gender.FEMALE, Level.B);
            Member earlierMember = member(3L, "이른 멤버 실명", "이른 멤버 닉네임", Gender.FEMALE, Level.C);
            Member laterMember = member(4L, "늦은 멤버 실명", "늦은 멤버 닉네임", Gender.MALE, Level.D);
            manager.updateProfileImg(ProfileImg.builder().imgKey("profiles/manager.jpg").build());

            MemberParty managerParty = memberParty(manager, Role.PARTY_MANAGER,
                    LocalDateTime.of(2026, 1, 4, 10, 0));
            MemberParty subManagerParty = memberParty(subManager, Role.PARTY_SUBMANAGER,
                    LocalDateTime.of(2026, 1, 3, 10, 0));
            MemberParty earlierMemberParty = memberParty(earlierMember, Role.PARTY_MEMBER,
                    LocalDateTime.of(2026, 1, 1, 10, 0));
            MemberParty laterMemberParty = memberParty(laterMember, Role.PARTY_MEMBER,
                    LocalDateTime.of(2026, 1, 2, 10, 0));
            ReflectionTestUtils.setField(exercise, "gameHostId", earlierMember.getId());

            given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
            given(memberPartyRepository.findAllByPartyIdAndStatusWithMemberAndProfile(
                    party.getId(), MemberPartyStatus.ACTIVE))
                    .willReturn(List.of(
                            laterMemberParty, earlierMemberParty, subManagerParty, managerParty));
            given(memberExerciseRepository.findLastExerciseDateByMemberIdsAndPartyId(
                    argThat(ids -> ids.containsAll(List.of(1L, 2L, 3L, 4L))),
                    org.mockito.ArgumentMatchers.eq(party.getId())))
                    .willReturn(List.<Object[]>of(
                            new Object[]{manager.getId(), LocalDate.of(2026, 1, 12)},
                            new Object[]{subManager.getId(), LocalDate.of(2026, 1, 10)}));
            given(fileService.getUrlFromKey("profiles/manager.jpg"))
                    .willReturn("https://example.com/profiles/manager.jpg");

            ExerciseGameHostResult result = exerciseGameHostQueryService
                    .getGameHost(exercise.getId(), manager.getId());

            assertThat(result.totalCount()).isEqualTo(4);
            assertThat(result.participants())
                    .extracting(
                            ExerciseGameHostResult.Participant::participantId,
                            ExerciseGameHostResult.Participant::partyPosition,
                            ExerciseGameHostResult.Participant::gameHost,
                            ExerciseGameHostResult.Participant::name,
                            ExerciseGameHostResult.Participant::lastExerciseDate)
                    .containsExactly(
                            tuple(1L, Role.PARTY_MANAGER, false, "모임장 실명", LocalDate.of(2026, 1, 12)),
                            tuple(2L, Role.PARTY_SUBMANAGER, false, "부모임장 실명", LocalDate.of(2026, 1, 10)),
                            tuple(3L, Role.PARTY_MEMBER, true, "이른 멤버 실명", null),
                            tuple(4L, Role.PARTY_MEMBER, false, "늦은 멤버 실명", null));
            assertThat(result.participants())
                    .filteredOn(ExerciseGameHostResult.Participant::gameHost)
                    .hasSize(1);
            assertThat(result.participants().get(0).profileImageUrl())
                    .isEqualTo("https://example.com/profiles/manager.jpg");
        }

        @Test
        @DisplayName("부모임장도 조회할 수 있다")
        void subManagerCanRead() {
            Member subManager = member(2L, "부모임장", "부모임장", Gender.FEMALE, Level.B);

            given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));
            given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                    party.getId(), subManager.getId(), Role.PARTY_MANAGER)).willReturn(false);
            given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                    party.getId(), subManager.getId(), Role.PARTY_SUBMANAGER)).willReturn(true);
            given(memberPartyRepository.findAllByPartyIdAndStatusWithMemberAndProfile(
                    party.getId(), MemberPartyStatus.ACTIVE)).willReturn(List.of());

            ExerciseGameHostResult result = exerciseGameHostQueryService
                    .getGameHost(exercise.getId(), subManager.getId());

            assertThat(result.totalCount()).isZero();
            assertThat(result.participants()).isEmpty();
            verify(memberExerciseRepository, never())
                    .findLastExerciseDateByMemberIdsAndPartyId(
                            org.mockito.ArgumentMatchers.anyList(),
                            org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("일반 멤버는 조회할 수 없다")
        void normalMemberCannotRead() {
            Member normalMember = member(3L, "일반 멤버", "일반 멤버", Gender.MALE, Level.C);
            given(exerciseRepository.findById(exercise.getId())).willReturn(Optional.of(exercise));

            assertThatThrownBy(() -> exerciseGameHostQueryService
                    .getGameHost(exercise.getId(), normalMember.getId()))
                    .isInstanceOf(ExerciseException.class)
                    .extracting(exception -> ((ExerciseException) exception).getCode())
                    .isEqualTo(ExerciseErrorCode.INSUFFICIENT_PERMISSION);

            verify(memberPartyRepository, never())
                    .findAllByPartyIdAndStatusWithMemberAndProfile(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("존재하지 않는 운동은 조회할 수 없다")
        void exerciseNotFound() {
            given(exerciseRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> exerciseGameHostQueryService.getGameHost(999L, manager.getId()))
                    .isInstanceOf(ExerciseException.class)
                    .extracting(exception -> ((ExerciseException) exception).getCode())
                    .isEqualTo(ExerciseErrorCode.EXERCISE_NOT_FOUND);
        }
    }

    private Member member(Long id, String memberName, String nickname, Gender gender, Level level) {
        Member member = MemberFixture.createMemberWithName(
                memberName, nickname, gender, level, 1000L + id);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private MemberParty memberParty(Member member, Role role, LocalDateTime joinedAt) {
        return MemberParty.builder()
                .party(party)
                .member(member)
                .role(role)
                .joinedAt(joinedAt)
                .status(MemberPartyStatus.ACTIVE)
                .build();
    }
}
