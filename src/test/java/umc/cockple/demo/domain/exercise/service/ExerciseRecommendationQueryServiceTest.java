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
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseRecommendationCalendarResult;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseRecommendationResult;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.repository.support.ExerciseRecommendationSearchCondition;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.bookmark.service.query.lookup.ExerciseBookmarkLookupService;
import umc.cockple.demo.domain.exercise.service.support.calculator.ExerciseDistanceCalculator;
import umc.cockple.demo.domain.exercise.service.support.assembler.ExerciseRecommendationResultAssembler;
import umc.cockple.demo.domain.exercise.service.support.reader.ExerciseReader;
import umc.cockple.demo.domain.exercise.service.query.model.ExerciseRecommendationFilterCondition;
import umc.cockple.demo.domain.member.service.query.lookup.MemberLookupService;
import umc.cockple.demo.domain.file.service.FileService;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.member.exception.MemberException;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.domain.exercise.service.query.ExerciseRecommendationQueryService;
import umc.cockple.demo.domain.exercise.service.query.lookup.ExerciseParticipantCountLookupService;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.ExerciseCalendarTestHelper;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberAddrFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseRecommendationQueryService")
class ExerciseRecommendationQueryServiceTest {

    private ExerciseRecommendationQueryService exerciseRecommendationQueryService;

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberPartyRepository memberPartyRepository;
    @Mock private MemberExerciseRepository memberExerciseRepository;
    @Mock private ExerciseBookmarkRepository exerciseBookmarkRepository;
    @Mock private FileService fileService;

    private Member member;
    private MemberAddr mainAddr;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        ExerciseRecommendationResultAssembler exerciseRecommendationResultAssembler =
                new ExerciseRecommendationResultAssembler(
                        new ImageUrlResolver(fileService), new ExerciseDistanceCalculator());
        exerciseRecommendationQueryService =
                createExerciseRecommendationQueryService(exerciseRecommendationResultAssembler);

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

    private ExerciseRecommendationQueryService createExerciseRecommendationQueryService(
            ExerciseRecommendationResultAssembler exerciseRecommendationResultAssembler) {
        return new ExerciseRecommendationQueryService(
                new ExerciseReader(exerciseRepository),
                new ExerciseBookmarkLookupService(exerciseBookmarkRepository),
                new ExerciseParticipantCountLookupService(exerciseRepository),
                new ExerciseDistanceCalculator(),
                new MemberLookupService(memberRepository),
                exerciseRecommendationResultAssembler
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
                ExerciseRecommendationResult response =
                        exerciseRecommendationQueryService.getRecommendedExercises(member.getId());

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
                ExerciseRecommendationResult response =
                        exerciseRecommendationQueryService.getRecommendedExercises(member.getId());

                // then
                ExerciseRecommendationResult.ExerciseItem item = response.exercises().get(0);
                assertThat(item.exerciseId()).isEqualTo(100L);
                assertThat(item.partyId()).isEqualTo(10L);
                assertThat(item.partyName()).isEqualTo("테스트 모임");
                assertThat(item.date()).isEqualTo(exercise.getDate());
                assertThat(item.buildingName()).isEqualTo("테스트 체육관");
                assertThat(item.bookmarked()).isFalse();
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
                ExerciseRecommendationResult response =
                        exerciseRecommendationQueryService.getRecommendedExercises(member.getId());

                // then
                assertThat(response.exercises().get(0).bookmarked()).isTrue();
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
                ExerciseRecommendationResult response =
                        exerciseRecommendationQueryService.getRecommendedExercises(member.getId());

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
                ExerciseRecommendationResult response =
                        exerciseRecommendationQueryService.getRecommendedExercises(member.getId());

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
                ExerciseRecommendationResult response =
                        exerciseRecommendationQueryService.getRecommendedExercises(member.getId());

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
                assertThatThrownBy(() -> exerciseRecommendationQueryService.getRecommendedExercises(999L))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
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
                assertThatThrownBy(() -> exerciseRecommendationQueryService.getRecommendedExercises(2L))
                        .isInstanceOf(MemberException.class)
                        .satisfies(e -> assertThat(((MemberException) e).getCode())
                                .isEqualTo(MemberErrorCode.MAIN_ADDRESS_NULL));
            }
        }
    }
    @Nested
    @DisplayName("getRecommendedExerciseCalendar")
    class GetRecommendedExerciseCalendar {

        private Member recommendationMember;
        private Member memberWithoutMainAddr;
        private MemberAddr mainAddr;
        private Party filteredParty;
        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void setUp() {
            recommendationMember = MemberFixture.createMember("추천캘린더회원", Gender.MALE, Level.A, 11001L,
                    LocalDate.of(1995, 6, 15));
            ReflectionTestUtils.setField(recommendationMember, "id", 11L);
            mainAddr = MemberAddrFixture.createMainAddr(recommendationMember);
            ReflectionTestUtils.setField(recommendationMember, "addresses", List.of(mainAddr));

            memberWithoutMainAddr = MemberFixture.createMember("주소없는추천회원", Gender.MALE, Level.A, 11002L,
                    LocalDate.of(1995, 6, 15));
            ReflectionTestUtils.setField(memberWithoutMainAddr, "id", 12L);
            ReflectionTestUtils.setField(memberWithoutMainAddr, "addresses", List.of(MemberAddrFixture.createSubAddr(memberWithoutMainAddr)));

            party.addLevel(Gender.MALE, Level.A);

            filteredParty = PartyFixture.createParty("필터 모임", recommendationMember.getId(),
                    PartyFixture.createPartyAddr("서울특별시", "강남구"));
            ReflectionTestUtils.setField(filteredParty, "id", 20L);
            ReflectionTestUtils.setField(filteredParty, "partyType", ParticipationType.SINGLE);
            ReflectionTestUtils.setField(filteredParty, "activityTime", ActivityTime.AFTERNOON);
            filteredParty.addLevel(Gender.MALE, Level.B);

            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 4, 5);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("콕플 추천 기본 기간은 기본 범위를 사용하고 거리순으로 정렬한다")
            void 콕플_추천_기본_기간은_기본_범위를_사용하고_거리순으로_정렬한다() {
                // given
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate expectedEnd = ExerciseCalendarTestHelper.expectedDefaultEndDate();
                LocalDate targetDate = expectedStart.plusDays(9);
                int weekIndex = ExerciseCalendarTestHelper.weekIndexFor(expectedStart, targetDate);
                int dayIndex = ExerciseCalendarTestHelper.dayIndexFor(targetDate);

                Exercise nearExercise = createRecommendationExercise(party, 1001L, targetDate,
                        LocalTime.of(11, 0), LocalTime.of(13, 0), 37.5, 127.0, "가까운 체육관");
                Exercise farExercise = createRecommendationExercise(party, 1002L, targetDate,
                        LocalTime.of(9, 0), LocalTime.of(11, 0), 35.1, 129.1, "먼 체육관");

                given(memberRepository.findMemberWithAddresses(recommendationMember.getId()))
                        .willReturn(Optional.of(recommendationMember));
                given(exerciseRepository.findCockpleRecommendedExercisesByDateRange(
                        recommendationMember.getId(), Gender.MALE, Level.A, 1995, expectedStart, expectedEnd))
                        .willReturn(List.of(farExercise, nearExercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        recommendationMember.getId(), List.of(farExercise.getId(), nearExercise.getId())))
                        .willReturn(List.of(nearExercise.getId()));
                given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                        List.of(farExercise.getId(), nearExercise.getId())))
                        .willReturn(List.of());

                // when
                ExerciseRecommendationCalendarResult response = exerciseRecommendationQueryService.getRecommendedExerciseCalendar(
                        recommendationMember.getId(), null, null, true, recommendationFilter(), MyPartyExerciseOrderType.LATEST);

                // then
                assertThat(response.startDate()).isEqualTo(expectedStart);
                assertThat(response.endDate()).isEqualTo(expectedEnd);
                assertThat(response.weeks()).hasSize(5);
                assertThat(response.weeks().get(weekIndex).days().get(dayIndex).exercises())
                        .extracting(
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::exerciseId,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::partyId,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::partyName,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::buildingName,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::startTime,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::endTime,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::bookmarked
                        )
                        .containsExactly(
                                tuple(nearExercise.getId(), party.getId(), "테스트 모임", "가까운 체육관",
                                        LocalTime.of(11, 0), LocalTime.of(13, 0), true),
                                tuple(farExercise.getId(), party.getId(), "테스트 모임", "먼 체육관",
                                        LocalTime.of(9, 0), LocalTime.of(11, 0), false)
                        );
                assertThat(response.weeks().get(weekIndex).days().get(dayIndex).exercises().get(0).distance()).isZero();
                assertThat(response.weeks().get(weekIndex).days().get(dayIndex).exercises().get(1).distance()).isGreaterThan(0.0);
                verify(exerciseRepository).findCockpleRecommendedExercisesByDateRange(
                        recommendationMember.getId(), Gender.MALE, Level.A, 1995, expectedStart, expectedEnd);
                verify(exerciseRepository, never()).findFilteredRecommendedExercisesForCalendar(any(), any(), any(), any(), any());
            }

            @Test
            @DisplayName("필터 추천은 필터 리포지토리만 호출하고 인기순 정렬을 적용한다")
            void 필터_추천은_필터_리포지토리만_호출하고_인기순_정렬을_적용한다() {
                // given
                Exercise popularExercise = createRecommendationExercise(filteredParty, 1101L, LocalDate.of(2026, 3, 25),
                        LocalTime.of(18, 0), LocalTime.of(20, 0), 37.52, 127.02, "인기 체육관");
                Exercise earlyExercise = createRecommendationExercise(filteredParty, 1102L, LocalDate.of(2026, 3, 25),
                        LocalTime.of(9, 0), LocalTime.of(11, 0), 37.53, 127.03, "이른 체육관");

                ExerciseRecommendationFilterCondition filterCondition = ExerciseRecommendationFilterCondition.builder()
                        .addr1("서울특별시")
                        .addr2("강남구")
                        .levels(List.of(Level.B))
                        .participationTypes(List.of(ParticipationType.SINGLE))
                        .activityTimes(List.of(ActivityTime.AFTERNOON))
                        .build();

                ExerciseRecommendationSearchCondition expectedSearchCondition = new ExerciseRecommendationSearchCondition(
                        "서울특별시",
                        "강남구",
                        List.of(Level.B),
                        List.of(ParticipationType.SINGLE),
                        List.of(ActivityTime.AFTERNOON)
                );

                given(memberRepository.findMemberWithAddresses(recommendationMember.getId()))
                        .willReturn(Optional.of(recommendationMember));
                given(exerciseRepository.findFilteredRecommendedExercisesForCalendar(
                        recommendationMember.getId(), 1995, expectedSearchCondition, startDate, endDate))
                        .willReturn(List.of(earlyExercise, popularExercise));
                given(exerciseBookmarkRepository.findAllExerciseIdsByMemberIdAndExerciseIds(
                        recommendationMember.getId(), List.of(earlyExercise.getId(), popularExercise.getId())))
                        .willReturn(List.of(popularExercise.getId()));
                given(exerciseRepository.findExerciseParticipantCountsByExerciseIds(
                        List.of(earlyExercise.getId(), popularExercise.getId())))
                        .willReturn(List.of(
                                new Object[]{popularExercise.getId(), 3},
                                new Object[]{earlyExercise.getId(), 1}
                        ));

                // when
                ExerciseRecommendationCalendarResult response = exerciseRecommendationQueryService.getRecommendedExerciseCalendar(
                        recommendationMember.getId(), startDate, endDate, false, filterCondition, MyPartyExerciseOrderType.POPULARITY);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks().get(0).days().get(2).exercises())
                        .extracting(
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::exerciseId,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::partyId,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::partyName,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::buildingName,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::bookmarked,
                                ExerciseRecommendationCalendarResult.ExerciseCalendarItem::distance
                        )
                        .containsExactly(
                                tuple(popularExercise.getId(), filteredParty.getId(), "필터 모임", "인기 체육관", true, null),
                                tuple(earlyExercise.getId(), filteredParty.getId(), "필터 모임", "이른 체육관", false, null)
                        );
                verify(exerciseRepository, never()).findCockpleRecommendedExercisesByDateRange(any(), any(), any(), anyInt(), any(), any());
                verify(exerciseRepository).findFilteredRecommendedExercisesForCalendar(
                        recommendationMember.getId(), 1995, expectedSearchCondition, startDate, endDate);
            }

            @Test
            @DisplayName("추천 운동이 없으면 기간 메타데이터와 빈 일자별 캘린더를 반환한다")
            void 추천_운동이_없으면_기간_메타데이터와_빈_일자별_캘린더를_반환한다() {
                // given
                given(memberRepository.findMemberWithAddresses(recommendationMember.getId()))
                        .willReturn(Optional.of(recommendationMember));
                given(exerciseRepository.findCockpleRecommendedExercisesByDateRange(
                        recommendationMember.getId(), Gender.MALE, Level.A, 1995, startDate, endDate))
                        .willReturn(List.of());

                // when
                ExerciseRecommendationCalendarResult response = exerciseRecommendationQueryService.getRecommendedExerciseCalendar(
                        recommendationMember.getId(), startDate, endDate, true, recommendationFilter(), MyPartyExerciseOrderType.LATEST);

                // then
                assertThat(response.startDate()).isEqualTo(startDate);
                assertThat(response.endDate()).isEqualTo(endDate);
                assertThat(response.weeks()).hasSize(2);
                assertThat(response.weeks().get(0).days()).hasSize(7);
                assertThat(response.weeks().get(0).days().get(0).exercises()).isEmpty();
                verify(exerciseRepository, never()).findExerciseParticipantCountsByExerciseIds(any());
            }

            @Test
            @DisplayName("startDate만 주어져도 기본 기간이 적용된다")
            void startDate만_주어져도_기본_기간이_적용된다() {
                // given
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate expectedEnd = ExerciseCalendarTestHelper.expectedDefaultEndDate();

                given(memberRepository.findMemberWithAddresses(recommendationMember.getId()))
                        .willReturn(Optional.of(recommendationMember));
                given(exerciseRepository.findCockpleRecommendedExercisesByDateRange(
                        recommendationMember.getId(), Gender.MALE, Level.A, 1995, expectedStart, expectedEnd))
                        .willReturn(List.of());

                // when
                ExerciseRecommendationCalendarResult response = exerciseRecommendationQueryService.getRecommendedExerciseCalendar(
                        recommendationMember.getId(), LocalDate.of(2026, 3, 25), null, true,
                        recommendationFilter(), MyPartyExerciseOrderType.LATEST);

                // then
                assertThat(response.startDate()).isEqualTo(expectedStart);
                assertThat(response.endDate()).isEqualTo(expectedEnd);
                assertThat(response.weeks()).hasSize(5);
            }

            @Test
            @DisplayName("종료일이 시작일보다 이전이어도 빈 캘린더를 반환한다")
            void 종료일이_시작일보다_이전이어도_빈_캘린더를_반환한다() {
                // given
                LocalDate reversedStart = LocalDate.of(2026, 4, 5);
                LocalDate reversedEnd = LocalDate.of(2026, 3, 23);

                given(memberRepository.findMemberWithAddresses(recommendationMember.getId()))
                        .willReturn(Optional.of(recommendationMember));
                given(exerciseRepository.findCockpleRecommendedExercisesByDateRange(
                        recommendationMember.getId(), Gender.MALE, Level.A, 1995, reversedStart, reversedEnd))
                        .willReturn(List.of());

                // when
                ExerciseRecommendationCalendarResult response = exerciseRecommendationQueryService.getRecommendedExerciseCalendar(
                        recommendationMember.getId(), reversedStart, reversedEnd, true,
                        recommendationFilter(), MyPartyExerciseOrderType.LATEST);

                // then
                assertThat(response.startDate()).isEqualTo(reversedStart);
                assertThat(response.endDate()).isEqualTo(reversedEnd);
                assertThat(response.weeks()).isEmpty();
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
                assertThatThrownBy(() -> exerciseRecommendationQueryService.getRecommendedExerciseCalendar(
                        999L, startDate, endDate, true, recommendationFilter(), MyPartyExerciseOrderType.LATEST))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MEMBER_NOT_FOUND);
            }

            @Test
            @DisplayName("대표주소가 없으면 예외를 던진다")
            void 대표주소가_없으면_예외를_던진다() {
                // given
                given(memberRepository.findMemberWithAddresses(memberWithoutMainAddr.getId()))
                        .willReturn(Optional.of(memberWithoutMainAddr));
                given(exerciseRepository.findCockpleRecommendedExercisesByDateRange(
                        memberWithoutMainAddr.getId(), Gender.MALE, Level.A, 1995, startDate, endDate))
                        .willReturn(List.of());

                // when & then
                assertThatThrownBy(() -> exerciseRecommendationQueryService.getRecommendedExerciseCalendar(
                        memberWithoutMainAddr.getId(), startDate, endDate, true, recommendationFilter(), MyPartyExerciseOrderType.LATEST))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("code", MemberErrorCode.MAIN_ADDRESS_NULL);
            }
        }

        private ExerciseRecommendationFilterCondition recommendationFilter() {
            return ExerciseRecommendationFilterCondition.builder()
                    .build();
        }

        private Exercise createRecommendationExercise(Party exerciseParty, long id, LocalDate date,
                                                      LocalTime startTime, LocalTime endTime,
                                                      double latitude, double longitude, String buildingName) {
            Exercise recommendationExercise = ExerciseFixture.createRecommendableExercise(
                    exerciseParty, date, latitude, longitude, buildingName);
            ReflectionTestUtils.setField(recommendationExercise, "id", id);
            ReflectionTestUtils.setField(recommendationExercise, "startTime", startTime);
            ReflectionTestUtils.setField(recommendationExercise, "endTime", endTime);
            return recommendationExercise;
        }
    }
}
