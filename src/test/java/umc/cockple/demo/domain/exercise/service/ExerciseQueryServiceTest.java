package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseBuildingDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseEditDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMapBuildingsDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMyGuestListDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyExerciseListDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.dto.MyPartyExerciseDTO;
import umc.cockple.demo.domain.exercise.dto.PartyExerciseCalendarDTO;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.member.domain.MemberAddr;
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
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Nested
    @DisplayName("getMyExerciseCalendar")
    class GetMyExerciseCalendar {

        private Member calendarMember;
        private LocalDate startDate;
        private LocalDate endDate;
        private Exercise myExercise;

        @BeforeEach
        void setUp() {
            calendarMember = MemberFixture.createMember("캘린더멤버", Gender.FEMALE, Level.B, 4001L);
            ReflectionTestUtils.setField(calendarMember, "id", 4L);

            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 3, 29);

            myExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 3, 25));
            ReflectionTestUtils.setField(myExercise, "id", 200L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("내 운동 캘린더를 주차별_일자별로 반환한다")
            void 내_운동_캘린더를_주차별_일자별로_반환한다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(exerciseRepository.findByMemberIdAndDateRange(calendarMember.getId(), startDate, endDate))
                        .willReturn(List.of(myExercise));

                // when
                MyExerciseCalendarDTO.Response response = exerciseQueryService.getMyExerciseCalendar(
                        calendarMember.getId(), startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).hasSize(1);
                assertThat(response.weeks().get(0).weekStartDate()).isEqualTo(startDate);
                assertThat(response.weeks().get(0).weekEndDate()).isEqualTo(endDate);
                assertThat(response.weeks().get(0).days()).hasSize(7);
                assertThat(response.weeks().get(0).days().get(2).date())
                        .isEqualTo(LocalDate.of(2026, 3, 25));
                assertThat(response.weeks().get(0).days().get(2).exercises())
                        .extracting(
                                MyExerciseCalendarDTO.ExerciseCalendarItem::exerciseId,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::partyId,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::partyName,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::buildingName,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::startTime,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::endTime,
                                MyExerciseCalendarDTO.ExerciseCalendarItem::profileImageUrl)
                        .containsExactly(tuple(200L, 10L, "테스트 모임", "테스트 체육관", LocalTime.of(10, 0), null, null));
            }

            @Test
            @DisplayName("기간 내 참여 운동이 없으면 빈 캘린더를 반환한다")
            void 기간_내_참여_운동이_없으면_빈_캘린더를_반환한다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(exerciseRepository.findByMemberIdAndDateRange(calendarMember.getId(), startDate, endDate))
                        .willReturn(List.of());

                // when
                MyExerciseCalendarDTO.Response response = exerciseQueryService.getMyExerciseCalendar(
                        calendarMember.getId(), startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).isEmpty();
            }

            @Test
            @DisplayName("시작일과_종료일이_없으면_기본_기간이_적용된다")
            void 시작일과_종료일이_없으면_기본_기간이_적용된다() {
                // given
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate expectedEnd = ExerciseCalendarTestHelper.expectedDefaultEndDate();

                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(exerciseRepository.findByMemberIdAndDateRange(calendarMember.getId(), expectedStart, expectedEnd))
                        .willReturn(List.of());

                // when
                MyExerciseCalendarDTO.Response response = exerciseQueryService.getMyExerciseCalendar(
                        calendarMember.getId(), null, null);

                // then
                assertThat(response.startDate()).isEqualTo(expectedStart);
                assertThat(response.endDate()).isEqualTo(expectedEnd);
                assertThat(response.weeks()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_멤버면_예외를_던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getMyExerciseCalendar(999L, startDate, endDate))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("시작일과 종료일이 함께 오지 않으면 예외를 던진다")
            void 시작일과_종료일이_함께_오지_않으면_예외를_던진다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getMyExerciseCalendar(
                        calendarMember.getId(), startDate, null))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INCOMPLETE_DATE_RANGE);
            }

            @Test
            @DisplayName("시작일이 종료일과 같거나 늦으면 예외를 던진다")
            void 시작일이_종료일과_같거나_늦으면_예외를_던진다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getMyExerciseCalendar(
                        calendarMember.getId(), endDate, startDate))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INVALID_DATE_RANGE);
            }
        }
    }

    @Nested
    @DisplayName("getMyPartyExercise")
    class GetMyPartyExercise {

        private Member partyMember;
        private Exercise firstUpcomingExercise;
        private Exercise secondUpcomingExercise;

        @BeforeEach
        void setUp() {
            partyMember = MemberFixture.createMember("내모임멤버", Gender.MALE, Level.B, 5001L);
            ReflectionTestUtils.setField(partyMember, "id", 5L);

            firstUpcomingExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 4, 1));
            ReflectionTestUtils.setField(firstUpcomingExercise, "id", 301L);

            secondUpcomingExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 4, 2));
            ReflectionTestUtils.setField(secondUpcomingExercise, "id", 302L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("내 모임의 예정된 운동 목록을 반환한다")
            void 내_모임의_예정된_운동_목록을_반환한다() {
                // given
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));
                given(memberPartyRepository.findPartyIdsByMemberId(partyMember.getId()))
                        .willReturn(List.of(party.getId()));
                given(exerciseRepository.findRecentExercisesByPartyIds(eq(List.of(party.getId())), argThat(
                        (org.springframework.data.domain.Pageable pageable) -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 6)))
                        .willReturn(List.of(firstUpcomingExercise, secondUpcomingExercise));

                // when
                MyPartyExerciseDTO.Response response = exerciseQueryService.getMyPartyExercise(partyMember.getId());

                // then
                assertThat(response.totalExercises()).isEqualTo(2);
                assertThat(response.exercises())
                        .extracting(
                                MyPartyExerciseDTO.Exercises::exerciseId,
                                MyPartyExerciseDTO.Exercises::partyId,
                                MyPartyExerciseDTO.Exercises::partyName,
                                MyPartyExerciseDTO.Exercises::buildingName,
                                MyPartyExerciseDTO.Exercises::date,
                                MyPartyExerciseDTO.Exercises::dayOfWeek,
                                MyPartyExerciseDTO.Exercises::startTime,
                                MyPartyExerciseDTO.Exercises::profileImageUrl)
                        .containsExactly(
                                tuple(301L, 10L, "테스트 모임", "테스트 체육관", LocalDate.of(2026, 4, 1), "WEDNESDAY", LocalTime.of(10, 0), null),
                                tuple(302L, 10L, "테스트 모임", "테스트 체육관", LocalDate.of(2026, 4, 2), "THURSDAY", LocalTime.of(10, 0), null)
                        );
            }

            @Test
            @DisplayName("속한 모임이 없으면 빈 응답을 반환한다")
            void 속한_모임이_없으면_빈_응답을_반환한다() {
                // given
                given(memberRepository.findById(partyMember.getId()))
                        .willReturn(Optional.of(partyMember));
                given(memberPartyRepository.findPartyIdsByMemberId(partyMember.getId()))
                        .willReturn(List.of());

                // when
                MyPartyExerciseDTO.Response response = exerciseQueryService.getMyPartyExercise(partyMember.getId());

                // then
                assertThat(response.totalExercises()).isZero();
                assertThat(response.exercises()).isEmpty();
                verify(exerciseRepository, never()).findRecentExercisesByPartyIds(any(), any());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_멤버면_예외를_던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getMyPartyExercise(999L))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("getMyPartyExerciseCalendar")
    class GetMyPartyExerciseCalendar {

        private Member calendarMember;
        private Exercise calendarExercise;
        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void setUp() {
            calendarMember = MemberFixture.createMember("내모임캘린더멤버", Gender.FEMALE, Level.B, 6001L);
            ReflectionTestUtils.setField(calendarMember, "id", 6L);

            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 3, 29);

            calendarExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 3, 25));
            ReflectionTestUtils.setField(calendarExercise, "id", 400L);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("내 모임 운동 캘린더를 주차별_일자별로 반환한다")
            void 내_모임_운동_캘린더를_주차별_일자별로_반환한다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(memberPartyRepository.findPartyIdsByMemberId(calendarMember.getId()))
                        .willReturn(List.of(party.getId()));
                given(exerciseRepository.findByPartyIdsAndDateRange(List.of(party.getId()), startDate, endDate))
                        .willReturn(List.of(calendarExercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        calendarMember.getId(), List.of(calendarExercise.getId())))
                        .willReturn(List.of());
                given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                        List.of(calendarExercise.getId()), startDate, endDate))
                        .willReturn(Collections.singletonList(new Object[]{calendarExercise.getId(), 3}));

                // when
                MyPartyExerciseCalendarDTO.Response response = exerciseQueryService.getMyPartyExerciseCalendar(
                        calendarMember.getId(), MyPartyExerciseOrderType.LATEST, startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).hasSize(1);
                assertThat(response.weeks().get(0).weekStartDate()).isEqualTo(startDate);
                assertThat(response.weeks().get(0).weekEndDate()).isEqualTo(endDate);
                assertThat(response.weeks().get(0).days()).hasSize(7);
                assertThat(response.weeks().get(0).days().get(2).date())
                        .isEqualTo(LocalDate.of(2026, 3, 25));
                assertThat(response.weeks().get(0).days().get(2).exercises())
                        .extracting(
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::exerciseId,
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::partyId,
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::partyName,
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::buildingName,
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::isBookmarked,
                                MyPartyExerciseCalendarDTO.ExerciseCalendarItem::nowCapacity)
                        .containsExactly(tuple(400L, 10L, "테스트 모임", "테스트 체육관", false, 3));
            }

            @Test
            @DisplayName("북마크한 운동은 isBookmarked가 true로 반환된다")
            void 북마크한_운동은_isBookmarked가_true로_반환된다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(memberPartyRepository.findPartyIdsByMemberId(calendarMember.getId()))
                        .willReturn(List.of(party.getId()));
                given(exerciseRepository.findByPartyIdsAndDateRange(List.of(party.getId()), startDate, endDate))
                        .willReturn(List.of(calendarExercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        calendarMember.getId(), List.of(calendarExercise.getId())))
                        .willReturn(List.of(calendarExercise.getId()));
                given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                        List.of(calendarExercise.getId()), startDate, endDate))
                        .willReturn(List.of());

                // when
                MyPartyExerciseCalendarDTO.Response response = exerciseQueryService.getMyPartyExerciseCalendar(
                        calendarMember.getId(), MyPartyExerciseOrderType.LATEST, startDate, endDate);

                // then
                assertThat(response.weeks().get(0).days().get(2).exercises().get(0).isBookmarked()).isTrue();
            }

            @Test
            @DisplayName("속한 모임이 없으면 빈 캘린더를 반환한다")
            void 속한_모임이_없으면_빈_캘린더를_반환한다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(memberPartyRepository.findPartyIdsByMemberId(calendarMember.getId()))
                        .willReturn(List.of());

                // when
                MyPartyExerciseCalendarDTO.Response response = exerciseQueryService.getMyPartyExerciseCalendar(
                        calendarMember.getId(), MyPartyExerciseOrderType.LATEST, startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).isEmpty();
                verify(exerciseRepository, never()).findByPartyIdsAndDateRange(any(), any(), any());
            }

            @Test
            @DisplayName("기간 내 내 모임 운동이 없으면 빈 캘린더를 반환한다")
            void 기간_내_내_모임_운동이_없으면_빈_캘린더를_반환한다() {
                // given
                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(memberPartyRepository.findPartyIdsByMemberId(calendarMember.getId()))
                        .willReturn(List.of(party.getId()));
                given(exerciseRepository.findByPartyIdsAndDateRange(List.of(party.getId()), startDate, endDate))
                        .willReturn(List.of());

                // when
                MyPartyExerciseCalendarDTO.Response response = exerciseQueryService.getMyPartyExerciseCalendar(
                        calendarMember.getId(), MyPartyExerciseOrderType.LATEST, startDate, endDate);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).isEmpty();
            }

            @Test
            @DisplayName("시작일과_종료일이_없으면_기본_기간이_적용된다")
            void 시작일과_종료일이_없으면_기본_기간이_적용된다() {
                // given
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate expectedEnd = ExerciseCalendarTestHelper.expectedDefaultEndDate();

                given(memberRepository.findById(calendarMember.getId()))
                        .willReturn(Optional.of(calendarMember));
                given(memberPartyRepository.findPartyIdsByMemberId(calendarMember.getId()))
                        .willReturn(List.of(party.getId()));
                given(exerciseRepository.findByPartyIdsAndDateRange(List.of(party.getId()), expectedStart, expectedEnd))
                        .willReturn(List.of());

                // when
                MyPartyExerciseCalendarDTO.Response response = exerciseQueryService.getMyPartyExerciseCalendar(
                        calendarMember.getId(), MyPartyExerciseOrderType.LATEST, null, null);

                // then
                assertThat(response.startDate()).isEqualTo(expectedStart);
                assertThat(response.endDate()).isEqualTo(expectedEnd);
                assertThat(response.weeks()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_멤버면_예외를_던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getMyPartyExerciseCalendar(
                        999L, MyPartyExerciseOrderType.LATEST, startDate, endDate))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("getMyExercises")
    class GetMyExercises {

        private Member myExerciseMember;
        private Exercise completedExercise;
        private Exercise upcomingExercise;
        private Exercise futureLatestExercise;
        private Pageable firstPage;

        @BeforeEach
        void setUp() {
            myExerciseMember = MemberFixture.createMember("내참여운동멤버", Gender.MALE, Level.B, 7001L,
                    LocalDate.of(2000, 1, 1));
            ReflectionTestUtils.setField(myExerciseMember, "id", 7L);

            party.addLevel(Gender.FEMALE, Level.B);
            party.addLevel(Gender.MALE, Level.A);

            completedExercise = createMyExercise(701L, LocalDate.of(2024, 1, 5),
                    LocalTime.of(9, 0), LocalTime.of(11, 0), 18, false);
            upcomingExercise = createMyExercise(702L, LocalDate.of(2099, 1, 3),
                    LocalTime.of(18, 0), null, 12, true);
            futureLatestExercise = createMyExercise(703L, LocalDate.of(2099, 1, 10),
                    LocalTime.of(7, 30), LocalTime.of(9, 0), 20, true);
            firstPage = PageRequest.of(0, 2);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("ALL 최신순은 전체 운동 리포지토리를 날짜 내림차순으로 호출한다")
            void ALL_최신순은_전체_운동_리포지토리를_날짜_내림차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.DESC, Sort.Direction.DESC))))
                        .willReturn(emptySlice(firstPage));

                // when
                MyExerciseListDTO.Response response = exerciseQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.ALL, MyExerciseOrderType.LATEST, firstPage);

                // then
                assertThat(response.totalCount()).isZero();
                assertThat(response.hasNext()).isFalse();
                assertThat(response.exercises()).isEmpty();
                verify(exerciseRepository).findMyExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
                verify(exerciseRepository, never()).findMyUpcomingExercisesWithPaging(any(), any());
                verify(exerciseRepository, never()).findMyCompletedExercisesWithPaging(any(), any());
            }

            @Test
            @DisplayName("UPCOMING 최신순은 예정 운동 리포지토리를 날짜 오름차순으로 호출한다")
            void UPCOMING_최신순은_예정_운동_리포지토리를_날짜_오름차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyUpcomingExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.ASC, Sort.Direction.ASC))))
                        .willReturn(emptySlice(firstPage));

                // when
                exerciseQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.UPCOMING, MyExerciseOrderType.LATEST, firstPage);

                // then
                verify(exerciseRepository, never()).findMyExercisesWithPaging(any(), any());
                verify(exerciseRepository).findMyUpcomingExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
                verify(exerciseRepository, never()).findMyCompletedExercisesWithPaging(any(), any());
            }

            @Test
            @DisplayName("COMPLETED 최신순은 완료 운동 리포지토리를 날짜 내림차순으로 호출한다")
            void COMPLETED_최신순은_완료_운동_리포지토리를_날짜_내림차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyCompletedExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.DESC, Sort.Direction.DESC))))
                        .willReturn(emptySlice(firstPage));

                // when
                exerciseQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.COMPLETED, MyExerciseOrderType.LATEST, firstPage);

                // then
                verify(exerciseRepository, never()).findMyExercisesWithPaging(any(), any());
                verify(exerciseRepository, never()).findMyUpcomingExercisesWithPaging(any(), any());
                verify(exerciseRepository).findMyCompletedExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
            }

            @Test
            @DisplayName("ALL 오래된순은 전체 운동 리포지토리를 날짜 오름차순으로 호출한다")
            void ALL_오래된순은_전체_운동_리포지토리를_날짜_오름차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.ASC, Sort.Direction.ASC))))
                        .willReturn(emptySlice(firstPage));

                // when
                exerciseQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.ALL, MyExerciseOrderType.OLDEST, firstPage);

                // then
                verify(exerciseRepository).findMyExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
            }

            @Test
            @DisplayName("UPCOMING 오래된순은 예정 운동 리포지토리를 날짜 내림차순으로 호출한다")
            void UPCOMING_오래된순은_예정_운동_리포지토리를_날짜_내림차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyUpcomingExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.DESC, Sort.Direction.DESC))))
                        .willReturn(emptySlice(firstPage));

                // when
                exerciseQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.UPCOMING, MyExerciseOrderType.OLDEST, firstPage);

                // then
                verify(exerciseRepository).findMyUpcomingExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
            }

            @Test
            @DisplayName("COMPLETED 오래된순은 완료 운동 리포지토리를 날짜 오름차순으로 호출한다")
            void COMPLETED_오래된순은_완료_운동_리포지토리를_날짜_오름차순으로_호출한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyCompletedExercisesWithPaging(eq(myExerciseMember.getId()), argThat(
                        pageable -> matchesSort(pageable, Sort.Direction.ASC, Sort.Direction.ASC))))
                        .willReturn(emptySlice(firstPage));

                // when
                exerciseQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.COMPLETED, MyExerciseOrderType.OLDEST, firstPage);

                // then
                verify(exerciseRepository).findMyCompletedExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class));
            }

            @Test
            @DisplayName("조회된 운동이 없으면 빈 응답을 반환한다")
            void 조회된_운동이_없으면_빈_응답을_반환한다() {
                // given
                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class)))
                        .willReturn(emptySlice(firstPage));

                // when
                MyExerciseListDTO.Response response = exerciseQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.ALL, MyExerciseOrderType.LATEST, firstPage);

                // then
                assertThat(response.totalCount()).isZero();
                assertThat(response.hasNext()).isFalse();
                assertThat(response.exercises()).isEmpty();
                verify(exerciseRepository, never()).findExerciseParticipantCountsByExerciseIds(any());
                verify(exerciseBookmarkRepository, never()).findAllExerciseIdsByMemberIdAndExerciseIds(any(), any());
            }

            @Test
            @DisplayName("조회 결과를 DTO 필드와 hasNext true로 매핑한다")
            void 조회_결과를_DTO_필드와_hasNext_true로_매핑한다() {
                // given
                Slice<Exercise> exerciseSlice = sliceOf(List.of(futureLatestExercise, completedExercise), true, firstPage);

                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class)))
                        .willReturn(exerciseSlice);
                given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                        List.of(futureLatestExercise.getId(), completedExercise.getId())))
                        .willReturn(List.of(
                                new Object[]{futureLatestExercise.getId(), 3},
                                new Object[]{completedExercise.getId(), 1}
                        ));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        myExerciseMember.getId(), List.of(futureLatestExercise.getId(), completedExercise.getId())))
                        .willReturn(List.of(futureLatestExercise.getId()));

                // when
                MyExerciseListDTO.Response response = exerciseQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.ALL, MyExerciseOrderType.LATEST, firstPage);

                // then
                assertThat(response.totalCount()).isEqualTo(2);
                assertThat(response.hasNext()).isTrue();
                assertThat(response.exercises())
                        .extracting(
                                MyExerciseListDTO.ExerciseItem::exerciseId,
                                MyExerciseListDTO.ExerciseItem::partyId,
                                MyExerciseListDTO.ExerciseItem::partyName,
                                MyExerciseListDTO.ExerciseItem::isBookmarked,
                                MyExerciseListDTO.ExerciseItem::date,
                                MyExerciseListDTO.ExerciseItem::dayOfWeek,
                                MyExerciseListDTO.ExerciseItem::buildingName,
                                MyExerciseListDTO.ExerciseItem::startTime,
                                MyExerciseListDTO.ExerciseItem::endTime,
                                MyExerciseListDTO.ExerciseItem::currentParticipants,
                                MyExerciseListDTO.ExerciseItem::maxCapacity,
                                MyExerciseListDTO.ExerciseItem::isCompleted,
                                MyExerciseListDTO.ExerciseItem::partyGuestInviteAccept
                        )
                        .containsExactly(
                                tuple(703L, 10L, "테스트 모임", true,
                                        LocalDate.of(2099, 1, 10), "SATURDAY", "테스트 체육관",
                                        LocalTime.of(7, 30), LocalTime.of(9, 0), 3, 20, false, true),
                                tuple(701L, 10L, "테스트 모임", false,
                                        LocalDate.of(2024, 1, 5), "FRIDAY", "테스트 체육관",
                                        LocalTime.of(9, 0), LocalTime.of(11, 0), 1, 18, true, false)
                        );
            }

            @Test
            @DisplayName("조회 결과를 hasNext false로 매핑한다")
            void 조회_결과를_hasNext_false로_매핑한다() {
                // given
                Pageable secondPage = PageRequest.of(1, 1);
                Slice<Exercise> exerciseSlice = sliceOf(List.of(upcomingExercise), false, secondPage);

                given(memberRepository.findById(myExerciseMember.getId()))
                        .willReturn(Optional.of(myExerciseMember));
                given(exerciseRepository.findMyExercisesWithPaging(eq(myExerciseMember.getId()), any(Pageable.class)))
                        .willReturn(exerciseSlice);
                given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(List.of(upcomingExercise.getId())))
                        .willReturn(List.of());
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        myExerciseMember.getId(), List.of(upcomingExercise.getId())))
                        .willReturn(List.of());

                // when
                MyExerciseListDTO.Response response = exerciseQueryService.getMyExercises(
                        myExerciseMember.getId(), MyExerciseFilterType.ALL, MyExerciseOrderType.LATEST, secondPage);

                // then
                assertThat(response.totalCount()).isEqualTo(1);
                assertThat(response.hasNext()).isFalse();
                assertThat(response.exercises().get(0).exerciseId()).isEqualTo(upcomingExercise.getId());
                assertThat(response.exercises().get(0).isCompleted()).isFalse();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 멤버면 예외를 던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getMyExercises(
                        999L, MyExerciseFilterType.ALL, MyExerciseOrderType.LATEST, firstPage))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }
        }

        private Exercise createMyExercise(long id, LocalDate date, LocalTime startTime,
                                          LocalTime endTime, int maxCapacity, boolean partyGuestAccept) {
            Exercise createdExercise = ExerciseFixture.createExerciseWithAddr(party, date, maxCapacity);
            ReflectionTestUtils.setField(createdExercise, "id", id);
            ReflectionTestUtils.setField(createdExercise, "startTime", startTime);
            ReflectionTestUtils.setField(createdExercise, "endTime", endTime);
            ReflectionTestUtils.setField(createdExercise, "partyGuestAccept", partyGuestAccept);
            return createdExercise;
        }

        private Slice<Exercise> emptySlice(Pageable pageable) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        private Slice<Exercise> sliceOf(List<Exercise> exercises, boolean hasNext, Pageable pageable) {
            return new SliceImpl<>(exercises, pageable, hasNext);
        }

        private boolean matchesSort(Pageable pageable, Sort.Direction dateDirection, Sort.Direction timeDirection) {
            if (pageable.getPageNumber() != firstPage.getPageNumber() || pageable.getPageSize() != firstPage.getPageSize()) {
                return false;
            }

            List<Sort.Order> orders = pageable.getSort().stream().toList();
            return orders.size() == 2
                   && orders.get(0).getProperty().equals("date")
                   && orders.get(0).getDirection() == dateDirection
                   && orders.get(1).getProperty().equals("startTime")
                   && orders.get(1).getDirection() == timeDirection;
        }
    }

    @Nested
    @DisplayName("getBuildingExerciseDetails")
    class GetBuildingExerciseDetails {

        private Member buildingMember;
        private LocalDate targetDate;
        private String buildingName;
        private String streetAddr;

        @BeforeEach
        void setUp() {
            buildingMember = MemberFixture.createMember("건물상세멤버", Gender.FEMALE, Level.B, 8001L,
                    LocalDate.of(2000, 1, 1));
            ReflectionTestUtils.setField(buildingMember, "id", 8L);

            targetDate = LocalDate.of(2026, 5, 10);
            buildingName = "콕플 타워";
            streetAddr = "서울특별시 강남구 테헤란로 10";
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("해당 건물 운동이 없으면 메타데이터가 포함된 빈 응답을 반환한다")
            void 해당_건물_운동이_없으면_메타데이터가_포함된_빈_응답을_반환한다() {
                // given
                given(memberRepository.findById(buildingMember.getId()))
                        .willReturn(Optional.of(buildingMember));
                given(exerciseRepository.findExercisesByBuildingAndDate(buildingName, streetAddr, targetDate))
                        .willReturn(List.of());

                // when
                ExerciseBuildingDetailDTO.Response response = exerciseQueryService.getBuildingExerciseDetails(
                        buildingName, streetAddr, targetDate, buildingMember.getId());

                // then
                assertThat(response.date()).isEqualTo(targetDate);
                assertThat(response.dayOfWeek()).isEqualTo("SUNDAY");
                assertThat(response.buildingName()).isEqualTo(buildingName);
                assertThat(response.exercises()).isEmpty();
                verify(exerciseRepository).findExercisesByBuildingAndDate(buildingName, streetAddr, targetDate);
                verify(exerciseBookmarkRepository, never()).findAllExerciseIdsByMemberIdAndExerciseIds(any(), any());
            }

            @Test
            @DisplayName("운동 목록을 순서와 북마크 상태를 유지해 DTO로 반환한다")
            void 운동_목록을_순서와_북마크_상태를_유지해_DTO로_반환한다() {
                // given
                Exercise morningExercise = createBuildingExercise(801L, LocalTime.of(9, 0), LocalTime.of(11, 0));
                Exercise eveningExercise = createBuildingExercise(802L, LocalTime.of(19, 0), LocalTime.of(21, 0));

                given(memberRepository.findById(buildingMember.getId()))
                        .willReturn(Optional.of(buildingMember));
                given(exerciseRepository.findExercisesByBuildingAndDate(buildingName, streetAddr, targetDate))
                        .willReturn(List.of(morningExercise, eveningExercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        buildingMember.getId(), List.of(morningExercise.getId(), eveningExercise.getId())))
                        .willReturn(List.of(eveningExercise.getId()));

                // when
                ExerciseBuildingDetailDTO.Response response = exerciseQueryService.getBuildingExerciseDetails(
                        buildingName, streetAddr, targetDate, buildingMember.getId());

                // then
                assertThat(response.date()).isEqualTo(targetDate);
                assertThat(response.dayOfWeek()).isEqualTo("SUNDAY");
                assertThat(response.buildingName()).isEqualTo(buildingName);
                assertThat(response.exercises())
                        .extracting(
                                ExerciseBuildingDetailDTO.ExerciseItem::exerciseId,
                                ExerciseBuildingDetailDTO.ExerciseItem::partyId,
                                ExerciseBuildingDetailDTO.ExerciseItem::partyName,
                                ExerciseBuildingDetailDTO.ExerciseItem::profileImageUrl,
                                ExerciseBuildingDetailDTO.ExerciseItem::isBookmarked,
                                ExerciseBuildingDetailDTO.ExerciseItem::startTime,
                                ExerciseBuildingDetailDTO.ExerciseItem::endTime
                        )
                        .containsExactly(
                                tuple(801L, 10L, "테스트 모임", null, false, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                                tuple(802L, 10L, "테스트 모임", null, true, LocalTime.of(19, 0), LocalTime.of(21, 0))
                        );
                verify(exerciseRepository).findExercisesByBuildingAndDate(buildingName, streetAddr, targetDate);
                verify(exerciseBookmarkRepository).findAllExerciseIdsByMemberIdAndExerciseIds(
                        buildingMember.getId(), List.of(morningExercise.getId(), eveningExercise.getId()));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 멤버면 예외를 던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findById(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getBuildingExerciseDetails(
                        buildingName, streetAddr, targetDate, 999L))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }
        }

        private Exercise createBuildingExercise(long id, LocalTime startTime, LocalTime endTime) {
            Exercise buildingExercise = ExerciseFixture.createExerciseWithAddr(party, targetDate, 12);
            ReflectionTestUtils.setField(buildingExercise, "id", id);
            ReflectionTestUtils.setField(buildingExercise, "startTime", startTime);
            ReflectionTestUtils.setField(buildingExercise, "endTime", endTime);
            ReflectionTestUtils.setField(buildingExercise, "exerciseAddr",
                    ExerciseFixture.createExerciseAddr(buildingName, streetAddr));
            return buildingExercise;
        }
    }

    @Nested
    @DisplayName("getExerciseMapCalendarSummary")
    class GetExerciseMapCalendarSummary {

        private Member mapMember;
        private Member memberWithoutMainAddr;
        private MemberAddr mainAddr;
        private Double radiusKm;

        @BeforeEach
        void setUp() {
            mapMember = MemberFixture.createMember("지도멤버", Gender.MALE, Level.B, 9001L,
                    LocalDate.of(2000, 1, 1));
            ReflectionTestUtils.setField(mapMember, "id", 9L);

            mainAddr = MemberAddr.builder()
                    .member(mapMember)
                    .addr1("서울특별시")
                    .addr2("강남구")
                    .addr3("역삼동")
                    .streetAddr("서울특별시 강남구 테헤란로 1")
                    .buildingName("대표주소")
                    .latitude(37.501)
                    .longitude(127.039)
                    .isMain(true)
                    .build();
            ReflectionTestUtils.setField(mapMember, "addresses", List.of(mainAddr));

            memberWithoutMainAddr = MemberFixture.createMember("대표주소없음", Gender.FEMALE, Level.C, 9002L,
                    LocalDate.of(2001, 1, 1));
            ReflectionTestUtils.setField(memberWithoutMainAddr, "id", 10L);
            MemberAddr subAddr = MemberAddr.builder()
                    .member(memberWithoutMainAddr)
                    .addr1("서울특별시")
                    .addr2("송파구")
                    .addr3("잠실동")
                    .streetAddr("서울특별시 송파구 올림픽로 1")
                    .buildingName("서브주소")
                    .latitude(37.514)
                    .longitude(127.102)
                    .isMain(false)
                    .build();
            ReflectionTestUtils.setField(memberWithoutMainAddr, "addresses", List.of(subAddr));

            radiusKm = 3.9;
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("date가 null이면 현재 월 범위와 대표주소 좌표로 조회한다")
            void date가_null이면_현재_월_범위와_대표주소_좌표로_조회한다() {
                // given
                YearMonth currentMonth = YearMonth.now();
                LocalDate monthStart = currentMonth.atDay(1);
                LocalDate monthEnd = currentMonth.atEndOfMonth();

                given(memberRepository.findMemberWithAddresses(mapMember.getId()))
                        .willReturn(Optional.of(mapMember));
                given(exerciseRepository.findExercisesByMonthAndRadius(
                        eq(monthStart), eq(monthEnd), eq(mainAddr.getLatitude()), eq(mainAddr.getLongitude()), eq(3)))
                        .willReturn(List.of());

                // when
                ExerciseMapBuildingsDTO.Response response = exerciseQueryService.getExerciseMapCalendarSummary(
                        null, null, null, radiusKm, mapMember.getId());

                // then
                assertThat(response.year()).isEqualTo(currentMonth.getYear());
                assertThat(response.month()).isEqualTo(currentMonth.getMonthValue());
                assertThat(response.centerLatitude()).isEqualTo(mainAddr.getLatitude());
                assertThat(response.centerLongitude()).isEqualTo(mainAddr.getLongitude());
                assertThat(response.radiusKm()).isEqualTo(radiusKm);
                assertThat(response.buildings()).isEmpty();
            }

            @Test
            @DisplayName("명시 좌표가 있으면 대표주소 대신 해당 좌표와 절삭 반경으로 조회한다")
            void 명시_좌표가_있으면_대표주소_대신_해당_좌표와_절삭_반경으로_조회한다() {
                // given
                LocalDate targetDate = LocalDate.of(2026, 4, 15);
                LocalDate monthStart = LocalDate.of(2026, 4, 1);
                LocalDate monthEnd = LocalDate.of(2026, 4, 30);

                given(memberRepository.findMemberWithAddresses(mapMember.getId()))
                        .willReturn(Optional.of(mapMember));
                given(exerciseRepository.findExercisesByMonthAndRadius(
                        eq(monthStart), eq(monthEnd), eq(37.55), eq(127.11), eq(3)))
                        .willReturn(List.of());

                // when
                ExerciseMapBuildingsDTO.Response response = exerciseQueryService.getExerciseMapCalendarSummary(
                        targetDate, 37.55, 127.11, radiusKm, mapMember.getId());

                // then
                assertThat(response.year()).isEqualTo(2026);
                assertThat(response.month()).isEqualTo(4);
                assertThat(response.centerLatitude()).isEqualTo(37.55);
                assertThat(response.centerLongitude()).isEqualTo(127.11);
                assertThat(response.radiusKm()).isEqualTo(radiusKm);
                assertThat(response.buildings()).isEmpty();
            }

            @Test
            @DisplayName("운동을 날짜별과 건물별로 그룹화해 응답을 만든다")
            void 운동을_날짜별과_건물별로_그룹화해_응답을_만든다() {
                // given
                LocalDate targetDate = LocalDate.of(2026, 4, 15);
                Exercise dayOneMorning = createMapExercise(901L, LocalDate.of(2026, 4, 3),
                        "A빌딩", "서울특별시 강남구 테헤란로 10", 37.501, 127.041, LocalTime.of(9, 0));
                Exercise dayOneEveningSameBuilding = createMapExercise(902L, LocalDate.of(2026, 4, 3),
                        "A빌딩", "서울특별시 강남구 테헤란로 10", 37.501, 127.041, LocalTime.of(19, 0));
                Exercise dayOneOtherBuilding = createMapExercise(903L, LocalDate.of(2026, 4, 3),
                        "B빌딩", "서울특별시 강남구 테헤란로 20", 37.502, 127.042, LocalTime.of(13, 0));
                Exercise dayTwoBuilding = createMapExercise(904L, LocalDate.of(2026, 4, 4),
                        "A빌딩", "서울특별시 강남구 테헤란로 10", 37.501, 127.041, LocalTime.of(10, 0));

                given(memberRepository.findMemberWithAddresses(mapMember.getId()))
                        .willReturn(Optional.of(mapMember));
                given(exerciseRepository.findExercisesByMonthAndRadius(any(), any(), any(), any(), any()))
                        .willReturn(List.of(dayOneMorning, dayOneEveningSameBuilding, dayOneOtherBuilding, dayTwoBuilding));

                // when
                ExerciseMapBuildingsDTO.Response response = exerciseQueryService.getExerciseMapCalendarSummary(
                        targetDate, null, null, radiusKm, mapMember.getId());

                // then
                assertThat(response.year()).isEqualTo(2026);
                assertThat(response.month()).isEqualTo(4);
                assertThat(response.centerLatitude()).isEqualTo(mainAddr.getLatitude());
                assertThat(response.centerLongitude()).isEqualTo(mainAddr.getLongitude());
                assertThat(response.radiusKm()).isEqualTo(radiusKm);
                assertThat(response.buildings().keySet())
                        .containsExactly(LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 4));
                assertThat(response.buildings().get(LocalDate.of(2026, 4, 3)))
                        .extracting(
                                ExerciseMapBuildingsDTO.BuildingInfo::buildingName,
                                ExerciseMapBuildingsDTO.BuildingInfo::streetAddr,
                                ExerciseMapBuildingsDTO.BuildingInfo::latitude,
                                ExerciseMapBuildingsDTO.BuildingInfo::longitude
                        )
                        .containsExactlyInAnyOrder(
                                tuple("A빌딩", "서울특별시 강남구 테헤란로 10", 37.501, 127.041),
                                tuple("B빌딩", "서울특별시 강남구 테헤란로 20", 37.502, 127.042)
                        );
                assertThat(response.buildings().get(LocalDate.of(2026, 4, 4)))
                        .extracting(
                                ExerciseMapBuildingsDTO.BuildingInfo::buildingName,
                                ExerciseMapBuildingsDTO.BuildingInfo::streetAddr
                        )
                        .containsExactly(tuple("A빌딩", "서울특별시 강남구 테헤란로 10"));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 멤버면 예외를 던진다")
            void 존재하지_않는_멤버면_예외를_던진다() {
                // given
                given(memberRepository.findMemberWithAddresses(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getExerciseMapCalendarSummary(
                        LocalDate.of(2026, 4, 1), null, null, radiusKm, 999L))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("대표주소가 없으면 예외를 던진다")
            void 대표주소가_없으면_예외를_던진다() {
                // given
                given(memberRepository.findMemberWithAddresses(memberWithoutMainAddr.getId()))
                        .willReturn(Optional.of(memberWithoutMainAddr));

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getExerciseMapCalendarSummary(
                        LocalDate.of(2026, 4, 1), null, null, radiusKm, memberWithoutMainAddr.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MAIN_ADDRESS_NULL);
            }

            @Test
            @DisplayName("대표주소가 없으면 명시 좌표가 있어도 예외를 던진다")
            void 대표주소가_없으면_명시_좌표가_있어도_예외를_던진다() {
                // given
                given(memberRepository.findMemberWithAddresses(memberWithoutMainAddr.getId()))
                        .willReturn(Optional.of(memberWithoutMainAddr));

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getExerciseMapCalendarSummary(
                        LocalDate.of(2026, 4, 1), 37.5, 127.0, radiusKm, memberWithoutMainAddr.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.MAIN_ADDRESS_NULL);
            }

            @Test
            @DisplayName("위도와 경도 중 하나만 주면 예외를 던진다")
            void 위도와_경도_중_하나만_주면_예외를_던진다() {
                // given
                given(memberRepository.findMemberWithAddresses(mapMember.getId()))
                        .willReturn(Optional.of(mapMember));

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getExerciseMapCalendarSummary(
                        LocalDate.of(2026, 4, 1), 37.5, null, radiusKm, mapMember.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INCOMPLETE_LOCATION_INFO);
            }
        }

        private Exercise createMapExercise(long id, LocalDate date, String buildingName,
                                           String streetAddr, double latitude, double longitude,
                                           LocalTime startTime) {
            Exercise mapExercise = ExerciseFixture.createExerciseWithAddr(party, date, 12);
            ReflectionTestUtils.setField(mapExercise, "id", id);
            ReflectionTestUtils.setField(mapExercise, "startTime", startTime);
            ReflectionTestUtils.setField(mapExercise, "exerciseAddr",
                    ExerciseFixture.createExerciseAddr(buildingName, streetAddr, latitude, longitude));
            return mapExercise;
        }
    }
}
