package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("게임 랜덤 매칭 API 통합 테스트")
class GameRandomMatchIntegrationTest extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PartyAddrRepository partyAddrRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private GameBoardMemberRepository gameBoardMemberRepository;
    @Autowired private GameRepository gameRepository;

    private Member gameHost;
    private Member otherMember;
    private Exercise exercise;
    private List<GameBoardMember> members;

    @BeforeEach
    void setUp() {
        gameHost = memberRepository.save(MemberFixture.createMemberWithName(
                "랜덤 진행자", "랜덤진행자", Gender.MALE, Level.A, 71901L));
        otherMember = memberRepository.save(MemberFixture.createMemberWithName(
                "일반 회원", "일반회원", Gender.FEMALE, Level.B, 71902L));

        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        Party party = partyRepository.save(
                PartyFixture.createParty("랜덤 매칭 테스트 모임", gameHost.getId(), partyAddr));
        exercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31)));

        members = new ArrayList<>();
        for (int index = 1; index <= 4; index++) {
            members.add(gameBoardMemberRepository.save(GameBoardMember.builder()
                    .gameBoard(exercise.getGameBoard())
                    .name("선수" + index)
                    .gender(Gender.MALE)
                    .level(Level.A)
                    .ageGroup(AgeGroup.TWENTIES)
                    .shuttlecockSubmitted(false)
                    .participating(true)
                    .gameCount(0)
                    .build()));
        }
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHelper.clearAuthentication();
    }

    @Test
    @DisplayName("게임 진행자에게 오름차순 명단 ID 4개만 반환하고 게임은 저장하지 않는다")
    void randomMatch_returnsIdsWithoutPersistingGame() throws Exception {
        authenticate(gameHost);

        mockMvc.perform(post(
                        "/api/game-boards/{gameBoardId}/games/random-match",
                        exercise.getGameBoard().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", aMapWithSize(1)))
                .andExpect(jsonPath("$.data.gameBoardMemberIds.length()").value(4))
                .andExpect(jsonPath("$.data.gameBoardMemberIds[0]").value(members.get(0).getId()))
                .andExpect(jsonPath("$.data.gameBoardMemberIds[1]").value(members.get(1).getId()))
                .andExpect(jsonPath("$.data.gameBoardMemberIds[2]").value(members.get(2).getId()))
                .andExpect(jsonPath("$.data.gameBoardMemberIds[3]").value(members.get(3).getId()))
                .andExpect(jsonPath("$.data.matchType").doesNotExist());

        assertThat(gameRepository.count()).isZero();
    }

    @Test
    @DisplayName("급수없음 제외 후 후보가 4명 미만이면 GAME415를 반환한다")
    void randomMatch_rejectsInsufficientAvailablePlayers() throws Exception {
        GameBoardMember excluded = members.get(3);
        excluded.updateInfo(excluded.getName(), excluded.getGender(), Level.NONE, excluded.getAgeGroup());
        authenticate(gameHost);

        expectBadRequest(GameErrorCode.INSUFFICIENT_AVAILABLE_PLAYERS);
    }

    @Test
    @DisplayName("성별 구성이 남자 3명 여자 1명이면 GAME416을 반환한다")
    void randomMatch_rejectsInsufficientGenderComposition() throws Exception {
        GameBoardMember female = members.get(3);
        female.updateInfo(female.getName(), Gender.FEMALE, female.getLevel(), female.getAgeGroup());
        authenticate(gameHost);

        expectBadRequest(GameErrorCode.INSUFFICIENT_GENDER_COMPOSITION);
    }

    @Test
    @DisplayName("최소 경기 수 +5 안에 4명이 모이지 않으면 GAME417을 반환한다")
    void randomMatch_rejectsWhenCandidateExpansionFails() throws Exception {
        increaseGameCount(members.get(1), 1);
        increaseGameCount(members.get(2), 5);
        increaseGameCount(members.get(3), 6);
        authenticate(gameHost);

        expectBadRequest(GameErrorCode.RANDOM_MATCH_NOT_FOUND);
    }

    @Test
    @DisplayName("게임 진행자가 아닌 회원은 403을 반환한다")
    void randomMatch_deniesNonGameHost() throws Exception {
        authenticate(otherMember);

        mockMvc.perform(post("/api/game-boards/{gameBoardId}/games/random-match",
                        exercise.getGameBoard().getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(GameErrorCode.GAME_BOARD_ACCESS_DENIED.getCode()));
    }

    @Test
    @DisplayName("미인증 요청은 401을 반환한다")
    void randomMatch_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/game-boards/{gameBoardId}/games/random-match",
                        exercise.getGameBoard().getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 게임판은 404를 반환한다")
    void randomMatch_rejectsMissingGameBoard() throws Exception {
        authenticate(gameHost);

        mockMvc.perform(post("/api/game-boards/{gameBoardId}/games/random-match", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(GameErrorCode.GAME_BOARD_NOT_FOUND.getCode()));
    }

    private void expectBadRequest(GameErrorCode errorCode) throws Exception {
        mockMvc.perform(post("/api/game-boards/{gameBoardId}/games/random-match",
                        exercise.getGameBoard().getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(errorCode.getCode()));
    }

    private void increaseGameCount(GameBoardMember member, int count) {
        for (int index = 0; index < count; index++) {
            member.increaseGameCount();
        }
    }

    private void authenticate(Member member) {
        SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());
    }
}
