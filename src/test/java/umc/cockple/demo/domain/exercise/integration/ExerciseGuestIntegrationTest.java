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
import umc.cockple.demo.domain.exercise.dto.ExerciseGuestInviteDTO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseGuestIntegrationTest extends IntegrationTestBase {

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
    private Member normalMember;
    private Party party;

    @BeforeEach
    void setUp() {
        manager = memberRepository.save(MemberFixture.createMember("모임장", Gender.MALE, Level.A, 1001L, LocalDate.of(2000, 1, 1)));
        normalMember = memberRepository.save(MemberFixture.createMember("일반멤버", Gender.MALE, Level.C, 1003L, LocalDate.of(2000, 1, 1)));

        PartyAddr addr = partyAddrRepository.save(PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(PartyFixture.createParty("테스트 모임", manager.getId(), addr));

        memberPartyRepository.save(MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER));
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

}
