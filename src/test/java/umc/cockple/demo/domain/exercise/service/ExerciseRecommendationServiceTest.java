package umc.cockple.demo.domain.exercise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.exercise.converter.ExerciseConverter;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.dto.ExerciseRecommendationDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.service.support.ExerciseBookmarkReader;
import umc.cockple.demo.domain.exercise.service.support.ExerciseDistanceCalculator;
import umc.cockple.demo.domain.exercise.service.support.ExerciseParticipantReader;
import umc.cockple.demo.domain.exercise.service.support.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.support.GuestReader;
import umc.cockple.demo.domain.member.service.support.MemberLookupService;
import umc.cockple.demo.domain.party.service.support.PartyLookupService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberAddrFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseQueryService - 사용자 추천 운동 조회")
class ExerciseRecommendationServiceTest {

    private ExerciseQueryService exerciseQueryService;

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private ExerciseBookmarkRepository exerciseBookmarkRepository;
    @Mock private FileService fileService;

    private Member member;
    private MemberAddr mainAddr;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        ExerciseConverter exerciseConverter = new ExerciseConverter(fileService);
        exerciseQueryService = createExerciseQueryService(exerciseConverter);

        member = MemberFixture.createMember("테스트회원", Gender.MALE, Level.A, 1001L, LocalDate.of(1995, 6, 15));
        ReflectionTestUtils.setField(member, "id", 1L);

        mainAddr = MemberAddrFixture.createMainAddr(member);
        List<MemberAddr> addresses = new ArrayList<>();
        addresses.add(mainAddr);
        ReflectionTestUtils.setField(member, "addresses", addresses);

        party = PartyFixture.createParty("테스트 모임", member.getId(),
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        ReflectionTestUtils.setField(party, "id", 10L);

        ExerciseAddr exerciseAddr = ExerciseFixture.createExerciseAddr();
        exercise = ExerciseFixture.createExercise(party, LocalDate.now().plusDays(3),
                null, true, true);
        ReflectionTestUtils.setField(exercise, "id", 100L);
        ReflectionTestUtils.setField(exercise, "exerciseAddr", exerciseAddr);
    }

    private ExerciseQueryService createExerciseQueryService(ExerciseConverter exerciseConverter) {
        return new ExerciseQueryService(
                new ExerciseReader(exerciseRepository),
                new GuestReader(guestRepository),
                new ExerciseParticipantReader(exerciseRepository, memberExerciseRepository, memberPartyRepository),
                new ExerciseBookmarkReader(exerciseBookmarkRepository),
                new ExerciseDistanceCalculator(),
                new MemberLookupService(memberRepository),
                new PartyLookupService(partyRepository),
                exerciseConverter
        );
    }

    @Nested
    @DisplayName("getRecommendedExercises")
    class GetRecommendedExercises {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("추천 운동이 존재하면 운동 목록과 총 개수를 반환한다")
            void 추천_운동이_존재하면_목록과_총개수를_반환한다() {
                // given
                given(memberRepository.findMemberWithAddresses(member.getId()))
                        .willReturn(Optional.of(member));
                given(exerciseRepository.findExercisesByMemberIdAndLevelAndBirthYear(
                        eq(member.getId()), eq(Gender.MALE), eq(Level.A), eq(1995)))
                        .willReturn(List.of(exercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        eq(member.getId()), anyList()))
                        .willReturn(List.of());

                // when
                ExerciseRecommendationDTO.Response response =
                        exerciseQueryService.getRecommendedExercises(member.getId());

                // then
                assertThat(response.totalExercises()).isEqualTo(1);
                assertThat(response.exercises()).hasSize(1);
            }

            @Test
            @DisplayName("추천 운동의 필드가 올바르게 매핑된다")
            void 추천_운동_필드가_올바르게_매핑된다() {
                // given
                given(memberRepository.findMemberWithAddresses(member.getId()))
                        .willReturn(Optional.of(member));
                given(exerciseRepository.findExercisesByMemberIdAndLevelAndBirthYear(
                        eq(member.getId()), eq(Gender.MALE), eq(Level.A), eq(1995)))
                        .willReturn(List.of(exercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        eq(member.getId()), anyList()))
                        .willReturn(List.of());

                // when
                ExerciseRecommendationDTO.Response response =
                        exerciseQueryService.getRecommendedExercises(member.getId());

                // then
                ExerciseRecommendationDTO.ExerciseItem item = response.exercises().get(0);
                assertThat(item.exerciseId()).isEqualTo(100L);
                assertThat(item.partyId()).isEqualTo(10L);
                assertThat(item.partyName()).isEqualTo("테스트 모임");
                assertThat(item.date()).isEqualTo(exercise.getDate());
                assertThat(item.dayOfWeek()).isEqualTo(exercise.getDate().getDayOfWeek().name());
                assertThat(item.buildingName()).isEqualTo("테스트 체육관");
                assertThat(item.isBookmarked()).isFalse();
            }

            @Test
            @DisplayName("찜한 운동은 isBookmarked가 true로 반환된다")
            void 찜한_운동은_isBookmarked가_true로_반환된다() {
                // given
                given(memberRepository.findMemberWithAddresses(member.getId()))
                        .willReturn(Optional.of(member));
                given(exerciseRepository.findExercisesByMemberIdAndLevelAndBirthYear(
                        eq(member.getId()), eq(Gender.MALE), eq(Level.A), eq(1995)))
                        .willReturn(List.of(exercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        eq(member.getId()), anyList()))
                        .willReturn(List.of(100L));

                // when
                ExerciseRecommendationDTO.Response response =
                        exerciseQueryService.getRecommendedExercises(member.getId());

                // then
                assertThat(response.exercises().get(0).isBookmarked()).isTrue();
            }

            @Test
            @DisplayName("추천 운동이 없으면 빈 목록과 totalExercises 0을 반환한다")
            void 추천_운동이_없으면_빈_목록을_반환한다() {
                // given
                given(memberRepository.findMemberWithAddresses(member.getId()))
                        .willReturn(Optional.of(member));
                given(exerciseRepository.findExercisesByMemberIdAndLevelAndBirthYear(
                        eq(member.getId()), eq(Gender.MALE), eq(Level.A), eq(1995)))
                        .willReturn(Collections.emptyList());

                // when
                ExerciseRecommendationDTO.Response response =
                        exerciseQueryService.getRecommendedExercises(member.getId());

                // then
                assertThat(response.totalExercises()).isEqualTo(0);
                assertThat(response.exercises()).isEmpty();
            }

            @Test
            @DisplayName("추천 운동이 10개를 초과하면 거리순으로 최대 10개만 반환된다")
            void 추천_운동이_10개_초과하면_거리순으로_10개만_반환된다() {
                // given - 같은 위치(거리 0)의 운동 12개 생성
                List<Exercise> candidates = new ArrayList<>();
                for (int i = 1; i <= 12; i++) {
                    Exercise ex = ExerciseFixture.createExercise(party, LocalDate.now().plusDays(i),
                            null, true, true);
                    ReflectionTestUtils.setField(ex, "id", (long) (100 + i));
                    ReflectionTestUtils.setField(ex, "exerciseAddr", ExerciseFixture.createExerciseAddr());
                    candidates.add(ex);
                }

                given(memberRepository.findMemberWithAddresses(member.getId()))
                        .willReturn(Optional.of(member));
                given(exerciseRepository.findExercisesByMemberIdAndLevelAndBirthYear(
                        eq(member.getId()), eq(Gender.MALE), eq(Level.A), eq(1995)))
                        .willReturn(candidates);
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        eq(member.getId()), anyList()))
                        .willReturn(List.of());

                // when
                ExerciseRecommendationDTO.Response response =
                        exerciseQueryService.getRecommendedExercises(member.getId());

                // then
                assertThat(response.totalExercises()).isEqualTo(10);
                assertThat(response.exercises()).hasSize(10);
            }

            @Test
            @DisplayName("거리가 가까운 운동이 먼저 정렬된다")
            void 거리가_가까운_운동이_먼저_정렬된다() {
                // given - 거리가 다른 두 운동 (좌표 차이로 구분)
                ExerciseAddr nearAddr = ExerciseAddr.builder()
                        .addr1("서울특별시").addr2("강남구")
                        .streetAddr("테헤란로 1").buildingName("가까운 체육관")
                        .latitude(37.5).longitude(127.0) // mainAddr과 동일 위치 -> 거리 0
                        .build();
                ExerciseAddr farAddr = ExerciseAddr.builder()
                        .addr1("부산광역시").addr2("해운대구")
                        .streetAddr("해운대로 1").buildingName("먼 체육관")
                        .latitude(35.1).longitude(129.1) // 부산 -> 거리 멀다
                        .build();

                Exercise nearExercise = ExerciseFixture.createExercise(party, LocalDate.now().plusDays(5),
                        null, true, true);
                ReflectionTestUtils.setField(nearExercise, "id", 101L);
                ReflectionTestUtils.setField(nearExercise, "exerciseAddr", nearAddr);

                Exercise farExercise = ExerciseFixture.createExercise(party, LocalDate.now().plusDays(1),
                        null, true, true);
                ReflectionTestUtils.setField(farExercise, "id", 102L);
                ReflectionTestUtils.setField(farExercise, "exerciseAddr", farAddr);

                given(memberRepository.findMemberWithAddresses(member.getId()))
                        .willReturn(Optional.of(member));
                given(exerciseRepository.findExercisesByMemberIdAndLevelAndBirthYear(
                        eq(member.getId()), eq(Gender.MALE), eq(Level.A), eq(1995)))
                        .willReturn(List.of(farExercise, nearExercise)); // 먼 것을 먼저 넣어도
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        eq(member.getId()), anyList()))
                        .willReturn(List.of());

                // when
                ExerciseRecommendationDTO.Response response =
                        exerciseQueryService.getRecommendedExercises(member.getId());

                // then - 가까운 운동이 먼저
                assertThat(response.exercises().get(0).exerciseId()).isEqualTo(101L);
                assertThat(response.exercises().get(1).exerciseId()).isEqualTo(102L);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
            void 존재하지_않는_회원이면_예외가_발생한다() {
                // given
                given(memberRepository.findMemberWithAddresses(999L))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getRecommendedExercises(999L))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.MEMBER_NOT_FOUND));
            }

            @Test
            @DisplayName("대표 주소가 없으면 MAIN_ADDRESS_NULL 예외가 발생한다")
            void 대표_주소가_없으면_예외가_발생한다() {
                // given - addresses 비어 있는 member
                Member memberWithoutAddr = MemberFixture.createMember("주소없는회원", Gender.MALE, Level.A, 2001L, LocalDate.of(1995, 1, 1));
                ReflectionTestUtils.setField(memberWithoutAddr, "id", 2L);
                ReflectionTestUtils.setField(memberWithoutAddr, "addresses", new ArrayList<>());

                given(memberRepository.findMemberWithAddresses(2L))
                        .willReturn(Optional.of(memberWithoutAddr));

                // when & then
                assertThatThrownBy(() -> exerciseQueryService.getRecommendedExercises(2L))
                        .isInstanceOf(ExerciseException.class)
                        .satisfies(e -> assertThat(((ExerciseException) e).getCode())
                                .isEqualTo(ExerciseErrorCode.MAIN_ADDRESS_NULL));
            }
        }
    }
}
