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
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseEditDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMyGuestListDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.PartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.enums.PartyStatus;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.exception.PartyException;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.ExerciseCalendarTestHelper;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.GuestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
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

        ReflectionTestUtils.setField(exercise, "exerciseAddr", ExerciseFixture.createExerciseAddr());
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
            @DisplayName("부모임장이_조회하면_isManager_false로_반환된다")
            void 부모임장이_조회하면_isManager_false로_반환된다() {
                // given
                Member subManager = MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 2003L);
                ReflectionTestUtils.setField(subManager, "id", 21L);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(subManager.getId()))
                        .willReturn(Optional.of(subManager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of());
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), subManager.getId(), Role.party_MANAGER))
                        .willReturn(false);

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), subManager.getId());

                // then
                assertThat(response.isManager()).isFalse();
            }

            @Test
            @DisplayName("모임_일반_멤버여도_isManager_false로_반환된다")
            void 모임_일반_멤버여도_isManager_false로_반환된다() {
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
            @DisplayName("모임_외부_회원도_상세_조회에_성공하고_isManager_false로_반환된다")
            void 모임_외부_회원도_상세_조회에_성공하고_isManager_false로_반환된다() {
                // given
                Member outsider = MemberFixture.createMember("외부회원", Gender.MALE, Level.C, 3003L);
                ReflectionTestUtils.setField(outsider, "id", 3L);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(outsider.getId()))
                        .willReturn(Optional.of(outsider));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of());
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), outsider.getId(), Role.party_MANAGER))
                        .willReturn(false);

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), outsider.getId());

                // then
                assertThat(response.isManager()).isFalse();
                assertThat(response.info().buildingName()).isEqualTo("테스트 체육관");
            }

            @Test
            @DisplayName("탈퇴_회원은_isWithdrawn_true로_반환된다")
            void 탈퇴_회원은_isWithdrawn_true로_반환된다() {
                // given
                Member withdrawnMember = MemberFixture.createWithdrawnMember("탈퇴회원", "탈퇴닉네임", 9999L);
                ReflectionTestUtils.setField(withdrawnMember, "id", 99L);

                MemberExercise memberExercise = MemberFixture.createMemberExercise(withdrawnMember, exercise);

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
                Guest guest = GuestFixture.createGuest(exercise, manager.getId());
                ReflectionTestUtils.setField(guest, "id", 70L);

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
                assertThat(participants.get(0).isWithdrawn()).isFalse();
                assertThat(participants.get(0).partyPosition()).isNull();
            }

            @Test
            @DisplayName("참가자_유형별_partyPosition이_올바르게_반환된다")
            void 참가자_유형별_partyPosition이_올바르게_반환된다() {
                // given
                Member subManager = MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 5003L);
                ReflectionTestUtils.setField(subManager, "id", 31L);

                Member normalMember = MemberFixture.createMember("일반멤버", Gender.MALE, Level.C, 5004L);
                ReflectionTestUtils.setField(normalMember, "id", 32L);

                Member outsider = MemberFixture.createMember("외부회원", Gender.FEMALE, Level.B, 5005L);
                ReflectionTestUtils.setField(outsider, "id", 33L);

                MemberExercise managerExercise = MemberFixture.createMemberExercise(manager, exercise);
                ReflectionTestUtils.setField(managerExercise, "createdAt", LocalDateTime.now().minusMinutes(5));

                MemberExercise subManagerExercise = MemberFixture.createMemberExercise(subManager, exercise);
                ReflectionTestUtils.setField(subManagerExercise, "createdAt", LocalDateTime.now().minusMinutes(4));

                MemberExercise normalMemberExercise = MemberFixture.createMemberExercise(normalMember, exercise);
                ReflectionTestUtils.setField(normalMemberExercise, "createdAt", LocalDateTime.now().minusMinutes(3));

                MemberExercise outsiderExercise = MemberFixture.createExternalMemberExercise(outsider, exercise);
                ReflectionTestUtils.setField(outsiderExercise, "createdAt", LocalDateTime.now().minusMinutes(2));

                Guest guest = GuestFixture.createGuest(exercise, manager.getId());
                ReflectionTestUtils.setField(guest, "id", 71L);
                ReflectionTestUtils.setField(guest, "createdAt", LocalDateTime.now().minusMinutes(1));

                MemberParty managerParty = MemberFixture.createMemberParty(party, manager, Role.party_MANAGER);
                MemberParty subManagerParty = MemberFixture.createMemberParty(party, subManager, Role.party_SUBMANAGER);
                MemberParty memberParty = MemberFixture.createMemberParty(party, normalMember, Role.party_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(managerExercise, subManagerExercise, normalMemberExercise, outsiderExercise));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of(guest));
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.party_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(manager.getId(), subManager.getId(), normalMember.getId(), outsider.getId())))
                        .willReturn(List.of(managerParty, subManagerParty, memberParty));
                given(memberRepository.findMemberNamesByIds(any()))
                        .willReturn(Map.of(manager.getId(), "모임장"));

                // when
                ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.participants().list())
                        .extracting(
                                ExerciseDetailDTO.ParticipantInfo::name,
                                ExerciseDetailDTO.ParticipantInfo::participantType,
                                ExerciseDetailDTO.ParticipantInfo::partyPosition)
                        .containsExactly(
                                tuple("모임장", "PARTY_MEMBER", "party_MANAGER"),
                                tuple("부모임장", "PARTY_MEMBER", "party_SUBMANAGER"),
                                tuple("일반멤버", "PARTY_MEMBER", "party_MEMBER"),
                                tuple("외부회원", "EXTERNAL_PARTICIPANT", null),
                                tuple("게스트", "GUEST", null)
                        );
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

    @Nested
    @DisplayName("getExerciseForEdit")
    class GetExerciseForEdit {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("운동 수정용 상세 정보의 모든 필드가 올바르게 반환된다")
            void 운동_수정용_상세_정보의_모든_필드가_올바르게_반환된다() {
                // given
                LocalDate targetDate = LocalDate.of(2026, 3, 24);
                Exercise exerciseForEdit = ExerciseFixture.createExerciseForEdit(party, targetDate);
                ReflectionTestUtils.setField(exerciseForEdit, "id", 101L);

                given(exerciseRepository.findExerciseWithBasicInfo(exerciseForEdit.getId()))
                        .willReturn(Optional.of(exerciseForEdit));

                // when
                ExerciseEditDetailDTO.Response response = exerciseQueryService.getExerciseForEdit(
                        exerciseForEdit.getId(), manager.getId());

                // then
                assertThat(response.date()).isEqualTo(targetDate);
                assertThat(response.buildingName()).isEqualTo("테스트 체육관");
                assertThat(response.roadAddress()).isEqualTo("서울특별시 강남구 테헤란로 1");
                assertThat(response.latitude()).isEqualTo(37.5);
                assertThat(response.longitude()).isEqualTo(127.0);
                assertThat(response.startTime()).isEqualTo(LocalTime.of(10, 0));
                assertThat(response.endTime()).isEqualTo(LocalTime.of(12, 30));
                assertThat(response.maxCapacity()).isEqualTo(18);
                assertThat(response.allowMemberGuestsInvitation()).isTrue();
                assertThat(response.allowExternalGuests()).isFalse();
                assertThat(response.notice()).isEqualTo("수정 공지사항");
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
                assertThatThrownBy(() -> exerciseQueryService.getExerciseForEdit(999L, manager.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.EXERCISE_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("getMyInvitedGuests")
    class GetMyInvitedGuests {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("내가_초대한_게스트만_참가번호와_대기상태와_함께_반환된다")
            void 내가_초대한_게스트만_참가번호와_대기상태와_함께_반환된다() {
                // given
                ReflectionTestUtils.setField(exercise, "maxCapacity", 1);

                Guest myFirstGuest = GuestFixture.createGuest(exercise, manager.getId(), "내게스트1", Gender.MALE);
                ReflectionTestUtils.setField(myFirstGuest, "id", 201L);
                ReflectionTestUtils.setField(myFirstGuest, "createdAt", LocalDateTime.now().minusMinutes(3));

                Guest otherInvitedGuest = GuestFixture.createGuest(exercise, 2L, "다른사람게스트", Gender.MALE);
                ReflectionTestUtils.setField(otherInvitedGuest, "id", 202L);
                ReflectionTestUtils.setField(otherInvitedGuest, "createdAt", LocalDateTime.now().minusMinutes(2));

                Guest mySecondGuest = GuestFixture.createGuest(exercise, manager.getId(), "내게스트2", Gender.FEMALE);
                ReflectionTestUtils.setField(mySecondGuest, "id", 203L);
                ReflectionTestUtils.setField(mySecondGuest, "createdAt", LocalDateTime.now().minusMinutes(1));

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(guestRepository.findByExerciseIdAndInviterId(exercise.getId(), manager.getId()))
                        .willReturn(List.of(myFirstGuest, mySecondGuest));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of());
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of(myFirstGuest, otherInvitedGuest, mySecondGuest));

                // when
                ExerciseMyGuestListDTO.Response response = exerciseQueryService.getMyInvitedGuests(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.totalCount()).isEqualTo(2);
                assertThat(response.maleCount()).isEqualTo(1);
                assertThat(response.femaleCount()).isEqualTo(1);
                assertThat(response.list())
                        .extracting(
                                ExerciseMyGuestListDTO.GuestInfo::guestId,
                                ExerciseMyGuestListDTO.GuestInfo::isWaiting,
                                ExerciseMyGuestListDTO.GuestInfo::participantNumber,
                                ExerciseMyGuestListDTO.GuestInfo::name,
                                ExerciseMyGuestListDTO.GuestInfo::gender,
                                ExerciseMyGuestListDTO.GuestInfo::level,
                                ExerciseMyGuestListDTO.GuestInfo::inviterName
                        )
                        .containsExactly(
                                tuple(201L, false, 1, "내게스트1", Gender.MALE, Level.B, manager.getMemberName()),
                                tuple(203L, true, 2, "내게스트2", Gender.FEMALE, Level.B, manager.getMemberName())
                        );
            }

            @Test
            @DisplayName("초대한_게스트가_없으면_빈_응답을_반환한다")
            void 초대한_게스트가_없으면_빈_응답을_반환한다() {
                // given
                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(guestRepository.findByExerciseIdAndInviterId(exercise.getId(), manager.getId()))
                        .willReturn(List.of());

                // when
                ExerciseMyGuestListDTO.Response response = exerciseQueryService.getMyInvitedGuests(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.totalCount()).isZero();
                assertThat(response.maleCount()).isZero();
                assertThat(response.femaleCount()).isZero();
                assertThat(response.list()).isEmpty();
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
                assertThatThrownBy(() -> exerciseQueryService.getMyInvitedGuests(999L, manager.getId()))
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
                assertThatThrownBy(() -> exerciseQueryService.getMyInvitedGuests(exercise.getId(), 999L))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("getPartyExerciseCalendar")
    class GetPartyExerciseCalendar {

        private Member partyMember;
        private Member outsiderMember;
        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void setUp() {
            partyMember = MemberFixture.createMember("파티멤버", Gender.FEMALE, Level.B, 3001L);
            ReflectionTestUtils.setField(partyMember, "id", 2L);

            outsiderMember = MemberFixture.createMember("외부멤버", Gender.MALE, Level.C, 3002L);
            ReflectionTestUtils.setField(outsiderMember, "id", 3L);

            party.addLevel(Gender.FEMALE, Level.B);
            party.addLevel(Gender.MALE, Level.A);

            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 3, 29);

            ReflectionTestUtils.setField(exercise, "date", LocalDate.of(2026, 3, 24));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("모임 운동 캘린더를 주차별_일자별로 반환한다")
            void 모임_운동_캘린더를_주차별_일자별로_반환한다() {
                // given
                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));
                given(memberPartyRepository.existsByPartyAndMember(party, partyMember))
                        .willReturn(true);
                given(exerciseRepository.findByPartyIdAndDateRange(party.getId(), startDate, endDate))
                        .willReturn(List.of(exercise));
                given(exerciseRepository.findExerciseParticipantCounts(party.getId(), startDate, endDate))
                        .willReturn(java.util.Collections.singletonList(new Object[]{exercise.getId(), 2}));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        partyMember.getId(), List.of(exercise.getId())))
                        .willReturn(List.of(exercise.getId()));
                given(memberExerciseRepository.findAllExerciseIdsByMemberAndExerciseIds(
                        partyMember.getId(), List.of(exercise.getId())))
                        .willReturn(List.of(exercise.getId()));

                // when
                PartyExerciseCalendarDTO.Response response = exerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), partyMember.getId(), startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.isMember()).isTrue();
                assertThat(response.partyName()).isEqualTo(party.getPartyName());
                assertThat(response.weeks()).hasSize(1);
                assertThat(response.weeks().get(0).weekStartDate()).isEqualTo(startDate);
                assertThat(response.weeks().get(0).weekEndDate()).isEqualTo(endDate);
                assertThat(response.weeks().get(0).days()).hasSize(7);
                assertThat(response.weeks().get(0).days().get(1).date())
                        .isEqualTo(LocalDate.of(2026, 3, 24));
                assertThat(response.weeks().get(0).days().get(1).exercises())
                        .extracting(
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::exerciseId,
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::isBookmarked,
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::buildingName,
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::currentParticipants,
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::maxCapacity,
                                PartyExerciseCalendarDTO.ExerciseCalendarItem::isParticipating)
                        .containsExactly(tuple(exercise.getId(), true, "테스트 체육관", 2, 10, true));
            }

            @Test
            @DisplayName("기간 내 운동이 없으면 빈 캘린더를 반환한다")
            void 기간_내_운동이_없으면_빈_캘린더를_반환한다() {
                // given
                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(outsiderMember.getId()))
                        .willReturn(Optional.of(outsiderMember));
                given(memberPartyRepository.existsByPartyAndMember(party, outsiderMember))
                        .willReturn(false);
                given(exerciseRepository.findByPartyIdAndDateRange(party.getId(), startDate, endDate))
                        .willReturn(List.of());

                // when
                PartyExerciseCalendarDTO.Response response = exerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), outsiderMember.getId(), startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.isMember()).isFalse();
                assertThat(response.partyName()).isEqualTo(party.getPartyName());
                assertThat(response.weeks()).isEmpty();
            }

            @Test
            @DisplayName("시작일과_종료일이_없으면_기본_기간이_적용된다")
            void 시작일과_종료일이_없으면_기본_기간이_적용된다() {
                // given
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate expectedEnd = ExerciseCalendarTestHelper.expectedDefaultEndDate();

                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));
                given(memberPartyRepository.existsByPartyAndMember(party, partyMember))
                        .willReturn(true);
                given(exerciseRepository.findByPartyIdAndDateRange(party.getId(), expectedStart, expectedEnd))
                        .willReturn(List.of());

                // when
                PartyExerciseCalendarDTO.Response response = exerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), partyMember.getId(), null, null);

                // then
                assertThat(response.startDate()).isEqualTo(expectedStart);
                assertThat(response.endDate()).isEqualTo(expectedEnd);
                assertThat(response.isMember()).isTrue();
                assertThat(response.weeks()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("시작일과 종료일이 함께 오지 않으면 예외를 던진다")
            void 시작일과_종료일이_함께_오지_않으면_예외를_던진다() {
                // given
                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), partyMember.getId(), startDate, null))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INCOMPLETE_DATE_RANGE);
            }

            @Test
            @DisplayName("삭제된 모임이면 예외를 던진다")
            void 삭제된_모임이면_예외를_던진다() {
                // given
                ReflectionTestUtils.setField(party, "status", PartyStatus.INACTIVE);

                given(partyRepository.findByIdWithLevels(party.getId()))
                        .willReturn(Optional.of(party));
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getPartyExerciseCalendar(
                        party.getId(), partyMember.getId(), startDate, endDate))
                        .isInstanceOf(PartyException.class)
                        .hasFieldOrPropertyWithValue("code", PartyErrorCode.PARTY_IS_DELETED);
            }
        }
    }
}
