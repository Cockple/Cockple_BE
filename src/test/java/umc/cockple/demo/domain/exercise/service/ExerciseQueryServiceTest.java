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
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.GuestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseQueryService")
class ExerciseQueryServiceTest {

    @InjectMocks
    private ExerciseQueryService exerciseQueryService;

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private ExerciseBookmarkRepository exerciseBookmarkRepository;
    @Mock private FileService fileService;

    private ExerciseConverter exerciseConverter;

    private Member manager;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        exerciseConverter = new ExerciseConverter(fileService);
        ReflectionTestUtils.setField(exerciseQueryService, "exerciseConverter", exerciseConverter);

        manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        exercise = ExerciseFixture.createExercise(party, LocalDate.now().minusDays(1));
        ReflectionTestUtils.setField(exercise, "id", 100L);

        ExerciseAddr exerciseAddr = ExerciseAddr.builder()
                .addr1("서울특별시")
                .addr2("강남구")
                .streetAddr("서울특별시 강남구 테헤란로 1")
                .buildingName("테스트 체육관")
                .latitude(37.5)
                .longitude(127.0)
                .build();
        ReflectionTestUtils.setField(exercise, "exerciseAddr", exerciseAddr);
    }

    @Nested
    @DisplayName("getExerciseDetail")
    class GetExerciseDetail {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("모임장이면_isManager_true로_반환된다")
            void 모임장이면_isManager_true로_반환된다() {
                // given
                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of());
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.party_MANAGER))
                        .willReturn(true);

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.isManager()).isTrue();
            }

            @Test
            @DisplayName("일반_멤버면_isManager_false로_반환된다")
            void 일반_멤버면_isManager_false로_반환된다() {
                // given
                Member normalMember = MemberFixture.createMember("일반멤버", Gender.FEMALE, Level.B, 2002L);
                ReflectionTestUtils.setField(normalMember, "id", 2L);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(normalMember.getId()))
                        .willReturn(Optional.of(normalMember));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of());
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), normalMember.getId(), Role.party_MANAGER))
                        .willReturn(false);

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), normalMember.getId());

                // then
                assertThat(response.isManager()).isFalse();
            }

            @Test
            @DisplayName("탈퇴_회원은_isWithdrawn_true로_반환된다")
            void 탈퇴_회원은_isWithdrawn_true로_반환된다() {
                // given
                Member withdrawnMember = Member.builder()
                        .memberName("탈퇴회원")
                        .nickname("탈퇴닉네임")
                        .gender(Gender.MALE)
                        .level(Level.C)
                        .isActive(MemberStatus.INACTIVE)
                        .socialId(9999L)
                        .build();
                ReflectionTestUtils.setField(withdrawnMember, "id", 99L);

                MemberExercise memberExercise = MemberFixture.createMemberExercise(withdrawnMember, exercise);
                ReflectionTestUtils.setField(memberExercise, "createdAt", LocalDateTime.now());

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(memberExercise));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.party_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(withdrawnMember.getId())))
                        .willReturn(List.of());

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                List<ExerciseDetailDTO.ParticipantInfo> participants = response.participants().list();
                assertThat(participants).hasSize(1);
                assertThat(participants.get(0).isWithdrawn()).isTrue();
            }

            @Test
            @DisplayName("활성_회원은_isWithdrawn_false로_반환된다")
            void 활성_회원은_isWithdrawn_false로_반환된다() {
                // given
                Member activeMember = MemberFixture.createMember("활성회원", Gender.FEMALE, Level.B, 2002L);
                ReflectionTestUtils.setField(activeMember, "id", 2L);

                MemberExercise memberExercise = MemberFixture.createMemberExercise(activeMember, exercise);
                ReflectionTestUtils.setField(memberExercise, "createdAt", LocalDateTime.now());

                MemberParty memberParty = MemberFixture.createMemberParty(party, activeMember, Role.party_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(memberExercise));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.party_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(activeMember.getId())))
                        .willReturn(List.of(memberParty));

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                List<ExerciseDetailDTO.ParticipantInfo> participants = response.participants().list();
                assertThat(participants).hasSize(1);
                assertThat(participants.get(0).isWithdrawn()).isFalse();
            }

            @Test
            @DisplayName("게스트는_isWithdrawn_false로_반환된다")
            void 게스트는_isWithdrawn_false로_반환된다() {
                // given
                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of());
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.party_MANAGER))
                        .willReturn(true);

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                List<ExerciseDetailDTO.ParticipantInfo> participants = response.participants().list();
                assertThat(participants).isEmpty();
            }

            @Test
            @DisplayName("정원_초과_참가자는_대기자_목록으로_반환된다")
            void 정원_초과_참가자는_대기자_목록으로_반환된다() {
                // given
                ReflectionTestUtils.setField(exercise, "maxCapacity", 1);

                Member firstMember = MemberFixture.createMember("첫번째", Gender.MALE, Level.A, 3001L);
                ReflectionTestUtils.setField(firstMember, "id", 3L);

                Member secondMember = MemberFixture.createMember("두번째", Gender.FEMALE, Level.B, 3002L);
                ReflectionTestUtils.setField(secondMember, "id", 4L);

                MemberExercise first = MemberFixture.createMemberExercise(firstMember, exercise);
                ReflectionTestUtils.setField(first, "createdAt", LocalDateTime.now().minusMinutes(10));

                MemberExercise second = MemberFixture.createMemberExercise(secondMember, exercise);
                ReflectionTestUtils.setField(second, "createdAt", LocalDateTime.now());

                MemberParty firstParty = MemberFixture.createMemberParty(party, firstMember, Role.party_MEMBER);
                MemberParty secondParty = MemberFixture.createMemberParty(party, secondMember, Role.party_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(first, second));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.party_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(firstMember.getId(), secondMember.getId())))
                        .willReturn(List.of(firstParty, secondParty));

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.participants().list()).hasSize(1);
                assertThat(response.waiting().list()).hasSize(1);
                assertThat(response.waiting().currentWaitingCount()).isEqualTo(1);
            }

            @Test
            @DisplayName("게스트_참가자는_participantType이_GUEST이고_inviterName이_반환된다")
            void 게스트_참가자는_participantType이_GUEST이고_inviterName이_반환된다() {
                // given
                Guest guest = GuestFixture.createGuest(exercise, manager.getId());
                ReflectionTestUtils.setField(guest, "id", 50L);
                ReflectionTestUtils.setField(guest, "createdAt", LocalDateTime.now());

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of());
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of(guest));
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.party_MANAGER))
                        .willReturn(true);
                given(memberRepository.findMemberNamesByIds(any()))
                        .willReturn(Map.of(manager.getId(), "모임장"));

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                List<ExerciseDetailDTO.ParticipantInfo> participants = response.participants().list();
                assertThat(participants).hasSize(1);
                assertThat(participants.get(0).participantType()).isEqualTo("GUEST");
                assertThat(participants.get(0).inviterName()).isEqualTo("모임장");
            }

            @Test
            @DisplayName("먼저_가입한_참가자가_더_낮은_participantNumber를_받는다")
            void 먼저_가입한_참가자가_더_낮은_participantNumber를_받는다() {
                // given
                Member firstMember = MemberFixture.createMember("첫번째", Gender.MALE, Level.A, 5001L);
                ReflectionTestUtils.setField(firstMember, "id", 7L);

                Member secondMember = MemberFixture.createMember("두번째", Gender.FEMALE, Level.B, 5002L);
                ReflectionTestUtils.setField(secondMember, "id", 8L);

                MemberExercise first = MemberFixture.createMemberExercise(firstMember, exercise);
                ReflectionTestUtils.setField(first, "createdAt", LocalDateTime.now().minusMinutes(10));

                MemberExercise second = MemberFixture.createMemberExercise(secondMember, exercise);
                ReflectionTestUtils.setField(second, "createdAt", LocalDateTime.now());

                MemberParty firstParty = MemberFixture.createMemberParty(party, firstMember, Role.party_MEMBER);
                MemberParty secondParty = MemberFixture.createMemberParty(party, secondMember, Role.party_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(first, second));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.party_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(firstMember.getId(), secondMember.getId())))
                        .willReturn(List.of(firstParty, secondParty));

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                List<ExerciseDetailDTO.ParticipantInfo> participants = response.participants().list();
                assertThat(participants).hasSize(2);
                assertThat(participants.get(0).participantNumber()).isEqualTo(1);
                assertThat(participants.get(0).name()).isEqualTo("첫번째");
                assertThat(participants.get(1).participantNumber()).isEqualTo(2);
                assertThat(participants.get(1).name()).isEqualTo("두번째");
            }

            @Test
            @DisplayName("대기자_성별_카운트가_올바르게_계산된다")
            void 대기자_성별_카운트가_올바르게_계산된다() {
                // given
                ReflectionTestUtils.setField(exercise, "maxCapacity", 1);

                Member maleMember = MemberFixture.createMember("남성", Gender.MALE, Level.A, 6001L);
                ReflectionTestUtils.setField(maleMember, "id", 11L);

                Member femaleMember = MemberFixture.createMember("여성", Gender.FEMALE, Level.B, 6002L);
                ReflectionTestUtils.setField(femaleMember, "id", 12L);

                MemberExercise first = MemberFixture.createMemberExercise(maleMember, exercise);
                ReflectionTestUtils.setField(first, "createdAt", LocalDateTime.now().minusMinutes(10));

                MemberExercise second = MemberFixture.createMemberExercise(femaleMember, exercise);
                ReflectionTestUtils.setField(second, "createdAt", LocalDateTime.now());

                MemberParty maleParty = MemberFixture.createMemberParty(party, maleMember, Role.party_MEMBER);
                MemberParty femaleParty = MemberFixture.createMemberParty(party, femaleMember, Role.party_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(first, second));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.party_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(maleMember.getId(), femaleMember.getId())))
                        .willReturn(List.of(maleParty, femaleParty));

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.participants().manCount()).isEqualTo(1);
                assertThat(response.participants().womenCount()).isZero();
                assertThat(response.waiting().manCount()).isZero();
                assertThat(response.waiting().womenCount()).isEqualTo(1);
            }

            @Test
            @DisplayName("참가자_성별_카운트가_올바르게_계산된다")
            void 참가자_성별_카운트가_올바르게_계산된다() {
                // given
                Member maleMember = MemberFixture.createMember("남성", Gender.MALE, Level.A, 4001L);
                ReflectionTestUtils.setField(maleMember, "id", 5L);

                Member femaleMember = MemberFixture.createMember("여성", Gender.FEMALE, Level.B, 4002L);
                ReflectionTestUtils.setField(femaleMember, "id", 6L);

                MemberExercise maleExercise = MemberFixture.createMemberExercise(maleMember, exercise);
                ReflectionTestUtils.setField(maleExercise, "createdAt", LocalDateTime.now().minusMinutes(5));

                MemberExercise femaleExercise = MemberFixture.createMemberExercise(femaleMember, exercise);
                ReflectionTestUtils.setField(femaleExercise, "createdAt", LocalDateTime.now());

                MemberParty maleParty = MemberFixture.createMemberParty(party, maleMember, Role.party_MEMBER);
                MemberParty femaleParty = MemberFixture.createMemberParty(party, femaleMember, Role.party_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(maleExercise, femaleExercise));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.party_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(maleMember.getId(), femaleMember.getId())))
                        .willReturn(List.of(maleParty, femaleParty));

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.participants().manCount()).isEqualTo(1);
                assertThat(response.participants().womenCount()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_운동이면_예외를_던진다")
            void 존재하지_않는_운동이면_예외를_던진다() {
                // given
                given(exerciseRepository.findExerciseWithBasicInfo(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getExerciseDetail(999L, manager.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.EXERCISE_NOT_FOUND);
            }

            @Test
            @DisplayName("존재하지_않는_멤버면_예외를_던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getExerciseDetail(exercise.getId(), 999L))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }
}
