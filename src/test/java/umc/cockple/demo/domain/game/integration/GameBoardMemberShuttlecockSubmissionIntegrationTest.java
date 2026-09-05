package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("게임판 명단 셔틀콕 제출 상태 변경 통합 테스트")
class GameBoardMemberShuttlecockSubmissionIntegrationTest extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PartyAddrRepository partyAddrRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private GameBoardMemberRepository gameBoardMemberRepository;

    private Member gameHost;
    private Member otherMember;
    private Party party;
    private Exercise exercise;
    private GameBoardMember gameBoardMember;

    @BeforeEach
    void setUp() {
        gameHost = memberRepository.save(MemberFixture.createMemberWithName(
                "게임 진행자", "진행자", Gender.FEMALE, Level.A, 75001L));
        otherMember = memberRepository.save(MemberFixture.createMemberWithName(
                "일반 회원", "일반", Gender.MALE, Level.B, 75002L));

        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(
                PartyFixture.createParty("셔틀콕 제출 테스트 모임", gameHost.getId(), partyAddr));
        exercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31)));

        gameBoardMember = gameBoardMemberRepository.save(GameBoardMember.builder()
                .gameBoard(exercise.getGameBoard())
                .name("선수")
                .gender(Gender.MALE)
                .level(Level.C)
                .shuttlecockSubmitted(false)
                .participating(true)
                .gameCount(0)
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHelper.clearAuthentication();
        gameBoardMemberRepository.deleteAll();
        exerciseRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("셔틀콕 제출 상태를 양방향으로 변경하고 명단 조회와 필터에 반영한다")
        void changesBothDirectionsAndUpdatesMemberQuery() throws Exception {
            authenticate(gameHost);

            changeSubmission(gameBoardMember, true)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").doesNotExist());

            assertThat(readGameBoardMember().getShuttlecockSubmitted()).isTrue();
            mockMvc.perform(get("/api/game-boards/{gameBoardId}/gameBoardMembers",
                            exercise.getGameBoard().getId())
                            .param("shuttlecockSubmitted", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalCount").value(1))
                    .andExpect(jsonPath("$.data.gameBoardMembers.length()").value(1))
                    .andExpect(jsonPath("$.data.gameBoardMembers[0].gameBoardMemberId")
                            .value(gameBoardMember.getId()))
                    .andExpect(jsonPath("$.data.gameBoardMembers[0].shuttlecockSubmitted").value(true));

            changeSubmission(gameBoardMember, false)
                    .andExpect(status().isOk());

            assertThat(readGameBoardMember().getShuttlecockSubmitted()).isFalse();
        }

        @Test
        @DisplayName("현재 값과 같은 제출 상태 요청은 성공한다")
        void sameValueSucceeds() throws Exception {
            authenticate(gameHost);

            changeSubmission(gameBoardMember, false)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").doesNotExist());

            assertThat(readGameBoardMember().getShuttlecockSubmitted()).isFalse();
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("다른 게임판 소속 명단의 제출 상태는 변경할 수 없다")
        void rejectsMemberFromOtherGameBoard() throws Exception {
            Exercise otherExercise = exerciseRepository.save(
                    ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 30)));
            GameBoardMember otherBoardMember = gameBoardMemberRepository.save(GameBoardMember.builder()
                    .gameBoard(otherExercise.getGameBoard())
                    .name("다른 선수")
                    .gender(Gender.FEMALE)
                    .level(Level.D)
                    .shuttlecockSubmitted(false)
                    .participating(true)
                    .gameCount(0)
                    .build());
            authenticate(gameHost);

            mockMvc.perform(patch(
                            "/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}/shuttlecock-submission",
                            exercise.getGameBoard().getId(), otherBoardMember.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"shuttlecockSubmitted\":true}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code")
                            .value(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("게임 진행자가 아니면 제출 상태를 변경할 수 없다")
        void deniesNonGameHost() throws Exception {
            authenticate(otherMember);

            changeSubmission(gameBoardMember, true)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(GameErrorCode.GAME_BOARD_ACCESS_DENIED.getCode()));

            assertThat(readGameBoardMember().getShuttlecockSubmitted()).isFalse();
        }

        @Test
        @DisplayName("게임판을 찾을 수 없으면 404를 반환한다")
        void rejectsUnknownGameBoard() throws Exception {
            authenticate(gameHost);

            mockMvc.perform(patch(
                            "/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}/shuttlecock-submission",
                            Long.MAX_VALUE, gameBoardMember.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"shuttlecockSubmitted\":true}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(GameErrorCode.GAME_BOARD_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("제출 상태가 없거나 null이면 400을 반환한다")
        void requiresShuttlecockSubmittedValue() throws Exception {
            authenticate(gameHost);

            for (String request : List.of("{}", "{\"shuttlecockSubmitted\":null}")) {
                mockMvc.perform(patch(
                                "/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}/shuttlecock-submission",
                                exercise.getGameBoard().getId(), gameBoardMember.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                        .andExpect(status().isBadRequest());
            }

            assertThat(readGameBoardMember().getShuttlecockSubmitted()).isFalse();
        }
    }

    private ResultActions changeSubmission(
            GameBoardMember member, boolean shuttlecockSubmitted) throws Exception {
        return mockMvc.perform(patch(
                        "/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}/shuttlecock-submission",
                        exercise.getGameBoard().getId(), member.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shuttlecockSubmitted\":" + shuttlecockSubmitted + "}"));
    }

    private void authenticate(Member member) {
        SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());
    }

    private GameBoardMember readGameBoardMember() {
        return gameBoardMemberRepository.findById(gameBoardMember.getId()).orElseThrow();
    }
}
