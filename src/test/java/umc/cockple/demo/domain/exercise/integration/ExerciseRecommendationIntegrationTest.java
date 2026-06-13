package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.ExerciseCalendarTestHelper;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberAddrFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseRecommendationIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberAddrRepository memberAddrRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired ExerciseBookmarkRepository exerciseBookmarkRepository;

    // 조회 대상 회원 (모임 외부인, 추천 운동 수신 대상)
    private Member outsider;
    // 모임장 (모임 소속, 추천에서 제외)
    private Member manager;
    private Member subManager;
    private Party party;

    @BeforeEach
    void setUp() {
        // 추천 대상 회원: MALE, Level.A, 1995년생
        outsider = memberRepository.save(
                MemberFixture.createMember("외부회원", Gender.MALE, Level.A, 1001L, LocalDate.of(1995, 6, 15)));

        // 대표 주소 저장 (서울 강남구, lat=37.5, lon=127.0)
        memberAddrRepository.save(MemberAddrFixture.createMainAddr(outsider));

        // 모임장 (모임 소속)
        manager = memberRepository.save(
                MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1002L, LocalDate.of(1995, 1, 1)));

        subManager = memberRepository.save(
                MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1003L, LocalDate.of(1995, 1, 1)));

        // 모임 생성 (minBirthYear=1990, maxBirthYear=2005)
        PartyAddr partyAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(PartyFixture.createParty("테스트 모임", manager.getId(), partyAddr));

        // PartyLevel 추가: MALE A급 (cascade로 저장됨)
        party.addLevel(Gender.MALE, Level.A);
        partyRepository.save(party);

        // 모임장을 모임 멤버로 등록
        memberPartyRepository.save(MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER));
    }

    @AfterEach
    void tearDown() {
        guestRepository.deleteAll();
        exerciseBookmarkRepository.deleteAll();
        memberExerciseRepository.deleteAll();
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberAddrRepository.deleteAll();
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }

    @Nested
    @DisplayName("GET /api/exercises/recommendations - 사용자 추천 운동 조회")
    class GetRecommendedExercises {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("추천 운동이 존재하면 200 OK와 운동 목록을 반환한다")
            void 추천_운동이_존재하면_목록을_반환한다() throws Exception {
                // given
                exerciseRepository.save(ExerciseFixture.createRecommendableExercise(party,
                        LocalDate.now().plusDays(3), 37.5, 127.0, "테스트 체육관"));

                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                // when & then
                mockMvc.perform(get("/api/exercises/recommendations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalExercises").value(1))
                        .andExpect(jsonPath("$.data.exercises").isArray())
                        .andExpect(jsonPath("$.data.exercises[0].partyName").value("테스트 모임"))
                        .andExpect(jsonPath("$.data.exercises[0].buildingName").value("테스트 체육관"))
                        .andExpect(jsonPath("$.data.exercises[0].isBookmarked").value(false));
            }

            @Test
            @DisplayName("응답 필드가 모두 존재한다")
            void 응답_필드가_모두_존재한다() throws Exception {
                // given
                Exercise saved = exerciseRepository.save(ExerciseFixture.createRecommendableExercise(party,
                        LocalDate.now().plusDays(3), 37.5, 127.0, "필드확인 체육관"));

                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                // when & then
                mockMvc.perform(get("/api/exercises/recommendations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(saved.getId()))
                        .andExpect(jsonPath("$.data.exercises[0].partyId").value(party.getId()))
                        .andExpect(jsonPath("$.data.exercises[0].partyName").exists())
                        .andExpect(jsonPath("$.data.exercises[0].date").exists())
                        .andExpect(jsonPath("$.data.exercises[0].dayOfWeek").exists())
                        .andExpect(jsonPath("$.data.exercises[0].startTime").exists())
                        .andExpect(jsonPath("$.data.exercises[0].buildingName").exists())
                        .andExpect(jsonPath("$.data.exercises[0].isBookmarked").exists());
            }

            @Test
            @DisplayName("추천 운동이 없으면 빈 목록과 totalExercises 0을 반환한다")
            void 추천_운동이_없으면_빈_목록을_반환한다() throws Exception {
                // given - 운동 없음
                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                // when & then
                mockMvc.perform(get("/api/exercises/recommendations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalExercises").value(0))
                        .andExpect(jsonPath("$.data.exercises").isEmpty());
            }

            @Test
            @DisplayName("이미 소속된 모임의 운동은 추천되지 않는다")
            void 소속된_모임의_운동은_추천되지_않는다() throws Exception {
                // given - outsider를 모임에 가입시킴
                memberPartyRepository.save(
                        MemberFixture.createMemberParty(party, outsider, Role.PARTY_MEMBER));

                exerciseRepository.save(ExerciseFixture.createRecommendableExercise(party,
                        LocalDate.now().plusDays(3), 37.5, 127.0, "테스트 체육관"));

                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                // when & then - 소속 모임이므로 추천 목록에서 제외
                mockMvc.perform(get("/api/exercises/recommendations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalExercises").value(0));
            }

            @Test
            @DisplayName("outsideGuestAccept=false인 운동은 추천되지 않는다")
            void outsideGuestAccept_false_운동은_추천되지_않는다() throws Exception {
                // given - outsideGuestAccept=false 운동
                exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(3)));
                // createExerciseWithAddr는 outsideGuestAccept=false

                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                // when & then
                mockMvc.perform(get("/api/exercises/recommendations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalExercises").value(0));
            }

            @Test
            @DisplayName("이미 지난 운동은 추천되지 않는다")
            void 지난_운동은_추천되지_않는다() throws Exception {
                // given - 과거 날짜 운동
                exerciseRepository.save(ExerciseFixture.createRecommendableExercise(party,
                        LocalDate.now().minusDays(1), 37.5, 127.0, "과거 체육관"));

                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                // when & then
                mockMvc.perform(get("/api/exercises/recommendations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalExercises").value(0));
            }

            @Test
            @DisplayName("이미 참여한 운동은 추천되지 않는다")
            void 이미_참여한_운동은_추천되지_않는다() throws Exception {
                // given
                Exercise ex = exerciseRepository.save(ExerciseFixture.createRecommendableExercise(party,
                        LocalDate.now().plusDays(3), 37.5, 127.0, "참여완료 체육관"));
                memberExerciseRepository.save(MemberFixture.createMemberExercise(outsider, ex));

                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                // when & then
                mockMvc.perform(get("/api/exercises/recommendations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalExercises").value(0));
            }

            @Test
            @DisplayName("거리 가까운 순으로 정렬되어 반환된다")
            void 거리_가까운_순으로_정렬된다() throws Exception {
                // given
                // 가까운 운동 (강남, lat=37.5 lon=127.0 - outsider 대표주소와 동일)
                Exercise nearExercise = exerciseRepository.save(
                        ExerciseFixture.createRecommendableExercise(party,
                                LocalDate.now().plusDays(5), 37.5, 127.0, "가까운 체육관"));
                // 먼 운동 (부산 해운대, lat=35.1 lon=129.1)
                Exercise farExercise = exerciseRepository.save(
                        ExerciseFixture.createRecommendableExercise(party,
                                LocalDate.now().plusDays(1), 35.1, 129.1, "먼 체육관"));

                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                // when & then - 날짜가 늦어도 가까운 운동이 먼저
                mockMvc.perform(get("/api/exercises/recommendations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalExercises").value(2))
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(nearExercise.getId()))
                        .andExpect(jsonPath("$.data.exercises[1].exerciseId").value(farExercise.getId()));
            }

            @Test
            @DisplayName("최대 10개까지만 반환된다")
            void 최대_10개까지만_반환된다() throws Exception {
                // given - 12개 운동 저장
                for (int i = 1; i <= 12; i++) {
                    exerciseRepository.save(ExerciseFixture.createRecommendableExercise(party,
                            LocalDate.now().plusDays(i), 37.5, 127.0, "체육관" + i));
                }

                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                // when & then
                mockMvc.perform(get("/api/exercises/recommendations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalExercises").value(10))
                        .andExpect(jsonPath("$.data.exercises.length()").value(10));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("대표 주소가 없으면 400 에러를 반환한다")
            void 대표_주소가_없으면_400_에러를_반환한다() throws Exception {
                // given - 대표 주소 없는 회원
                Member noAddrMember = memberRepository.save(
                        MemberFixture.createMember("주소없는회원", Gender.MALE, Level.A, 9999L,
                                LocalDate.of(1995, 1, 1)));
                // MemberAddr 저장 안 함

                SecurityContextHelper.setAuthentication(noAddrMember.getId(), noAddrMember.getNickname());

                // when & then
                mockMvc.perform(get("/api/exercises/recommendations"))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/exercises/recommendations/calendar - 사용자 추천 운동 캘린더 조회")
    class GetRecommendedExerciseCalendar {

        private Member recommendationMember;
        private Member memberWithoutMainAddr;
        private Party filteredParty;
        private LocalDate startDate;
        private LocalDate endDate;
        private Exercise filteredEarlyExercise;
        private Exercise filteredPopularExercise;

        @BeforeEach
        void setUp() {
            recommendationMember = memberRepository.save(
                    MemberFixture.createMember("추천캘린더회원", Gender.MALE, Level.A, 1201L, LocalDate.of(1995, 6, 15)));
            memberAddrRepository.save(MemberAddrFixture.createMainAddr(recommendationMember));

            memberWithoutMainAddr = memberRepository.save(
                    MemberFixture.createMember("주소없는추천회원", Gender.MALE, Level.A, 1202L, LocalDate.of(1995, 6, 15)));


            PartyAddr filteredAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "송파구"));
            filteredParty = PartyFixture.createParty("필터 모임", manager.getId(), filteredAddr);
            ReflectionTestUtils.setField(filteredParty, "partyType", ParticipationType.SINGLE);
            ReflectionTestUtils.setField(filteredParty, "activityTime", ActivityTime.AFTERNOON);
            filteredParty = partyRepository.save(filteredParty);
            filteredParty.addLevel(Gender.MALE, Level.B);
            filteredParty = partyRepository.save(filteredParty);
            memberPartyRepository.save(MemberFixture.createMemberParty(filteredParty, manager, Role.PARTY_MANAGER));

            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 4, 5);

            filteredEarlyExercise = saveRecommendableExercise(filteredParty, LocalDate.of(2026, 3, 25),
                    37.51, 127.01, "필터 이른 체육관", LocalTime.of(9, 0), LocalTime.of(11, 0));
            filteredPopularExercise = saveRecommendableExercise(filteredParty, LocalDate.of(2026, 3, 25),
                    37.52, 127.02, "필터 인기 체육관", LocalTime.of(18, 0), LocalTime.of(20, 0));
            memberExerciseRepository.save(MemberFixture.createMemberExercise(manager, filteredPopularExercise));
            memberExerciseRepository.save(MemberFixture.createMemberExercise(subManager, filteredPopularExercise));
            exerciseBookmarkRepository.save(ExerciseBookmark.builder()
                    .member(recommendationMember)
                    .exercise(filteredPopularExercise)
                    .build());
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("기본 요청은 기본 기간의 콕플 추천 캘린더를 거리순으로 반환한다")
            void 기본_요청은_기본_기간의_콕플_추천_캘린더를_거리순으로_반환한다() throws Exception {
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate defaultExerciseDate = expectedStart.plusDays(9);
                int weekIndex = ExerciseCalendarTestHelper.weekIndexFor(expectedStart, defaultExerciseDate);
                int dayIndex = ExerciseCalendarTestHelper.dayIndexFor(defaultExerciseDate);

                Exercise nearExercise = saveRecommendableExercise(party, defaultExerciseDate,
                        37.5, 127.0, "가까운 체육관", LocalTime.of(11, 0), LocalTime.of(13, 0));
                Exercise farExercise = saveRecommendableExercise(party, defaultExerciseDate,
                        35.1, 129.1, "먼 체육관", LocalTime.of(9, 0), LocalTime.of(11, 0));

                SecurityContextHelper.setAuthentication(recommendationMember.getId(), recommendationMember.getNickname());

                mockMvc.perform(get("/api/exercises/recommendations/calendar"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value(expectedStart.toString()))
                        .andExpect(jsonPath("$.data.endDate").value(ExerciseCalendarTestHelper.expectedDefaultEndDate().toString()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].date").value(defaultExerciseDate.toString()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[0].exerciseId").value(nearExercise.getId()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[0].buildingName").value("가까운 체육관"))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[0].distance").value(0.0))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[1].exerciseId").value(farExercise.getId()));
            }

            @Test
            @DisplayName("필터 추천 최신순은 필터 조건에 맞는 운동만 시간순으로 반환한다")
            void 필터_추천_최신순은_필터_조건에_맞는_운동만_시간순으로_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(recommendationMember.getId(), recommendationMember.getNickname());

                mockMvc.perform(get("/api/exercises/recommendations/calendar")
                                .param("startDate", startDate.toString())
                                .param("endDate", endDate.toString())
                                .param("isCockpleRecommend", "false")
                                .param("levels", "B")
                                .param("participationTypes", "SINGLE")
                                .param("activityTimes", "AFTERNOON")
                                .param("sortType", "LATEST"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2026-03-23"))
                        .andExpect(jsonPath("$.data.endDate").value("2026-04-05"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].exerciseId").value(filteredEarlyExercise.getId()))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].partyId").value(filteredParty.getId()))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].partyName").value("필터 모임"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].isBookmarked").value(false))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].distance").value(nullValue()))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[1].exerciseId").value(filteredPopularExercise.getId()));
            }

            @Test
            @DisplayName("필터 추천 인기순은 참가자 수가 많은 운동을 먼저 반환한다")
            void 필터_추천_인기순은_참가자_수가_많은_운동을_먼저_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(recommendationMember.getId(), recommendationMember.getNickname());

                mockMvc.perform(get("/api/exercises/recommendations/calendar")
                                .param("startDate", startDate.toString())
                                .param("endDate", endDate.toString())
                                .param("isCockpleRecommend", "false")
                                .param("levels", "B")
                                .param("participationTypes", "SINGLE")
                                .param("activityTimes", "AFTERNOON")
                                .param("sortType", "POPULARITY"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].exerciseId").value(filteredPopularExercise.getId()))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].isBookmarked").value(true))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[1].exerciseId").value(filteredEarlyExercise.getId()));
            }

            @Test
            @DisplayName("추천 운동이 없으면 기간 메타데이터와 빈 일자별 캘린더를 반환한다")
            void 추천_운동이_없으면_기간_메타데이터와_빈_일자별_캘린더를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(recommendationMember.getId(), recommendationMember.getNickname());

                mockMvc.perform(get("/api/exercises/recommendations/calendar")
                                .param("startDate", "2030-01-05")
                                .param("endDate", "2030-01-11"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2030-01-05"))
                        .andExpect(jsonPath("$.data.endDate").value("2030-01-11"))
                        .andExpect(jsonPath("$.data.weeks[0].days[0].date").value("2029-12-31"))
                        .andExpect(jsonPath("$.data.weeks[0].days[5].date").value("2030-01-05"))
                        .andExpect(jsonPath("$.data.weeks[0].days[5].exercises").isEmpty());
            }

            @Test
            @DisplayName("startDate만 주어져도 기본 기간이 적용된다")
            void startDate만_주어져도_기본_기간이_적용된다() throws Exception {
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate defaultExerciseDate = expectedStart.plusDays(9);
                int weekIndex = ExerciseCalendarTestHelper.weekIndexFor(expectedStart, defaultExerciseDate);
                int dayIndex = ExerciseCalendarTestHelper.dayIndexFor(defaultExerciseDate);

                Exercise defaultExercise = saveRecommendableExercise(party, defaultExerciseDate,
                        37.5, 127.0, "기본기간 체육관", LocalTime.of(10, 0), LocalTime.of(12, 0));

                SecurityContextHelper.setAuthentication(recommendationMember.getId(), recommendationMember.getNickname());

                mockMvc.perform(get("/api/exercises/recommendations/calendar")
                                .param("startDate", "2026-03-25"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value(expectedStart.toString()))
                        .andExpect(jsonPath("$.data.endDate").value(ExerciseCalendarTestHelper.expectedDefaultEndDate().toString()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[0].exerciseId").value(defaultExercise.getId()));
            }

            @Test
            @DisplayName("종료일이 시작일보다 이전이어도 빈 캘린더를 반환한다")
            void 종료일이_시작일보다_이전이어도_빈_캘린더를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(recommendationMember.getId(), recommendationMember.getNickname());

                mockMvc.perform(get("/api/exercises/recommendations/calendar")
                                .param("startDate", "2026-04-05")
                                .param("endDate", "2026-03-23"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2026-04-05"))
                        .andExpect(jsonPath("$.data.endDate").value("2026-03-23"))
                        .andExpect(jsonPath("$.data.weeks").isEmpty());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 멤버면 에러를 반환한다")
            void 존재하지_않는_멤버면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(get("/api/exercises/recommendations/calendar")
                                .param("startDate", startDate.toString())
                                .param("endDate", endDate.toString()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("대표 주소가 없으면 에러를 반환한다")
            void 대표_주소가_없으면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(memberWithoutMainAddr.getId(), memberWithoutMainAddr.getNickname());

                mockMvc.perform(get("/api/exercises/recommendations/calendar")
                                .param("startDate", startDate.toString())
                                .param("endDate", endDate.toString()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MAIN_ADDRESS_NULL.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MAIN_ADDRESS_NULL.getMessage()));
            }

            @Test
            @DisplayName("날짜 형식이 잘못되면 400을 반환한다")
            void 날짜_형식이_잘못되면_400을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(recommendationMember.getId(), recommendationMember.getNickname());

                mockMvc.perform(get("/api/exercises/recommendations/calendar")
                                .param("startDate", "invalid-date")
                                .param("endDate", endDate.toString()))
                        .andExpect(status().isBadRequest());
            }
        }

        private Exercise saveRecommendableExercise(Party exerciseParty, LocalDate date,
                                                   double latitude, double longitude,
                                                   String buildingName, LocalTime startTime, LocalTime endTime) {
            Exercise exercise = ExerciseFixture.createRecommendableExercise(
                    exerciseParty, date, latitude, longitude, buildingName);
            ReflectionTestUtils.setField(exercise, "startTime", startTime);
            ReflectionTestUtils.setField(exercise, "endTime", endTime);
            return exerciseRepository.save(exercise);
        }
    }
}
