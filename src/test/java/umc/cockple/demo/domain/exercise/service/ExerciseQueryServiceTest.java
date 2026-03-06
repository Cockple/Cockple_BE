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
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.image.service.ImageService;
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
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock private ImageService imageService;

    private ExerciseConverter exerciseConverter;

    private Member manager;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        exerciseConverter = new ExerciseConverter(imageService);
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
    @DisplayName("getExerciseDetail - 탈퇴 회원")
    class GetExerciseDetailWithdrawn {

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
    }
}
