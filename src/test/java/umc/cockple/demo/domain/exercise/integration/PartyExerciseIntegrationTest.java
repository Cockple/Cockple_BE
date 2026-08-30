package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberAddrRepository;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
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
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PartyExerciseIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberAddrRepository memberAddrRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;
    @Autowired GuestRepository guestRepository;

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
    @DisplayName("GET /api/parties/{partyId}/exercises/calender - 모임 운동 캘린더 조회")
    class GetPartyExerciseCalendar {

        private Exercise exercise;
        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void setUp() {
            startDate = LocalDate.of(2026, 3, 23);
            endDate = LocalDate.of(2026, 3, 29);

            exercise = exerciseRepository.save(
                    ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 3, 24)));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("요청한 기간의 모임 운동 캘린더가 반환된다")
            void 요청한_기간의_모임_운동_캘린더가_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());
                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, exercise));

                mockMvc.perform(get("/api/parties/{partyId}/exercises/calender", party.getId())
                                .param("startDate", startDate.toString())
                                .param("endDate", endDate.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2026-03-23"))
                        .andExpect(jsonPath("$.data.endDate").value("2026-03-29"))
                        .andExpect(jsonPath("$.data.isMember").value(true))
                        .andExpect(jsonPath("$.data.partyName").value("테스트 모임"))
                        .andExpect(jsonPath("$.data.weeks[0].weekStartDate").value("2026-03-23"))
                        .andExpect(jsonPath("$.data.weeks[0].weekEndDate").value("2026-03-29"))
                        .andExpect(jsonPath("$.data.weeks[0].days[1].date").value("2026-03-24"))
                        .andExpect(jsonPath("$.data.weeks[0].days[1].dayOfWeek").value("TUESDAY"))
                        .andExpect(jsonPath("$.data.weeks[0].days[1].exercises[0].exerciseId").value(exercise.getId()))
                        .andExpect(jsonPath("$.data.weeks[0].days[1].exercises[0].isBookmarked").value(false))
                        .andExpect(jsonPath("$.data.weeks[0].days[1].exercises[0].buildingName").value("테스트 체육관"))
                        .andExpect(jsonPath("$.data.weeks[0].days[1].exercises[0].currentParticipants").value(1))
                        .andExpect(jsonPath("$.data.weeks[0].days[1].exercises[0].maxCapacity").value(10))
                        .andExpect(jsonPath("$.data.weeks[0].days[1].exercises[0].isParticipating").value(true));
            }

            @Test
            @DisplayName("기간 내 운동이 없으면 빈 캘린더를 반환한다")
            void 기간_내_운동이_없으면_빈_캘린더를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                mockMvc.perform(get("/api/parties/{partyId}/exercises/calender", party.getId())
                                .param("startDate", "2026-03-30")
                                .param("endDate", "2026-04-05"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value("2026-03-30"))
                        .andExpect(jsonPath("$.data.endDate").value("2026-04-05"))
                        .andExpect(jsonPath("$.data.isMember").value(false))
                        .andExpect(jsonPath("$.data.partyName").value("테스트 모임"))
                        .andExpect(jsonPath("$.data.weeks").isEmpty());
            }

            @Test
            @DisplayName("시작일과_종료일이_없으면_기본_기간이_적용된다")
            void 시작일과_종료일이_없으면_기본_기간이_적용된다() throws Exception {
                LocalDate expectedStart = ExerciseCalendarTestHelper.expectedDefaultStartDate();
                LocalDate defaultExerciseDate = expectedStart.plusDays(9);
                int weekIndex = ExerciseCalendarTestHelper.weekIndexFor(expectedStart, defaultExerciseDate);
                int dayIndex = ExerciseCalendarTestHelper.dayIndexFor(defaultExerciseDate);

                Exercise defaultExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, defaultExerciseDate));

                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());
                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, defaultExercise));

                mockMvc.perform(get("/api/parties/{partyId}/exercises/calender", party.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.startDate").value(expectedStart.toString()))
                        .andExpect(jsonPath("$.data.endDate").value(ExerciseCalendarTestHelper.expectedDefaultEndDate().toString()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].date").value(defaultExerciseDate.toString()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[0].exerciseId").value(defaultExercise.getId()))
                        .andExpect(jsonPath("$.data.weeks[" + weekIndex + "].days[" + dayIndex + "].exercises[0].isParticipating").value(true));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("시작일과 종료일이 함께 오지 않으면 에러를 반환한다")
            void 시작일과_종료일이_함께_오지_않으면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(get("/api/parties/{partyId}/exercises/calender", party.getId())
                                .param("startDate", startDate.toString()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INCOMPLETE_DATE_RANGE.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INCOMPLETE_DATE_RANGE.getMessage()));
            }
        }
    }

}
