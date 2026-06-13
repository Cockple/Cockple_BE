package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import umc.cockple.demo.domain.member.domain.MemberAddr;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.ExerciseCalendarTestHelper;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.GuestFixture;
import umc.cockple.demo.support.fixture.MemberAddrFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseMyIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberAddrRepository memberAddrRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired ExerciseBookmarkRepository exerciseBookmarkRepository;

    private Member manager;
    private Member subManager;
    private Member normalMember;
    private Member outsider;
    private Party party;

    @BeforeEach
    void setUp() {
        manager = memberRepository.save(MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L, LocalDate.of(2000, 1, 1)));
        subManager = memberRepository.save(MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1002L, LocalDate.of(2000, 1, 1)));
        normalMember = memberRepository.save(MemberFixture.createMember("일반멤버", Gender.MALE, Level.C, 1003L, LocalDate.of(2000, 1, 1)));
        outsider = memberRepository.save(MemberFixture.createMember("외부회원", Gender.FEMALE, Level.B, 1004L, LocalDate.of(2001, 1, 1)));

        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(PartyFixture.createParty("테스트 모임", manager.getId(), addr));

        memberPartyRepository.save(MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, subManager, Role.PARTY_SUBMANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, normalMember, Role.PARTY_MEMBER));
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
    @DisplayName("GET /api/exercises/my/calender - 내 운동 캘린더 조회")
    class GetMyExerciseCalendar {

        private Exercise exercise;
        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void setUp() {
            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 3, 29);

            exercise = exerciseRepository.save(
                    ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 3, 25)));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("요청한 기간의 내 운동 캘린더가 반환된다")
            void 요청한_기간의_내_운동_캘린더가_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());
                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, exercise));

                mockMvc.perform(get("/api/exercises/my/calender")
                                .param("startDate", startDate.toString())
                                .param("endDate", endDate.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2026-03-23"))
                        .andExpect(jsonPath("$.data.endDate").value("2026-03-29"))
                        .andExpect(jsonPath("$.data.weeks[0].weekStartDate").value("2026-03-23"))
                        .andExpect(jsonPath("$.data.weeks[0].weekEndDate").value("2026-03-29"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].date").value("2026-03-25"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].dayOfWeek").value("WEDNESDAY"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].exerciseId").value(exercise.getId()))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].partyId").value(party.getId()))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].partyName").value("테스트 모임"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].buildingName").value("테스트 체육관"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].startTime").value("10:00:00"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].endTime").value(nullValue()))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].profileImageUrl").value(nullValue()));
            }

            @Test
            @DisplayName("기간 내 참여 운동이 없으면 빈 캘린더를 반환한다")
            void 기간_내_참여_운동이_없으면_빈_캘린더를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                mockMvc.perform(get("/api/exercises/my/calender")
                                .param("startDate", "2026-03-30")
                                .param("endDate", "2026-04-05"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2026-03-30"))
                        .andExpect(jsonPath("$.data.endDate").value("2026-04-05"))
                        .andExpect(jsonPath("$.data.weeks").isEmpty());
            }

            @Test
            @DisplayName("시작일과_종료일이_없으면_기본_기간이_적용된다")
            void 시작일과_종료일이_없으면_기본_기간이_적용된다() throws Exception {
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate defaultExerciseDate = expectedStart.plusDays(8);
                int weekIndex = ExerciseCalendarTestHelper.weekIndexFor(expectedStart, defaultExerciseDate);
                int dayIndex = ExerciseCalendarTestHelper.dayIndexFor(defaultExerciseDate);

                Exercise defaultExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, defaultExerciseDate));

                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());
                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, defaultExercise));

                mockMvc.perform(get("/api/exercises/my/calender"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value(expectedStart.toString()))
                        .andExpect(jsonPath("$.data.endDate").value(ExerciseCalendarTestHelper.expectedDefaultEndDate().toString()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].date").value(defaultExerciseDate.toString()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[0].exerciseId").value(defaultExercise.getId()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[0].partyId").value(party.getId()));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("시작일과 종료일이 함께 오지 않으면 에러를 반환한다")
            void 시작일과_종료일이_함께_오지_않으면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(get("/api/exercises/my/calender")
                                .param("startDate", startDate.toString()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INCOMPLETE_DATE_RANGE.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INCOMPLETE_DATE_RANGE.getMessage()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/exercises/parties/my - 내 모임 운동 조회")
    class GetMyPartyExercise {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("시작한 운동은 제외하고 내가 속한 모임의 예정된 운동을 최대 6개까지 시간순으로 반환한다")
            void 시작한_운동은_제외하고_내가_속한_모임의_예정된_운동을_최대_6개까지_시간순으로_반환한다() throws Exception {
                PartyAddr otherAddr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "송파구"));
                Party otherParty = partyRepository.save(PartyFixture.createParty("다른 모임", outsider.getId(), otherAddr));

                Exercise pastExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().minusDays(1)));
                Exercise startedTodayExercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.now());
                ReflectionTestUtils.setField(startedTodayExercise, "startTime", LocalTime.now().minusMinutes(30));
                startedTodayExercise = exerciseRepository.save(startedTodayExercise);
                Exercise firstExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(1)));
                Exercise secondExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(2)));
                Exercise thirdExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(3)));
                Exercise fourthExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(4)));
                Exercise fifthExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(5)));
                Exercise sixthExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(6)));
                exerciseRepository.save(ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().plusDays(7)));
                exerciseRepository.save(ExerciseFixture.createExerciseWithAddr(otherParty, LocalDate.now().plusDays(1)));

                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/parties/my"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalExercises").value(6))
                        .andExpect(jsonPath("$.data.exercises.length()").value(6))
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(firstExercise.getId()))
                        .andExpect(jsonPath("$.data.exercises[1].exerciseId").value(secondExercise.getId()))
                        .andExpect(jsonPath("$.data.exercises[2].exerciseId").value(thirdExercise.getId()))
                        .andExpect(jsonPath("$.data.exercises[3].exerciseId").value(fourthExercise.getId()))
                        .andExpect(jsonPath("$.data.exercises[4].exerciseId").value(fifthExercise.getId()))
                        .andExpect(jsonPath("$.data.exercises[5].exerciseId").value(sixthExercise.getId()))
                        .andExpect(jsonPath("$.data.exercises[0].partyId").value(party.getId()))
                        .andExpect(jsonPath("$.data.exercises[0].partyName").value("테스트 모임"))
                        .andExpect(jsonPath("$.data.exercises[0].buildingName").value("테스트 체육관"))
                        .andExpect(jsonPath("$.data.exercises[0].profileImageUrl").value(nullValue()));
            }

            @Test
            @DisplayName("속한 모임이 없으면 빈 응답을 반환한다")
            void 속한_모임이_없으면_빈_응답을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                mockMvc.perform(get("/api/exercises/parties/my"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalExercises").value(0))
                        .andExpect(jsonPath("$.data.exercises").isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/exercises/parties/my/calendar - 내 모임 운동 캘린더 조회")
    class GetMyPartyExerciseCalendar {

        private Exercise exercise;
        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void setUp() {
            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 3, 29);

            exercise = exerciseRepository.save(
                    ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 3, 25)));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("요청한 기간의 내 모임 운동 캘린더가 반환된다")
            void 요청한_기간의_내_모임_운동_캘린더가_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/parties/my/calendar")
                                .param("startDate", startDate.toString())
                                .param("endDate", endDate.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2026-03-23"))
                        .andExpect(jsonPath("$.data.endDate").value("2026-03-29"))
                        .andExpect(jsonPath("$.data.weeks[0].weekStartDate").value("2026-03-23"))
                        .andExpect(jsonPath("$.data.weeks[0].weekEndDate").value("2026-03-29"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].date").value("2026-03-25"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].dayOfWeek").value("WEDNESDAY"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].exerciseId").value(exercise.getId()))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].partyId").value(party.getId()))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].partyName").value("테스트 모임"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].buildingName").value("테스트 체육관"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].isBookmarked").value(false))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].nowCapacity").value(0));
            }

            @Test
            @DisplayName("속한 모임이 없으면 빈 캘린더를 반환한다")
            void 속한_모임이_없으면_빈_캘린더를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                mockMvc.perform(get("/api/exercises/parties/my/calendar")
                                .param("startDate", startDate.toString())
                                .param("endDate", endDate.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2026-03-23"))
                        .andExpect(jsonPath("$.data.endDate").value("2026-03-29"))
                        .andExpect(jsonPath("$.data.weeks").isEmpty());
            }

            @Test
            @DisplayName("기간 내 내 모임 운동이 없으면 빈 캘린더를 반환한다")
            void 기간_내_내_모임_운동이_없으면_빈_캘린더를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/parties/my/calendar")
                                .param("startDate", "2026-03-30")
                                .param("endDate", "2026-04-05"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2026-03-30"))
                        .andExpect(jsonPath("$.data.endDate").value("2026-04-05"))
                        .andExpect(jsonPath("$.data.weeks").isEmpty());
            }

            @Test
            @DisplayName("시작일과_종료일이_없으면_기본_기간이_적용된다")
            void 시작일과_종료일이_없으면_기본_기간이_적용된다() throws Exception {
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate defaultExerciseDate = expectedStart.plusDays(8);
                int weekIndex = ExerciseCalendarTestHelper.weekIndexFor(expectedStart, defaultExerciseDate);
                int dayIndex = ExerciseCalendarTestHelper.dayIndexFor(defaultExerciseDate);

                Exercise defaultExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, defaultExerciseDate));

                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/parties/my/calendar"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value(expectedStart.toString()))
                        .andExpect(jsonPath("$.data.endDate").value(ExerciseCalendarTestHelper.expectedDefaultEndDate().toString()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].date").value(defaultExerciseDate.toString()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[0].exerciseId").value(defaultExercise.getId()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[0].partyId").value(party.getId()));
            }

            @Test
            @DisplayName("POPULARITY 정렬 옵션으로 조회 시 정상 반환된다")
            void POPULARITY_정렬_옵션으로_조회_시_정상_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/parties/my/calendar")
                                .param("orderType", "POPULARITY")
                                .param("startDate", startDate.toString())
                                .param("endDate", endDate.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2026-03-23"))
                        .andExpect(jsonPath("$.data.endDate").value("2026-03-29"))
                        .andExpect(jsonPath("$.data.weeks[0].days[2].exercises[0].exerciseId").value(exercise.getId()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/exercises/my - 내 참여 운동 조회")
    class GetMyExercises {

        private final List<Exercise> upcomingExercises = new ArrayList<>();
        private final List<Exercise> completedExercises = new ArrayList<>();

        @BeforeEach
        void setUp() {
            party.addLevel(Gender.FEMALE, Level.B);
            party.addLevel(Gender.MALE, Level.A);
            partyRepository.save(party);

            for (int day = 1; day <= 10; day++) {
                Exercise exercise = saveParticipatedExercise(LocalDate.of(2099, 1, day), LocalTime.of(10, 0), 10, true);
                upcomingExercises.add(exercise);
            }

            Exercise featuredUpcomingExercise = upcomingExercises.get(9);
            ReflectionTestUtils.setField(featuredUpcomingExercise, "startTime", LocalTime.of(7, 30));
            ReflectionTestUtils.setField(featuredUpcomingExercise, "endTime", LocalTime.of(9, 0));
            ReflectionTestUtils.setField(featuredUpcomingExercise, "maxCapacity", 20);
            ReflectionTestUtils.setField(featuredUpcomingExercise, "partyGuestAccept", false);
            featuredUpcomingExercise = exerciseRepository.save(featuredUpcomingExercise);
            upcomingExercises.set(9, featuredUpcomingExercise);

            memberExerciseRepository.save(MemberFixture.createMemberExercise(subManager, featuredUpcomingExercise));
            guestRepository.save(GuestFixture.createGuest(featuredUpcomingExercise, manager.getId()));
            exerciseBookmarkRepository.save(ExerciseBookmark.builder()
                    .member(normalMember)
                    .exercise(featuredUpcomingExercise)
                    .build());

            for (int day = 1; day <= 8; day++) {
                Exercise exercise = saveParticipatedExercise(LocalDate.of(2024, 1, day), LocalTime.of(10, 0), 10, true);
                completedExercises.add(exercise);
            }

            exerciseRepository.save(ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 2, 1)));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("파라미터 없이 호출하면 ALL 최신순 기본값과 15개 페이징이 적용된다")
            void 파라미터_없이_호출하면_ALL_최신순_기본값과_15개_페이징이_적용된다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/my"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalCount").value(15))
                        .andExpect(jsonPath("$.data.hasNext").value(true))
                        .andExpect(jsonPath("$.data.exercises.length()").value(15))
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(upcomingExercises.get(9).getId()))
                        .andExpect(jsonPath("$.data.exercises[0].partyId").value(party.getId()))
                        .andExpect(jsonPath("$.data.exercises[0].partyName").value("테스트 모임"))
                        .andExpect(jsonPath("$.data.exercises[0].isBookmarked").value(true))
                        .andExpect(jsonPath("$.data.exercises[0].date").value("2099-01-10"))
                        .andExpect(jsonPath("$.data.exercises[0].dayOfWeek").value("SATURDAY"))
                        .andExpect(jsonPath("$.data.exercises[0].buildingName").value("테스트 체육관"))
                        .andExpect(jsonPath("$.data.exercises[0].startTime").value("07:30:00"))
                        .andExpect(jsonPath("$.data.exercises[0].endTime").value("09:00:00"))
                        .andExpect(jsonPath("$.data.exercises[0].femaleLevel[0]").value("B조"))
                        .andExpect(jsonPath("$.data.exercises[0].maleLevel[0]").value("A조"))
                        .andExpect(jsonPath("$.data.exercises[0].currentParticipants").value(3))
                        .andExpect(jsonPath("$.data.exercises[0].maxCapacity").value(20))
                        .andExpect(jsonPath("$.data.exercises[0].isCompleted").value(false))
                        .andExpect(jsonPath("$.data.exercises[0].partyGuestInviteAccept").value(false))
                        .andExpect(jsonPath("$.data.exercises[14].exerciseId").value(completedExercises.get(3).getId()));
            }

            @Test
            @DisplayName("두 번째 페이지를 조회하면 남은 3개 운동과 hasNext false를 반환한다")
            void 두_번째_페이지를_조회하면_남은_3개_운동과_hasNext_false를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/my")
                                .param("page", "1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalCount").value(3))
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.exercises.length()").value(3))
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(completedExercises.get(2).getId()))
                        .andExpect(jsonPath("$.data.exercises[2].exerciseId").value(completedExercises.get(0).getId()));
            }

            @Test
            @DisplayName("UPCOMING 필터는 예정 운동만 최신순 기본정렬로 반환한다")
            void UPCOMING_필터는_예정_운동만_최신순_기본정렬로_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/my")
                                .param("filterType", "UPCOMING"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalCount").value(10))
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(upcomingExercises.get(0).getId()))
                        .andExpect(jsonPath("$.data.exercises[0].isCompleted").value(false))
                        .andExpect(jsonPath("$.data.exercises[9].exerciseId").value(upcomingExercises.get(9).getId()))
                        .andExpect(jsonPath("$.data.exercises[9].date").value("2099-01-10"));
            }

            @Test
            @DisplayName("UPCOMING 필터에 OLDEST 정렬을 주면 반대 순서로 반환한다")
            void UPCOMING_필터에_OLDEST_정렬을_주면_반대_순서로_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/my")
                                .param("filterType", "UPCOMING")
                                .param("orderType", "OLDEST"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalCount").value(10))
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(upcomingExercises.get(9).getId()))
                        .andExpect(jsonPath("$.data.exercises[9].exerciseId").value(upcomingExercises.get(0).getId()));
            }

            @Test
            @DisplayName("COMPLETED 필터는 완료 운동만 최신순 기본정렬로 반환한다")
            void COMPLETED_필터는_완료_운동만_최신순_기본정렬로_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/my")
                                .param("filterType", "COMPLETED"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalCount").value(8))
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(completedExercises.get(7).getId()))
                        .andExpect(jsonPath("$.data.exercises[0].isCompleted").value(true))
                        .andExpect(jsonPath("$.data.exercises[7].exerciseId").value(completedExercises.get(0).getId()));
            }

            @Test
            @DisplayName("COMPLETED 필터에 OLDEST 정렬을 주면 반대 순서로 반환한다")
            void COMPLETED_필터에_OLDEST_정렬을_주면_반대_순서로_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/my")
                                .param("filterType", "COMPLETED")
                                .param("orderType", "OLDEST"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalCount").value(8))
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(completedExercises.get(0).getId()))
                        .andExpect(jsonPath("$.data.exercises[7].exerciseId").value(completedExercises.get(7).getId()));
            }

            @Test
            @DisplayName("ALL 필터에 OLDEST 정렬을 주면 가장 오래된 완료 운동부터 반환한다")
            void ALL_필터에_OLDEST_정렬을_주면_가장_오래된_완료_운동부터_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/my")
                                .param("orderType", "OLDEST"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalCount").value(15))
                        .andExpect(jsonPath("$.data.hasNext").value(true))
                        .andExpect(jsonPath("$.data.exercises[0].exerciseId").value(completedExercises.get(0).getId()))
                        .andExpect(jsonPath("$.data.exercises[14].exerciseId").value(upcomingExercises.get(6).getId()));
            }

            @Test
            @DisplayName("참여한 운동이 없으면 빈 응답을 반환한다")
            void 참여한_운동이_없으면_빈_응답을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                mockMvc.perform(get("/api/exercises/my"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalCount").value(0))
                        .andExpect(jsonPath("$.data.hasNext").value(false))
                        .andExpect(jsonPath("$.data.exercises").isEmpty());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("잘못된 필터 타입이면 400을 반환한다")
            void 잘못된_필터_타입이면_400을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/my")
                                .param("filterType", "INVALID"))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("잘못된 정렬 타입이면 400을 반환한다")
            void 잘못된_정렬_타입이면_400을_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/my")
                                .param("orderType", "INVALID"))
                        .andExpect(status().isBadRequest());
            }
        }

        private Exercise saveParticipatedExercise(LocalDate date, LocalTime startTime,
                                                  int maxCapacity, boolean partyGuestAccept) {
            Exercise exercise = ExerciseFixture.createExerciseWithAddr(party, date, maxCapacity);
            ReflectionTestUtils.setField(exercise, "startTime", startTime);
            ReflectionTestUtils.setField(exercise, "partyGuestAccept", partyGuestAccept);

            Exercise savedExercise = exerciseRepository.save(exercise);
            memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, savedExercise));
            return savedExercise;
        }
    }

}
