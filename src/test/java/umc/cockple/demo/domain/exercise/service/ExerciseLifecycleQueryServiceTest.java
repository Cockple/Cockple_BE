package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.support.ExerciseParticipantInfoAssembler;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseParticipantReader;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.reader.GuestReader;
import umc.cockple.demo.domain.exercise.service.query.ExerciseLifecycleQueryService;
import umc.cockple.demo.domain.member.service.support.MemberLookupService;
import umc.cockple.demo.domain.party.service.support.PartyLookupService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseLifecycleQueryService")
class ExerciseLifecycleQueryServiceTest {

    private ExerciseLifecycleQueryService exerciseLifecycleQueryService;

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
        exerciseLifecycleQueryService = createExerciseLifecycleQueryService(exerciseConverter);

        manager = MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L);
        ReflectionTestUtils.setField(manager, "id", 1L);

        party = PartyFixture.createParty("테스트 모임", manager.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        exercise = ExerciseFixture.createExercise(party, LocalDate.now().minusDays(1));
        ReflectionTestUtils.setField(exercise, "id", 100L);

        ReflectionTestUtils.setField(exercise, "exerciseAddr", ExerciseFixture.createExerciseAddr());
    }

    private ExerciseLifecycleQueryService createExerciseLifecycleQueryService(ExerciseConverter exerciseConverter) {
        ExerciseParticipantReader exerciseParticipantReader = new ExerciseParticipantReader(
                memberExerciseRepository, memberPartyRepository);
        MemberLookupService memberLookupService = new MemberLookupService(memberRepository);

        return new ExerciseLifecycleQueryService(
                new ExerciseReader(exerciseRepository),
                exerciseParticipantReader,
                new ExerciseParticipantInfoAssembler(
                        exerciseParticipantReader,
                        new GuestReader(guestRepository),
                        memberLookupService,
                        exerciseConverter
                ),
                memberLookupService,
                new ExerciseValidator(memberPartyRepository, memberExerciseRepository),
                exerciseConverter
        );
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
                        party.getId(), manager.getId(), Role.PARTY_MANAGER))
                        .willReturn(true);

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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
                        party.getId(), subManager.getId(), Role.PARTY_MANAGER))
                        .willReturn(false);

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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
                        party.getId(), normalMember.getId(), Role.PARTY_MANAGER))
                        .willReturn(false);

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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
                        party.getId(), outsider.getId(), Role.PARTY_MANAGER))
                        .willReturn(false);

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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
                        party.getId(), manager.getId(), Role.PARTY_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(withdrawnMember.getId())))
                        .willReturn(List.of());

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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

                MemberParty memberParty = MemberFixture.createMemberParty(party, activeMember, Role.PARTY_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(memberExercise));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.PARTY_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(activeMember.getId())))
                        .willReturn(List.of(memberParty));

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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
                        party.getId(), manager.getId(), Role.PARTY_MANAGER))
                        .willReturn(true);
                given(memberRepository.findMemberNamesByIds(any()))
                        .willReturn(Map.of(manager.getId(), "모임장"));

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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

                MemberParty managerParty = MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER);
                MemberParty subManagerParty = MemberFixture.createMemberParty(party, subManager, Role.PARTY_SUBMANAGER);
                MemberParty memberParty = MemberFixture.createMemberParty(party, normalMember, Role.PARTY_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(managerExercise, subManagerExercise, normalMemberExercise, outsiderExercise));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of(guest));
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.PARTY_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(manager.getId(), subManager.getId(), normalMember.getId(), outsider.getId())))
                        .willReturn(List.of(managerParty, subManagerParty, memberParty));
                given(memberRepository.findMemberNamesByIds(any()))
                        .willReturn(Map.of(manager.getId(), "모임장"));

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
                        exercise.getId(), manager.getId());

                // then
                assertThat(response.participants().list())
                        .extracting(
                                ExerciseDetailDTO.ParticipantInfo::name,
                                ExerciseDetailDTO.ParticipantInfo::participantType,
                                ExerciseDetailDTO.ParticipantInfo::partyPosition)
                        .containsExactly(
                                tuple("모임장", "PARTY_MEMBER", "PARTY_MANAGER"),
                                tuple("부모임장", "PARTY_MEMBER", "PARTY_SUBMANAGER"),
                                tuple("일반멤버", "PARTY_MEMBER", "PARTY_MEMBER"),
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

                MemberParty firstParty = MemberFixture.createMemberParty(party, firstMember, Role.PARTY_MEMBER);
                MemberParty secondParty = MemberFixture.createMemberParty(party, secondMember, Role.PARTY_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(first, second));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.PARTY_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(firstMember.getId(), secondMember.getId())))
                        .willReturn(List.of(firstParty, secondParty));

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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
                        party.getId(), manager.getId(), Role.PARTY_MANAGER))
                        .willReturn(true);
                given(memberRepository.findMemberNamesByIds(any()))
                        .willReturn(Map.of(manager.getId(), "모임장"));

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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

                MemberParty firstParty = MemberFixture.createMemberParty(party, firstMember, Role.PARTY_MEMBER);
                MemberParty secondParty = MemberFixture.createMemberParty(party, secondMember, Role.PARTY_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(first, second));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.PARTY_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(firstMember.getId(), secondMember.getId())))
                        .willReturn(List.of(firstParty, secondParty));

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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

                MemberParty maleParty = MemberFixture.createMemberParty(party, maleMember, Role.PARTY_MEMBER);
                MemberParty femaleParty = MemberFixture.createMemberParty(party, femaleMember, Role.PARTY_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(first, second));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.PARTY_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(maleMember.getId(), femaleMember.getId())))
                        .willReturn(List.of(maleParty, femaleParty));

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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

                MemberParty maleParty = MemberFixture.createMemberParty(party, maleMember, Role.PARTY_MEMBER);
                MemberParty femaleParty = MemberFixture.createMemberParty(party, femaleMember, Role.PARTY_MEMBER);

                given(exerciseRepository.findExerciseWithBasicInfo(exercise.getId()))
                        .willReturn(Optional.of(exercise));
                given(memberRepository.findById(manager.getId()))
                        .willReturn(Optional.of(manager));
                given(memberExerciseRepository.findByExerciseIdWithMemberAndProfile(exercise.getId()))
                        .willReturn(List.of(maleExercise, femaleExercise));
                given(guestRepository.findByExerciseId(exercise.getId()))
                        .willReturn(List.of());
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), manager.getId(), Role.PARTY_MANAGER))
                        .willReturn(true);
                given(memberPartyRepository.findMemberRolesByPartyAndMembers(
                        party.getId(), List.of(maleMember.getId(), femaleMember.getId())))
                        .willReturn(List.of(maleParty, femaleParty));

                // when
                ExerciseDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseDetail(
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
                assertThatThrownBy(() -> exerciseLifecycleQueryService.getExerciseDetail(999L, manager.getId()))
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
                assertThatThrownBy(() -> exerciseLifecycleQueryService.getExerciseDetail(exercise.getId(), 999L))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
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
                ExerciseEditDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseForEdit(
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

            @Test
            @DisplayName("부모임장이면 수정용 상세 정보를 조회할 수 있다")
            void 부모임장이면_수정용_상세_정보를_조회할_수_있다() {
                // given
                Member subManager = MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 2003L);
                ReflectionTestUtils.setField(subManager, "id", 21L);

                Exercise exerciseForEdit = ExerciseFixture.createExerciseForEdit(party, LocalDate.of(2026, 3, 24));
                ReflectionTestUtils.setField(exerciseForEdit, "id", 101L);

                given(exerciseRepository.findExerciseWithBasicInfo(exerciseForEdit.getId()))
                        .willReturn(Optional.of(exerciseForEdit));
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), subManager.getId(), Role.PARTY_MANAGER))
                        .willReturn(false);
                given(memberPartyRepository.existsByPartyIdAndMemberIdAndRole(
                        party.getId(), subManager.getId(), Role.PARTY_SUBMANAGER))
                        .willReturn(true);

                // when
                ExerciseEditDetailDTO.Response response = exerciseLifecycleQueryService.getExerciseForEdit(
                        exerciseForEdit.getId(), subManager.getId());

                // then
                assertThat(response.date()).isEqualTo(LocalDate.of(2026, 3, 24));
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
                assertThatThrownBy(() -> exerciseLifecycleQueryService.getExerciseForEdit(999L, manager.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.EXERCISE_NOT_FOUND);
            }

            @Test
            @DisplayName("모임장이나 부모임장이 아니면 예외를 던진다")
            void 모임장이나_부모임장이_아니면_예외를_던진다() {
                // given
                Member normalMember = MemberFixture.createMember("일반멤버", Gender.MALE, Level.C, 2004L);
                ReflectionTestUtils.setField(normalMember, "id", 22L);

                Exercise exerciseForEdit = ExerciseFixture.createExerciseForEdit(party, LocalDate.of(2026, 3, 24));
                ReflectionTestUtils.setField(exerciseForEdit, "id", 101L);

                given(exerciseRepository.findExerciseWithBasicInfo(exerciseForEdit.getId()))
                        .willReturn(Optional.of(exerciseForEdit));

                // when & then
                assertThatThrownBy(() -> exerciseLifecycleQueryService.getExerciseForEdit(
                        exerciseForEdit.getId(), normalMember.getId()))
                        .isInstanceOf(ExerciseException.class)
                        .hasFieldOrPropertyWithValue("code", ExerciseErrorCode.INSUFFICIENT_PERMISSION);
            }
        }
    }
}
