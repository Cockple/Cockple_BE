package umc.cockple.demo.domain.exercise.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseCancelDTO;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseParticipationIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
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
    private Party party;

    @BeforeEach
    void setUp() {
        manager = memberRepository.save(MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L, LocalDate.of(2000, 1, 1)));
        subManager = memberRepository.save(MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1002L, LocalDate.of(2000, 1, 1)));
        normalMember = memberRepository.save(MemberFixture.createMember("일반멤버", Gender.MALE, Level.C, 1003L, LocalDate.of(2000, 1, 1)));

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
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }

    @Nested
    @DisplayName("POST /api/exercises/{exerciseId}/participants - 운동 신청")
    class JoinExercise {

        private Exercise exercise;

        @BeforeEach
        void setUp() {
            party.addLevel(Gender.MALE, Level.A);
            party.addLevel(Gender.MALE, Level.B);
            party.addLevel(Gender.MALE, Level.C);
            party.addLevel(Gender.FEMALE, Level.A);
            party.addLevel(Gender.FEMALE, Level.B);
            party.addLevel(Gender.FEMALE, Level.C);
            partyRepository.save(party);

            exercise = exerciseRepository.save(
                    ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                            LocalTime.of(12, 0), true, false));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("201 - 파티 멤버가 운동 신청하면 참여 정보를 반환한다")
            void partyMember_joinExercise() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(post("/api/exercises/{exerciseId}/participants", exercise.getId()))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.participantId").isNumber())
                        .andExpect(jsonPath("$.data.joinedAt").isString())
                        .andExpect(jsonPath("$.data.currentParticipants").value(1));
            }

            @Test
            @DisplayName("201 - 파티 외부 멤버가 outsideGuestAccept=true 운동 신청하면 성공한다")
            void outsideMember_joinExercise() throws Exception {
                Exercise outsideAcceptExercise = exerciseRepository.save(
                        ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                                LocalTime.of(12, 0), true, true));

                Member outsideMember = memberRepository.save(
                        MemberFixture.createMember("외부멤버", Gender.FEMALE, Level.C, 2001L, LocalDate.of(2000, 1, 1)));

                SecurityContextHelper.setAuthentication(outsideMember.getId(), outsideMember.getNickname());

                mockMvc.perform(post("/api/exercises/{exerciseId}/participants", outsideAcceptExercise.getId()))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.participantId").isNumber())
                        .andExpect(jsonPath("$.data.currentParticipants").value(1));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 운동이면 에러를 반환한다")
            void exerciseNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(post("/api/exercises/{exerciseId}/participants", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - 존재하지 않는 멤버면 에러를 반환한다")
            void memberNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(post("/api/exercises/{exerciseId}/participants", exercise.getId()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("400 - 이미 시작된 운동이면 에러를 반환한다")
            void alreadyStarted() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                Exercise startedExercise = exerciseRepository.save(
                        ExerciseFixture.createExercise(party, LocalDate.of(2000, 1, 1),
                                LocalTime.of(12, 0), true, false));

                mockMvc.perform(post("/api/exercises/{exerciseId}/participants", startedExercise.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_PARTICIPATION.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_PARTICIPATION.getMessage()));
            }

            @Test
            @DisplayName("400 - 이미 참여 신청한 운동이면 에러를 반환한다")
            void alreadyJoined() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                memberExerciseRepository.save(
                        MemberFixture.createMemberExercise(normalMember, exercise));

                mockMvc.perform(post("/api/exercises/{exerciseId}/participants", exercise.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.ALREADY_JOINED_EXERCISE.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.ALREADY_JOINED_EXERCISE.getMessage()));
            }

            @Test
            @DisplayName("403 - 파티 멤버가 아닌데 외부 참여 불가 운동이면 에러를 반환한다")
            void notPartyMember_outsideNotAccepted() throws Exception {
                Member outsideMember = memberRepository.save(
                        MemberFixture.createMember("외부인", Gender.MALE, Level.B, 3001L, LocalDate.of(2000, 1, 1)));

                SecurityContextHelper.setAuthentication(outsideMember.getId(), outsideMember.getNickname());

                mockMvc.perform(post("/api/exercises/{exerciseId}/participants", exercise.getId()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.NOT_PARTY_MEMBER.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.NOT_PARTY_MEMBER.getMessage()));
            }

            @Test
            @DisplayName("403 - 나이 조건 불일치면 에러를 반환한다")
            void ageNotAllowed() throws Exception {
                Member youngMember = memberRepository.save(
                        MemberFixture.createMember("어린회원", Gender.MALE, Level.B, 4001L, LocalDate.of(2010, 1, 1)));
                memberPartyRepository.save(MemberFixture.createMemberParty(party, youngMember, Role.PARTY_MEMBER));

                SecurityContextHelper.setAuthentication(youngMember.getId(), youngMember.getNickname());

                mockMvc.perform(post("/api/exercises/{exerciseId}/participants", exercise.getId()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_AGE_NOT_ALLOWED.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_AGE_NOT_ALLOWED.getMessage()));
            }

        }
    }

    @Nested
    @DisplayName("DELETE /api/exercises/{exerciseId}/participants/my - 운동 참여 취소")
    class CancelParticipation {

        private Exercise exercise;

        @BeforeEach
        void setUp() {
            exercise = exerciseRepository.save(
                    ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                            LocalTime.of(12, 0), true, false));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 참여자가 본인 참여를 취소하면 memberName을 반환한다")
            void cancelParticipation_success() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                memberExerciseRepository.save(
                        MemberFixture.createMemberExercise(normalMember, exercise));

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/my", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberName").value(normalMember.getMemberName()))
                        .andExpect(jsonPath("$.data.currentParticipants").value(0));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 운동이면 에러를 반환한다")
            void exerciseNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/my", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - SecurityContext의 멤버가 DB에 없으면 에러를 반환한다")
            void memberNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/my", exercise.getId()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("400 - 이미 시작된 운동이면 에러를 반환한다")
            void alreadyStarted() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                Exercise startedExercise = exerciseRepository.save(
                        ExerciseFixture.createExercise(party, LocalDate.of(2000, 1, 1),
                                LocalTime.of(12, 0), true, false));

                memberExerciseRepository.save(
                        MemberFixture.createMemberExercise(normalMember, startedExercise));

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/my", startedExercise.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL.getMessage()));
            }

            @Test
            @DisplayName("404 - 참여 기록이 없으면 에러를 반환한다")
            void memberExerciseNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/my", exercise.getId()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_EXERCISE_NOT_FOUND.getMessage()));
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/exercises/{exerciseId}/participants/{participantId} - 특정 참여자 운동 취소")
    class CancelParticipationByManager {

        private Exercise exercise;

        @BeforeEach
        void setUp() {
            exercise = exerciseRepository.save(
                    ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31)));
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 모임장이 멤버 참여를 취소하면 memberName을 반환한다")
            void owner_cancelMemberParticipation() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                memberExerciseRepository.save(
                        MemberFixture.createMemberExercise(normalMember, exercise));

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/{participantId}",
                                exercise.getId(), normalMember.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberName").isString());
            }

            @Test
            @DisplayName("200 - 부모임장도 멤버 참여를 취소할 수 있다")
            void subManager_cancelMemberParticipation() throws Exception {
                SecurityContextHelper.setAuthentication(subManager.getId(), subManager.getNickname());

                memberExerciseRepository.save(
                        MemberFixture.createMemberExercise(normalMember, exercise));

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/{participantId}",
                                exercise.getId(), normalMember.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberName").isString());
            }

            @Test
            @DisplayName("200 - 모임장이 게스트 참여를 취소할 수 있다")
            void owner_cancelGuestParticipation() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                Guest guest = guestRepository.save(GuestFixture.createGuest(exercise, manager.getId()));

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(true);

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/{participantId}",
                                exercise.getId(), guest.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberName").value("게스트"));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 운동이면 에러를 반환한다")
            void exerciseNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/{participantId}",
                                999L, normalMember.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - SecurityContext의 멤버가 DB에 없으면 에러를 반환한다")
            void managerNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/{participantId}",
                                exercise.getId(), normalMember.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("403 - 일반 멤버가 취소 시 에러를 반환한다")
            void normalMember_forbidden() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/{participantId}",
                                exercise.getId(), normalMember.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INSUFFICIENT_PERMISSION.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INSUFFICIENT_PERMISSION.getMessage()));
            }

            @Test
            @DisplayName("400 - 이미 시작된 운동이면 에러를 반환한다")
            void alreadyStarted() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                Exercise startedExercise = exerciseRepository.save(
                        ExerciseFixture.createExercise(party, LocalDate.of(2000, 1, 1)));

                memberExerciseRepository.save(
                        MemberFixture.createMemberExercise(normalMember, startedExercise));

                ExerciseCancelDTO.ByManagerRequest request = new ExerciseCancelDTO.ByManagerRequest(false);

                mockMvc.perform(delete("/api/exercises/{exerciseId}/participants/{participantId}",
                                startedExercise.getId(), normalMember.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL.getMessage()));
            }
        }
    }

}
