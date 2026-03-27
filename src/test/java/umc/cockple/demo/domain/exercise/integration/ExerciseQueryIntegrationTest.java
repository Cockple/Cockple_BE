package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.bookmark.domain.ExerciseBookmark;
import umc.cockple.demo.domain.bookmark.repository.ExerciseBookmarkRepository;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.domain.Member;
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
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseQueryIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired ExerciseBookmarkRepository exerciseBookmarkRepository;
    @Autowired DataSource dataSource;

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

        memberPartyRepository.save(MemberFixture.createMemberParty(party, manager, Role.party_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, subManager, Role.party_SUBMANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, normalMember, Role.party_MEMBER));
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
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }

    @Nested
    @DisplayName("GET /api/exercises/{exerciseId} - 운동 상세 조회")
    class GetExerciseDetail {

        private Exercise exercise;

        @BeforeEach
        void setUp() {
            exercise = exerciseRepository.save(
                    ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().minusDays(1)));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("응답의 모든 주요 필드가 올바르게 반환된다")
            void 응답의_모든_주요_필드가_올바르게_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                Exercise smallExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().minusDays(1), 1));

                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, smallExercise));
                memberExerciseRepository.save(MemberFixture.createMemberExercise(subManager, smallExercise));

                mockMvc.perform(get("/api/exercises/{exerciseId}", smallExercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.isManager").value(true))
                        .andExpect(jsonPath("$.data.info.buildingName").value("테스트 체육관"))
                        .andExpect(jsonPath("$.data.info.location").value("서울특별시 강남구 테헤란로 1"))
                        .andExpect(jsonPath("$.data.participants.currentParticipantCount").value(1))
                        .andExpect(jsonPath("$.data.participants.totalCount").value(1))
                        .andExpect(jsonPath("$.data.participants.manCount").value(1))
                        .andExpect(jsonPath("$.data.participants.womenCount").value(0))
                        .andExpect(jsonPath("$.data.participants.list[0].participantNumber").value(1))
                        .andExpect(jsonPath("$.data.participants.list[0].name").value(normalMember.getMemberName()))
                        .andExpect(jsonPath("$.data.participants.list[0].gender").value("MALE"))
                        .andExpect(jsonPath("$.data.participants.list[0].level").isString())
                        .andExpect(jsonPath("$.data.participants.list[0].participantType").value("PARTY_MEMBER"))
                        .andExpect(jsonPath("$.data.participants.list[0].partyPosition").value("party_MEMBER"))
                        .andExpect(jsonPath("$.data.participants.list[0].isWithdrawn").value(false))
                        .andExpect(jsonPath("$.data.waiting.currentWaitingCount").value(1))
                        .andExpect(jsonPath("$.data.waiting.manCount").value(0))
                        .andExpect(jsonPath("$.data.waiting.womenCount").value(1))
                        .andExpect(jsonPath("$.data.waiting.list[0].name").value(subManager.getMemberName()))
                        .andExpect(jsonPath("$.data.waiting.list[0].gender").value("FEMALE"))
                        .andExpect(jsonPath("$.data.waiting.list[0].participantType").value("PARTY_MEMBER"))
                        .andExpect(jsonPath("$.data.waiting.list[0].partyPosition").value("party_SUBMANAGER"))
                        .andExpect(jsonPath("$.data.waiting.list[0].isWithdrawn").value(false));
            }

            @Test
            @DisplayName("모임장이 조회하면 isManager true로 반환된다")
            void 모임장이_조회하면_isManager_true로_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.isManager").value(true));
            }

            @Test
            @DisplayName("일반 멤버가 조회하면 isManager false로 반환된다")
            void 일반_멤버가_조회하면_isManager_false로_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.isManager").value(false));
            }

            @Test
            @DisplayName("부모임장이 조회해도 isManager false로 반환된다")
            void 부모임장이_조회해도_isManager_false로_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(subManager.getId(), subManager.getNickname());

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.isManager").value(false));
            }

            @Test
            @DisplayName("모임 외부 회원이 조회해도 isManager false로 반환된다")
            void 모임_외부_회원이_조회해도_isManager_false로_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(outsider.getId(), outsider.getNickname());

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.isManager").value(false))
                        .andExpect(jsonPath("$.data.info.buildingName").value("테스트 체육관"));
            }

            @Test
            @DisplayName("정원 초과 참가자는 대기자로 반환된다")
            void 정원_초과_참가자는_대기자로_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                Exercise smallExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().minusDays(1), 1));

                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, smallExercise));
                memberExerciseRepository.save(MemberFixture.createMemberExercise(subManager, smallExercise));

                mockMvc.perform(get("/api/exercises/{exerciseId}", smallExercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.participants.currentParticipantCount").value(1))
                        .andExpect(jsonPath("$.data.waiting.currentWaitingCount").value(1));
            }

            @Test
            @DisplayName("먼저 가입한 참가자가 더 낮은 participantNumber를 받는다")
            void 먼저_가입한_참가자가_더_낮은_participantNumber를_받는다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, exercise));
                memberExerciseRepository.save(MemberFixture.createMemberExercise(subManager, exercise));

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.participants.currentParticipantCount").value(2))
                        .andExpect(jsonPath("$.data.participants.list[0].participantNumber").value(1))
                        .andExpect(jsonPath("$.data.participants.list[1].participantNumber").value(2))
                        .andExpect(jsonPath("$.data.participants.list[0].name").value(normalMember.getMemberName()))
                        .andExpect(jsonPath("$.data.participants.list[1].name").value(subManager.getMemberName()));
            }

            @Test
            @DisplayName("참가자의 성별 카운트가 올바르게 반환된다")
            void 참가자의_성별_카운트가_올바르게_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, exercise));
                memberExerciseRepository.save(MemberFixture.createMemberExercise(subManager, exercise));

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.participants.currentParticipantCount").value(2))
                        .andExpect(jsonPath("$.data.participants.manCount").value(1))
                        .andExpect(jsonPath("$.data.participants.womenCount").value(1));
            }

            @Test
            @DisplayName("대기자의 성별 카운트가 올바르게 반환된다")
            void 대기자의_성별_카운트가_올바르게_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                Exercise smallExercise = exerciseRepository.save(
                        ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().minusDays(1), 1));

                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, smallExercise));
                memberExerciseRepository.save(MemberFixture.createMemberExercise(subManager, smallExercise));

                mockMvc.perform(get("/api/exercises/{exerciseId}", smallExercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.participants.currentParticipantCount").value(1))
                        .andExpect(jsonPath("$.data.participants.manCount").value(1))
                        .andExpect(jsonPath("$.data.participants.womenCount").value(0))
                        .andExpect(jsonPath("$.data.waiting.currentWaitingCount").value(1))
                        .andExpect(jsonPath("$.data.waiting.manCount").value(0))
                        .andExpect(jsonPath("$.data.waiting.womenCount").value(1));
            }

            @Test
            @DisplayName("참가자 유형별 partyPosition이 올바르게 반환된다")
            void 참가자_유형별_partyPosition이_올바르게_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                memberExerciseRepository.save(MemberFixture.createMemberExercise(manager, exercise));
                memberExerciseRepository.save(MemberFixture.createMemberExercise(subManager, exercise));
                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, exercise));
                memberExerciseRepository.save(MemberFixture.createExternalMemberExercise(outsider, exercise));
                guestRepository.save(GuestFixture.createGuest(exercise, manager.getId()));

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.participants.list[0].name").value(manager.getMemberName()))
                        .andExpect(jsonPath("$.data.participants.list[0].participantType").value("PARTY_MEMBER"))
                        .andExpect(jsonPath("$.data.participants.list[0].partyPosition").value("party_MANAGER"))
                        .andExpect(jsonPath("$.data.participants.list[1].name").value(subManager.getMemberName()))
                        .andExpect(jsonPath("$.data.participants.list[1].participantType").value("PARTY_MEMBER"))
                        .andExpect(jsonPath("$.data.participants.list[1].partyPosition").value("party_SUBMANAGER"))
                        .andExpect(jsonPath("$.data.participants.list[2].name").value(normalMember.getMemberName()))
                        .andExpect(jsonPath("$.data.participants.list[2].participantType").value("PARTY_MEMBER"))
                        .andExpect(jsonPath("$.data.participants.list[2].partyPosition").value("party_MEMBER"))
                        .andExpect(jsonPath("$.data.participants.list[3].name").value(outsider.getMemberName()))
                        .andExpect(jsonPath("$.data.participants.list[3].participantType").value("EXTERNAL_PARTICIPANT"))
                        .andExpect(jsonPath("$.data.participants.list[3].partyPosition").value(nullValue()))
                        .andExpect(jsonPath("$.data.participants.list[4].name").value("게스트"))
                        .andExpect(jsonPath("$.data.participants.list[4].participantType").value("GUEST"))
                        .andExpect(jsonPath("$.data.participants.list[4].partyPosition").value(nullValue()));
            }

            @Test
            @DisplayName("게스트 참가자의 inviterName이 반환된다")
            void 게스트_참가자의_inviterName이_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                guestRepository.save(GuestFixture.createGuest(exercise, manager.getId()));

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.participants.list[0].participantType").value("GUEST"))
                        .andExpect(jsonPath("$.data.participants.list[0].inviterName").value(manager.getMemberName()));
            }

            @Test
            @DisplayName("활성 회원 참가자는 isWithdrawn false로 반환된다")
            void 활성_회원_참가자는_isWithdrawn_false로_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                memberExerciseRepository.save(MemberFixture.createMemberExercise(normalMember, exercise));

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.participants.list[0].isWithdrawn").value(false));
            }

            @Test
            @DisplayName("탈퇴 회원 참가자는 isWithdrawn true로 반환된다")
            void 탈퇴_회원_참가자는_isWithdrawn_true로_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                Member withdrawnMember = memberRepository.save(
                        MemberFixture.createWithdrawnMember("탈퇴회원", "탈퇴닉네임", 8888L));

                memberExerciseRepository.save(MemberFixture.createMemberExercise(withdrawnMember, exercise));

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.participants.list[0].isWithdrawn").value(true));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 운동이면 에러를 반환한다")
            void 존재하지_않는_운동이면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(get("/api/exercises/{exerciseId}", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("존재하지 않는 멤버면 에러를 반환한다")
            void 존재하지_않는_멤버면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(get("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/exercises/{exerciseId}/for-edit - 운동 수정용 상세 조회")
    class GetExerciseForEdit {

        private Exercise exercise;

        @BeforeEach
        void setUp() {
            Exercise exerciseForEdit = ExerciseFixture.createExerciseWithAddr(
                    party, LocalDate.of(2026, 3, 24), 18);
            ReflectionTestUtils.setField(exerciseForEdit, "endTime", LocalTime.of(12, 30));
            ReflectionTestUtils.setField(exerciseForEdit, "notice", "수정 공지사항");
            exercise = exerciseRepository.save(exerciseForEdit);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("응답의 모든 수정용 필드가 올바르게 반환된다")
            void 응답의_모든_수정용_필드가_올바르게_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(get("/api/exercises/{exerciseId}/for-edit", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.date").value("2026-03-24"))
                        .andExpect(jsonPath("$.data.buildingName").value("테스트 체육관"))
                        .andExpect(jsonPath("$.data.roadAddress").value("서울특별시 강남구 테헤란로 1"))
                        .andExpect(jsonPath("$.data.latitude").value(37.5))
                        .andExpect(jsonPath("$.data.longitude").value(127.0))
                        .andExpect(jsonPath("$.data.startTime").value("10:00:00"))
                        .andExpect(jsonPath("$.data.endTime").value("12:30:00"))
                        .andExpect(jsonPath("$.data.maxCapacity").value(18))
                        .andExpect(jsonPath("$.data.allowMemberGuestsInvitation").value(true))
                        .andExpect(jsonPath("$.data.allowExternalGuests").value(false))
                        .andExpect(jsonPath("$.data.notice").value("수정 공지사항"));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 운동이면 에러를 반환한다")
            void 존재하지_않는_운동이면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(get("/api/exercises/{exerciseId}/for-edit", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/exercises/{exerciseId}/guests - 내가 초대한 운동 게스트 조회")
    class GetMyInvitedGuests {

        private Exercise exercise;

        @BeforeEach
        void setUp() {
            exercise = exerciseRepository.save(
                    ExerciseFixture.createExerciseWithAddr(party, LocalDate.now().minusDays(1), 1));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("내가_초대한_게스트만_참가번호와_대기상태와_함께_반환된다")
            void 내가_초대한_게스트만_참가번호와_대기상태와_함께_반환된다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                var myFirstGuest = GuestFixture.createGuest(exercise, manager.getId(), "내게스트1", Gender.MALE);
                guestRepository.save(myFirstGuest);

                var otherGuest = GuestFixture.createGuest(exercise, normalMember.getId(), "다른사람게스트", Gender.MALE);
                guestRepository.save(otherGuest);

                var mySecondGuest = GuestFixture.createGuest(exercise, manager.getId(), "내게스트2", Gender.FEMALE);
                guestRepository.save(mySecondGuest);

                mockMvc.perform(get("/api/exercises/{exerciseId}/guests", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalCount").value(2))
                        .andExpect(jsonPath("$.data.maleCount").value(1))
                        .andExpect(jsonPath("$.data.femaleCount").value(1))
                        .andExpect(jsonPath("$.data.list[0].guestId").isNumber())
                        .andExpect(jsonPath("$.data.list[0].isWaiting").value(false))
                        .andExpect(jsonPath("$.data.list[0].participantNumber").value(1))
                        .andExpect(jsonPath("$.data.list[0].name").value("내게스트1"))
                        .andExpect(jsonPath("$.data.list[0].gender").value("MALE"))
                        .andExpect(jsonPath("$.data.list[0].level").value("B"))
                        .andExpect(jsonPath("$.data.list[0].inviterName").value(manager.getMemberName()))
                        .andExpect(jsonPath("$.data.list[1].guestId").isNumber())
                        .andExpect(jsonPath("$.data.list[1].isWaiting").value(true))
                        .andExpect(jsonPath("$.data.list[1].participantNumber").value(2))
                        .andExpect(jsonPath("$.data.list[1].name").value("내게스트2"))
                        .andExpect(jsonPath("$.data.list[1].gender").value("FEMALE"))
                        .andExpect(jsonPath("$.data.list[1].level").value("B"))
                        .andExpect(jsonPath("$.data.list[1].inviterName").value(manager.getMemberName()));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_운동이면_에러를_반환한다")
            void 존재하지_않는_운동이면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(get("/api/exercises/{exerciseId}/guests", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("존재하지_않는_멤버면_에러를_반환한다")
            void 존재하지_않는_멤버면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(get("/api/exercises/{exerciseId}/guests", exercise.getId()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }
        }
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

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 멤버면 에러를 반환한다")
            void 존재하지_않는_멤버면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(get("/api/exercises/parties/my"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
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

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 멤버면 에러를 반환한다")
            void 존재하지_않는_멤버면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(get("/api/exercises/parties/my/calendar")
                                .param("startDate", startDate.toString())
                                .param("endDate", endDate.toString()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
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
            @DisplayName("존재하지 않는 멤버면 에러를 반환한다")
            void 존재하지_않는_멤버면_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(get("/api/exercises/my"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }

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
