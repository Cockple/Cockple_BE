package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
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
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.GuestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalTime;

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
}
