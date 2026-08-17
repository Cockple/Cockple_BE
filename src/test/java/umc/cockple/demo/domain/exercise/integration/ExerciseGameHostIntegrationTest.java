package umc.cockple.demo.domain.exercise.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.presentation.dto.gamehost.ExerciseGameHostDTO;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseGameHostIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;

    private Member manager;
    private Member subManager;
    private Member normalMember;
    private Member bannedMember;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        Member managerFixture = MemberFixture.createMemberWithName(
                "김모임장", "모임장 닉네임", Gender.FEMALE, Level.D, 2001L);
        managerFixture.updateProfileImg(
                ProfileImg.builder().imgKey("profiles/manager.jpg").build());
        manager = memberRepository.save(managerFixture);
        subManager = memberRepository.save(MemberFixture.createMemberWithName(
                "이부모임장", "부모임장 닉네임", Gender.MALE, Level.EXPERT, 2002L));
        normalMember = memberRepository.save(MemberFixture.createMemberWithName(
                "박일반", "일반 멤버 닉네임", Gender.FEMALE, Level.BEGINNER, 2003L));
        bannedMember = memberRepository.save(MemberFixture.createMemberWithName(
                "최강퇴", "강퇴 멤버 닉네임", Gender.MALE, Level.C, 2004L));

        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(
                PartyFixture.createParty("게임 진행자 테스트 모임", manager.getId(), partyAddr));

        memberPartyRepository.save(memberParty(
                manager, Role.PARTY_MANAGER, MemberPartyStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 4, 10, 0)));
        memberPartyRepository.save(memberParty(
                subManager, Role.PARTY_SUBMANAGER, MemberPartyStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 3, 10, 0)));
        memberPartyRepository.save(memberParty(
                normalMember, Role.PARTY_MEMBER, MemberPartyStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 10, 0)));
        memberPartyRepository.save(memberParty(
                bannedMember, Role.PARTY_MEMBER, MemberPartyStatus.BANNED,
                LocalDateTime.of(2025, 12, 31, 10, 0)));

        exercise = ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31));
        ReflectionTestUtils.setField(exercise, "gameHostId", normalMember.getId());
        exercise = exerciseRepository.save(exercise);

        Exercise managerExercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 1, 12)));
        Exercise subManagerExercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2026, 1, 10)));
        memberExerciseRepository.save(MemberFixture.createMemberExercise(manager, managerExercise));
        memberExerciseRepository.save(MemberFixture.createMemberExercise(subManager, subManagerExercise));
    }

    @AfterEach
    void tearDown() {
        memberExerciseRepository.deleteAll();
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }

    @Nested
    @DisplayName("GET /api/exercises/{exerciseId}/game-host")
    class GetGameHost {

        @Test
        @DisplayName("200 - 활성 모임원과 현재 게임 진행자 정보를 공개 계약대로 반환한다")
        void managerGetsGameHostCandidates() throws Exception {
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            mockMvc.perform(get("/api/exercises/{exerciseId}/game-host", exercise.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(CommonSuccessCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.totalCount").value(3))
                    .andExpect(jsonPath("$.data.participants.length()").value(3))
                    .andExpect(jsonPath("$.data.participants[0].participantId").value(manager.getId()))
                    .andExpect(jsonPath("$.data.participants[0].profileImageUrl")
                            .value("https://storage.googleapis.com/test-bucket/profiles/manager.jpg"))
                    .andExpect(jsonPath("$.data.participants[0].partyPosition").value("모임장"))
                    .andExpect(jsonPath("$.data.participants[0].isGameHost").value(false))
                    .andExpect(jsonPath("$.data.participants[0].name").value("김모임장"))
                    .andExpect(jsonPath("$.data.participants[0].gender").value("FEMALE"))
                    .andExpect(jsonPath("$.data.participants[0].level").value("D조"))
                    .andExpect(jsonPath("$.data.participants[0].lastExerciseDate").value("2026-01-12"))
                    .andExpect(jsonPath("$.data.participants[1].participantId").value(subManager.getId()))
                    .andExpect(jsonPath("$.data.participants[1].partyPosition").value("부모임장"))
                    .andExpect(jsonPath("$.data.participants[1].isGameHost").value(false))
                    .andExpect(jsonPath("$.data.participants[1].level").value("자강"))
                    .andExpect(jsonPath("$.data.participants[1].lastExerciseDate").value("2026-01-10"))
                    .andExpect(jsonPath("$.data.participants[2].participantId").value(normalMember.getId()))
                    .andExpect(jsonPath("$.data.participants[2].partyPosition").value("멤버"))
                    .andExpect(jsonPath("$.data.participants[2].isGameHost").value(true))
                    .andExpect(jsonPath("$.data.participants[2].profileImageUrl").value(nullValue()))
                    .andExpect(jsonPath("$.data.participants[2].lastExerciseDate").value(nullValue()));
        }

        @Test
        @DisplayName("200 - 부모임장도 게임 진행자 후보를 조회할 수 있다")
        void subManagerGetsGameHostCandidates() throws Exception {
            SecurityContextHelper.setAuthentication(subManager.getId(), subManager.getNickname());

            mockMvc.perform(get("/api/exercises/{exerciseId}/game-host", exercise.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalCount").value(3));
        }

        @Test
        @DisplayName("403 - 일반 멤버는 게임 진행자 후보를 조회할 수 없다")
        void normalMemberCannotGetGameHostCandidates() throws Exception {
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

            mockMvc.perform(get("/api/exercises/{exerciseId}/game-host", exercise.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code")
                            .value(ExerciseErrorCode.GAME_HOST_MANAGEMENT_PERMISSION_DENIED.getCode()));
        }

        @Test
        @DisplayName("404 - 존재하지 않는 운동은 조회할 수 없다")
        void exerciseNotFound() throws Exception {
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            mockMvc.perform(get("/api/exercises/{exerciseId}/game-host", 999999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("PATCH /api/exercises/{exerciseId}/game-host")
    class ChangeGameHost {

        @Test
        @DisplayName("200 - 모임장이 활성 일반 멤버를 게임 진행자로 변경한다")
        void managerChangesGameHostToNormalMember() throws Exception {
            ReflectionTestUtils.setField(exercise, "gameHostId", manager.getId());
            exerciseRepository.save(exercise);
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());
            ExerciseGameHostDTO.ChangeRequest request =
                    new ExerciseGameHostDTO.ChangeRequest(normalMember.getId());

            mockMvc.perform(patch("/api/exercises/{exerciseId}/game-host", exercise.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.exerciseId").value(exercise.getId()))
                    .andExpect(jsonPath("$.data.participantId").value(normalMember.getId()));

            assertThat(exerciseRepository.findById(exercise.getId()).orElseThrow().getGameHostId())
                    .isEqualTo(normalMember.getId());
        }

        @Test
        @DisplayName("200 - 부모임장도 게임 진행자를 변경할 수 있다")
        void subManagerChangesGameHost() throws Exception {
            SecurityContextHelper.setAuthentication(subManager.getId(), subManager.getNickname());
            ExerciseGameHostDTO.ChangeRequest request =
                    new ExerciseGameHostDTO.ChangeRequest(manager.getId());

            mockMvc.perform(patch("/api/exercises/{exerciseId}/game-host", exercise.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.participantId").value(manager.getId()));

            assertThat(exerciseRepository.findById(exercise.getId()).orElseThrow().getGameHostId())
                    .isEqualTo(manager.getId());
        }

        @Test
        @DisplayName("200 - 현재 게임 진행자를 다시 지정해도 성공한다")
        void sameGameHostSucceeds() throws Exception {
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());
            ExerciseGameHostDTO.ChangeRequest request =
                    new ExerciseGameHostDTO.ChangeRequest(normalMember.getId());

            mockMvc.perform(patch("/api/exercises/{exerciseId}/game-host", exercise.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.participantId").value(normalMember.getId()));
        }

        @Test
        @DisplayName("403 - 일반 멤버는 게임 진행자를 변경할 수 없다")
        void normalMemberCannotChangeGameHost() throws Exception {
            SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());
            ExerciseGameHostDTO.ChangeRequest request =
                    new ExerciseGameHostDTO.ChangeRequest(manager.getId());

            mockMvc.perform(patch("/api/exercises/{exerciseId}/game-host", exercise.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code")
                            .value(ExerciseErrorCode.GAME_HOST_MANAGEMENT_PERMISSION_DENIED.getCode()));

            assertThat(exerciseRepository.findById(exercise.getId()).orElseThrow().getGameHostId())
                    .isEqualTo(normalMember.getId());
        }

        @Test
        @DisplayName("400 - 강퇴된 모임원은 게임 진행자로 지정할 수 없다")
        void bannedMemberCannotBecomeGameHost() throws Exception {
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());
            ExerciseGameHostDTO.ChangeRequest request =
                    new ExerciseGameHostDTO.ChangeRequest(bannedMember.getId());

            mockMvc.perform(patch("/api/exercises/{exerciseId}/game-host", exercise.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code")
                            .value(ExerciseErrorCode.INVALID_GAME_HOST_CANDIDATE.getCode()));

            assertThat(exerciseRepository.findById(exercise.getId()).orElseThrow().getGameHostId())
                    .isEqualTo(normalMember.getId());
        }

        @Test
        @DisplayName("400 - participantId가 없으면 요청 검증에 실패한다")
        void participantIdIsRequired() throws Exception {
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

            mockMvc.perform(patch("/api/exercises/{exerciseId}/game-host", exercise.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 - 존재하지 않는 운동은 게임 진행자를 변경할 수 없다")
        void exerciseNotFound() throws Exception {
            SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());
            ExerciseGameHostDTO.ChangeRequest request =
                    new ExerciseGameHostDTO.ChangeRequest(normalMember.getId());

            mockMvc.perform(patch("/api/exercises/{exerciseId}/game-host", 999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()));
        }
    }

    private MemberParty memberParty(
            Member member,
            Role role,
            MemberPartyStatus status,
            LocalDateTime joinedAt) {
        return MemberParty.builder()
                .party(party)
                .member(member)
                .role(role)
                .joinedAt(joinedAt)
                .status(status)
                .build();
    }
}
