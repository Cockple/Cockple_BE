package umc.cockple.demo.domain.exercise.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseUpdateDTO;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.member.repository.MemberExerciseRepository;
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
import umc.cockple.demo.support.fixture.GuestFixture;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseCommandIntegrationTest extends IntegrationTestBase {

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
                    "공지사항"
            );
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("200 - 모임장이 운동을 수정하면 exerciseId를 반환한다")
            void owner_updateExercise() throws Exception {
                SecurityContextHelper.setAuthentication(manager.getId(), manager.getNickname());

                mockMvc.perform(patch("/api/exercises/{exerciseId}", exercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.exerciseId").value(exercise.getId()));
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
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.MEMBER_NOT_FOUND.getMessage()));
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
                        "13:00", "11:00", null, null
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
                        "10:00", "12:00", null, null
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
                memberPartyRepository.save(MemberFixture.createMemberParty(party, youngMember, Role.party_MEMBER));

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

    @Nested
    @DisplayName("POST /api/exercises/{exerciseId}/guests - 게스트 초대")
    class InviteGuest {

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
            @DisplayName("201 - 파티 멤버가 게스트를 초대하면 guestId를 반환한다")
            void partyMember_inviteGuest() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                ExerciseGuestInviteDTO.Request request = new ExerciseGuestInviteDTO.Request(
                        "테스트게스트", "남성", "B조");

                mockMvc.perform(post("/api/exercises/{exerciseId}/guests", exercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.guestId").isNumber())
                        .andExpect(jsonPath("$.data.invitedAt").isString())
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

                ExerciseGuestInviteDTO.Request request = new ExerciseGuestInviteDTO.Request(
                        "테스트게스트", "남성", "B조");

                mockMvc.perform(post("/api/exercises/{exerciseId}/guests", 999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - SecurityContext의 멤버가 DB에 없으면 에러를 반환한다")
            void memberNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                ExerciseGuestInviteDTO.Request request = new ExerciseGuestInviteDTO.Request(
                        "테스트게스트", "남성", "B조");

                mockMvc.perform(post("/api/exercises/{exerciseId}/guests", exercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
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

                ExerciseGuestInviteDTO.Request request = new ExerciseGuestInviteDTO.Request(
                        "테스트게스트", "남성", "B조");

                mockMvc.perform(post("/api/exercises/{exerciseId}/guests", startedExercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_INVITATION.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_INVITATION.getMessage()));
            }

            @Test
            @DisplayName("403 - 파티 멤버가 아닌 사용자가 초대하면 에러를 반환한다")
            void notPartyMember() throws Exception {
                Member outsideMember = memberRepository.save(
                        MemberFixture.createMember("외부인", Gender.MALE, Level.B, 3001L, LocalDate.of(2000, 1, 1)));

                SecurityContextHelper.setAuthentication(outsideMember.getId(), outsideMember.getNickname());

                ExerciseGuestInviteDTO.Request request = new ExerciseGuestInviteDTO.Request(
                        "테스트게스트", "남성", "B조");

                mockMvc.perform(post("/api/exercises/{exerciseId}/guests", exercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.NOT_PARTY_MEMBER_FOR_GUEST_INVITE.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.NOT_PARTY_MEMBER_FOR_GUEST_INVITE.getMessage()));
            }

            @Test
            @DisplayName("403 - 게스트 초대 정책 비허용이면 에러를 반환한다")
            void guestPolicyNotAllowed() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                Exercise noGuestExercise = exerciseRepository.save(
                        ExerciseFixture.createExercise(party, LocalDate.of(2099, 12, 31),
                                LocalTime.of(12, 0), false, false));

                ExerciseGuestInviteDTO.Request request = new ExerciseGuestInviteDTO.Request(
                        "테스트게스트", "남성", "B조");

                mockMvc.perform(post("/api/exercises/{exerciseId}/guests", noGuestExercise.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.GUEST_INVITATION_NOT_ALLOWED.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.GUEST_INVITATION_NOT_ALLOWED.getMessage()));
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/exercises/{exerciseId}/guests/{guestId} - 게스트 초대 취소")
    class CancelGuestInvitation {

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
            @DisplayName("200 - 초대자가 본인 게스트를 취소하면 memberName을 반환한다")
            void cancelGuestInvitation_success() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                Guest guest = guestRepository.save(GuestFixture.createGuest(exercise, normalMember.getId()));

                mockMvc.perform(delete("/api/exercises/{exerciseId}/guests/{guestId}",
                                exercise.getId(), guest.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.memberName").value("게스트"))
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

                Guest guest = guestRepository.save(GuestFixture.createGuest(exercise, normalMember.getId()));

                mockMvc.perform(delete("/api/exercises/{exerciseId}/guests/{guestId}",
                                999L, guest.getId()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("404 - SecurityContext의 멤버가 DB에 없으면 에러를 반환한다")
            void memberNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(999L, "없는멤버");

                Guest guest = guestRepository.save(GuestFixture.createGuest(exercise, normalMember.getId()));

                mockMvc.perform(delete("/api/exercises/{exerciseId}/guests/{guestId}",
                                exercise.getId(), guest.getId()))
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

                Guest guest = guestRepository.save(GuestFixture.createGuest(startedExercise, normalMember.getId()));

                mockMvc.perform(delete("/api/exercises/{exerciseId}/guests/{guestId}",
                                startedExercise.getId(), guest.getId()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.EXERCISE_ALREADY_STARTED_CANCEL.getMessage()));
            }

            @Test
            @DisplayName("404 - 존재하지 않는 게스트면 에러를 반환한다")
            void guestNotFound() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                mockMvc.perform(delete("/api/exercises/{exerciseId}/guests/{guestId}",
                                exercise.getId(), 999L))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.GUEST_NOT_FOUND.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.GUEST_NOT_FOUND.getMessage()));
            }

            @Test
            @DisplayName("403 - 본인이 초대하지 않은 게스트면 에러를 반환한다")
            void guestNotInvitedByMember() throws Exception {
                SecurityContextHelper.setAuthentication(normalMember.getId(), normalMember.getNickname());

                Guest guest = guestRepository.save(GuestFixture.createGuest(exercise, manager.getId()));

                mockMvc.perform(delete("/api/exercises/{exerciseId}/guests/{guestId}",
                                exercise.getId(), guest.getId()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(ExerciseErrorCode.GUEST_NOT_INVITED_BY_MEMBER.getCode()))
                        .andExpect(jsonPath("$.message").value(ExerciseErrorCode.GUEST_NOT_INVITED_BY_MEMBER.getMessage()));
            }
        }
    }

}
