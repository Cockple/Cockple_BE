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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.presentation.dto.lifecycle.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.presentation.dto.lifecycle.ExerciseUpdateDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.exception.MemberErrorCode;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
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
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.GuestFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseLifecycleIntegrationTest extends IntegrationTestBase {

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
            @DisplayName("201 - 모임장이 운동을 생성하면 게임판이 함께 생성된다")
            void owner_createExercise() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                MvcResult mvcResult = mockMvc.perform(post("/api/parties/{partyId}/exercises", party.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.exerciseId").isNumber())
                        .andReturn();

                long exerciseId = objectMapper.readTree(mvcResult.getResponse().getContentAsString())
                        .path("data")
                        .path("exerciseId")
                        .asLong();
                Exercise createdExercise = exerciseRepository.findById(exerciseId).orElseThrow();

                assertThat(createdExercise.getGameBoard()).isNotNull();
                assertThat(createdExercise.getGameBoard().getId()).isNotNull();
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
                        .andExpect(jsonPath("$.code").value(PartyErrorCode.PARTY_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(PartyErrorCode.PARTY_NOT_FOUND.getMessage()));
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
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MEMBER_NOT_FOUND.getMessage()));
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
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MEMBER_NOT_FOUND.getMessage()));
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

    @Nested
    @DisplayName("PATCH /api/exercises/{exerciseId} - 운동 수정")
    class UpdateExercise {

        private Exercise exercise;
        private ExerciseUpdateDTO.Request validRequest;

        @BeforeEach
        void setUp() {
            exercise = exerciseRepository.save(
                    ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31)));

            validRequest = new ExerciseUpdateDTO.Request(
                    "2099-12-31",
                    "수정된 체육관",
                    "서울특별시 강남구 테헤란로 2",
                    37.6,
                    127.1,
                    "11:00",
                    "13:00",
                    12,
                    false,
                    true,
                    "공지사항"
            );
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 모임장이 운동을 수정하면 exerciseId와 갱신된 updatedAt을 반환한다")
            void owner_updateExercise() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());
                LocalDateTime previousUpdatedAt = exercise.getUpdatedAt();

                mockMvc.perform(patch("/api/exercises/{exerciseId}", exercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.exerciseId").value(exercise.getId()))
                        .andExpect(result -> {
                            String updatedAt = objectMapper.readTree(result.getResponse().getContentAsString())
                                    .path("data")
                                    .path("updatedAt")
                                    .asText();
                            assertThat(LocalDateTime.parse(updatedAt)).isAfter(previousUpdatedAt);
                        });

                Exercise updatedExercise = exerciseRepository.findById(exercise.getId()).orElseThrow();
                assertThat(updatedExercise.getPartyGuestAccept()).isFalse();
                assertThat(updatedExercise.getOutsideGuestAccept()).isTrue();
            }

            @Test
            @DisplayName("200 - 부모임장도 운동을 수정할 수 있다")
            void subManager_updateExercise() throws Exception {
                SecurityContextHelper.setAuthentication(subManager.getId(), subManager.getNickname());

                mockMvc.perform(patch("/api/exercises/{exerciseId}", exercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.exerciseId").value(exercise.getId()));
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("404 - 존재하지 않는 운동이면 에러를 반환한다")
            void exerciseNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(patch("/api/exercises/{exerciseId}", 999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - SecurityContext의 멤버가 DB에 없으면 에러를 반환한다")
            void memberNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                mockMvc.perform(patch("/api/exercises/{exerciseId}", exercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MEMBER_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("403 - 일반 멤버가 수정 시 에러를 반환한다")
            void normalMember_forbidden() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(patch("/api/exercises/{exerciseId}", exercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
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

                mockMvc.perform(patch("/api/exercises/{exerciseId}", startedExercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_UPDATE.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_UPDATE.getMessage()));
            }

            @Test
            @DisplayName("400 - 시작 시간이 종료 시간 이후면 에러를 반환한다")
            void invalidTime() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                ExerciseUpdateDTO.Request invalidTimeRequest = new ExerciseUpdateDTO.Request(
                        "2099-12-31", null, null, null, null,
                        "13:00", "11:00", null, null, null, null
                );

                mockMvc.perform(patch("/api/exercises/{exerciseId}", exercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidTimeRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INVALID_EXERCISE_TIME.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INVALID_EXERCISE_TIME.getMessage()));
            }

            @Test
            @DisplayName("400 - 과거 날짜로 수정 시 에러를 반환한다")
            void pastDate() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                ExerciseUpdateDTO.Request pastDateRequest = new ExerciseUpdateDTO.Request(
                        "2000-01-01", null, null, null, null,
                        "10:00", "12:00", null, null, null, null
                );

                mockMvc.perform(patch("/api/exercises/{exerciseId}", exercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pastDateRequest)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.PAST_TIME_NOT_ALLOWED.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.PAST_TIME_NOT_ALLOWED.getMessage()));
            }
        }
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
                        .andExpect(jsonPath("$.data.participants.list[0].partyPosition").value("PARTY_MEMBER"))
                        .andExpect(jsonPath("$.data.participants.list[0].isWithdrawn").value(false))
                        .andExpect(jsonPath("$.data.waiting.currentWaitingCount").value(1))
                        .andExpect(jsonPath("$.data.waiting.manCount").value(0))
                        .andExpect(jsonPath("$.data.waiting.womenCount").value(1))
                        .andExpect(jsonPath("$.data.waiting.list[0].name").value(subManager.getMemberName()))
                        .andExpect(jsonPath("$.data.waiting.list[0].gender").value("FEMALE"))
                        .andExpect(jsonPath("$.data.waiting.list[0].participantType").value("PARTY_MEMBER"))
                        .andExpect(jsonPath("$.data.waiting.list[0].partyPosition").value("PARTY_SUBMANAGER"))
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
                        .andExpect(jsonPath("$.data.participants.list[0].partyPosition").value("PARTY_MANAGER"))
                        .andExpect(jsonPath("$.data.participants.list[1].name").value(subManager.getMemberName()))
                        .andExpect(jsonPath("$.data.participants.list[1].participantType").value("PARTY_MEMBER"))
                        .andExpect(jsonPath("$.data.participants.list[1].partyPosition").value("PARTY_SUBMANAGER"))
                        .andExpect(jsonPath("$.data.participants.list[2].name").value(normalMember.getMemberName()))
                        .andExpect(jsonPath("$.data.participants.list[2].participantType").value("PARTY_MEMBER"))
                        .andExpect(jsonPath("$.data.participants.list[2].partyPosition").value("PARTY_MEMBER"))
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
                        .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(MemberErrorCode.MEMBER_NOT_FOUND.getMessage()));
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

            @Test
            @DisplayName("모임장이나 부모임장이 아니면 권한 없음 에러를 반환한다")
            void 모임장이나_부모임장이_아니면_권한_없음_에러를_반환한다() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(get("/api/exercises/{exerciseId}/for-edit", exercise.getId()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.INSUFFICIENT_PERMISSION.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.INSUFFICIENT_PERMISSION.getMessage()));
            }
        }
    }

}
