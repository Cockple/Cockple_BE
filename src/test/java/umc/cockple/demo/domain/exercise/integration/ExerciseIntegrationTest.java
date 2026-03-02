package umc.cockple.demo.domain.exercise.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.exception.PartyErrorCode;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.support.fixture.ExerciseFixture;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired ExerciseRepository exerciseRepository;

    private Member manager;
    private Member subManager;
    private Member normalMember;
    private Party party;

    @BeforeEach
    void setUp() {
        manager = memberRepository.save(MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L));
        subManager = memberRepository.save(MemberFixture.createMember("부모임장", Gender.FEMALE, Level.B, 1002L));
        normalMember = memberRepository.save(MemberFixture.createMember("일반멤버", Gender.MALE, Level.C, 1003L));

        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(PartyFixture.createParty("테스트 모임", manager.getId(), addr));

        memberPartyRepository.save(MemberFixture.createMemberParty(party, manager, Role.party_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, subManager, Role.party_SUBMANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, normalMember, Role.party_MEMBER));
    }

    @AfterEach
    void tearDown() {
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
        SecurityContextHelper.clearAuthentication();
    }

    @Nested
    @DisplayName("POST /api/parties/{partyId}/exercises - 운동 생성")
    class CreateExercise {

        private ExerciseCreateDTO.Request validRequest;

        @BeforeEach
        void setUp() {
            validRequest = ExerciseCreateDTO.Request.builder()
                    .date("2099-12-31")
                    .buildingName("테스트 체육관")
                    .roadAddress("서울특별시 강남구 테헤란로 1")
                    .latitude(37.5)
                    .longitude(127.0)
                    .startTime("10:00")
                    .endTime("12:00")
                    .maxCapacity(10)
                    .allowMemberGuestsInvitation(true)
                    .allowExternalGuests(false)
                    .build();
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("201 - 모임장이 운동을 생성하면 exerciseId를 반환한다")
            void owner_createExercise() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(post("/api/parties/{partyId}/exercises", party.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.exerciseId").isNumber());
            }

            @Test
            @DisplayName("201 - 부모임장도 운동을 생성할 수 있다")
            void subManager_createExercise() throws Exception {
                SecurityContextHelper.setAuthentication(subManager.getId(), subManager.getNickname());

                mockMvc.perform(post("/api/parties/{partyId}/exercises", party.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.exerciseId").isNumber());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 파티면 에러를 반환한다")
            void partyNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(post("/api/parties/{partyId}/exercises", 999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.PARTY_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.PARTY_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - SecurityContext의 멤버가 DB에 없으면 에러를 반환한다")
            void memberNotFound() throws Exception {
                // SecurityContext에는 존재하지 않는 memberId 세팅
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(post("/api/parties/{partyId}/exercises", party.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("400 - 비활성화된 파티면 에러를 반환한다")
            void inactiveParty() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                party.delete();
                partyRepository.save(party);

                mockMvc.perform(post("/api/parties/{partyId}/exercises", party.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_IS_DELETED.getCode()))
                        .andExpect(jsonPath("$.message").value(PartyErrorCode.PARTY_IS_DELETED.getMessage()));
            }

            @Test
            @DisplayName("403 - 일반 멤버가 생성 시 에러를 반환한다")
            void normalMember_forbidden() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(post("/api/parties/{partyId}/exercises", party.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INSUFFICIENT_PERMISSION.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INSUFFICIENT_PERMISSION.getMessage()));
            }

            @Test
            @DisplayName("400 - 시작 시간이 종료 시간 이후면 에러를 반환한다")
            void invalidExerciseTime() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                ExerciseCreateDTO.Request invalidRequest = ExerciseCreateDTO.Request.builder()
                        .date("2099-12-31")
                        .buildingName("체육관")
                        .roadAddress("서울특별시 강남구 테헤란로 1")
                        .latitude(37.5).longitude(127.0)
                        .startTime("12:00").endTime("10:00")
                        .maxCapacity(10)
                        .allowMemberGuestsInvitation(true)
                        .allowExternalGuests(false)
                        .build();

                mockMvc.perform(post("/api/parties/{partyId}/exercises", party.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INVALID_EXERCISE_TIME.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INVALID_EXERCISE_TIME.getMessage()));
            }

            @Test
            @DisplayName("400 - 과거 시간으로 운동 생성 시 에러를 반환한다")
            void pastTime() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                ExerciseCreateDTO.Request pastRequest = ExerciseCreateDTO.Request.builder()
                        .date("2000-01-01")
                        .buildingName("체육관")
                        .roadAddress("서울특별시 강남구 테헤란로 1")
                        .latitude(37.5).longitude(127.0)
                        .startTime("10:00").endTime("12:00")
                        .maxCapacity(10)
                        .allowMemberGuestsInvitation(true)
                        .allowExternalGuests(false)
                        .build();

                mockMvc.perform(post("/api/parties/{partyId}/exercises", party.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pastRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.PAST_TIME_NOT_ALLOWED.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.PAST_TIME_NOT_ALLOWED.getMessage()));
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/exercises/{exerciseId} - 운동 삭제")
    class DeleteExercise {

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
            @DisplayName("200 - 모임장이 운동을 삭제하면 deletedExerciseId를 반환한다")
            void owner_deleteExercise() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(delete("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.deletedExerciseId").value(exercise.getId()));
            }

            @Test
            @DisplayName("200 - 부모임장도 운동을 삭제할 수 있다")
            void subManager_deleteExercise() throws Exception {
                SecurityContextHelper.setAuthentication(subManager.getId(), subManager.getNickname());

                mockMvc.perform(delete("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.deletedExerciseId").value(exercise.getId()));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 운동이면 에러를 반환한다")
            void exerciseNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(delete("/api/exercises/{exerciseId}", 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - SecurityContext의 멤버가 DB에 없으면 에러를 반환한다")
            void memberNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(delete("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("403 - 일반 멤버가 삭제 시 에러를 반환한다")
            void normalMember_forbidden() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(delete("/api/exercises/{exerciseId}", exercise.getId()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INSUFFICIENT_PERMISSION.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INSUFFICIENT_PERMISSION.getMessage()));
            }
        }
    }
}
