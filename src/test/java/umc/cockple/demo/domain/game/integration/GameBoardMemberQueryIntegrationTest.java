package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameBoardRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.ProfileImg;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.SecurityContextHelper;
import umc.cockple.demo.support.fixture.MemberFixture;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("게임판 명단 조회 통합 테스트")
class GameBoardMemberQueryIntegrationTest extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private GameBoardRepository gameBoardRepository;
    @Autowired private GameBoardMemberRepository gameBoardMemberRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private MemberRepository memberRepository;

    private GameBoard gameBoard;
    private GameBoardMember playingMember;
    private GameBoardMember waitingMember;
    private GameBoardMember completedMember;
    private GameBoardMember idleMember;

    @BeforeEach
    void setUp() {
        gameBoard = gameBoardRepository.save(GameBoard.create());

        Member profileMember = MemberFixture.createMemberWithName(
                "프로필 선수", "프로필 닉네임", Gender.FEMALE, Level.A, 71001L);
        profileMember.updateProfileImg(ProfileImg.builder().imgKey("profiles/game-player.jpg").build());
        profileMember = memberRepository.save(profileMember);

        playingMember = saveMember(profileMember, "가 선수", Gender.FEMALE, Level.A,
                AgeGroup.TWENTIES, true, true, 3);
        waitingMember = saveMember(null, "나 선수", Gender.MALE, Level.B,
                AgeGroup.THIRTIES, false, true, 1);
        completedMember = saveMember(null, "다 선수", Gender.MALE, Level.A,
                null, true, false, 2);
        idleMember = saveMember(null, "라 선수", Gender.FEMALE, Level.C,
                AgeGroup.FORTIES, false, true, 0);

        saveGame(GameStatus.PLAYING, playingMember);
        saveGame(GameStatus.WAITING, waitingMember);
        saveGame(GameStatus.COMPLETED, completedMember);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHelper.clearAuthentication();
    }

    @Test
    @DisplayName("필터 없이 전체 명단을 ID 오름차순과 게임 상태, 프로필 정보로 조회한다")
    void getMembers_returnsAllMembersInIdOrder() throws Exception {
        authenticate();

        mockMvc.perform(get("/api/game-boards/{gameBoardId}/gameBoardMembers", gameBoard.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.gameBoardMembers.length()").value(4))
                .andExpect(jsonPath("$.data.gameBoardMembers[0].gameBoardMemberId").value(playingMember.getId()))
                .andExpect(jsonPath("$.data.gameBoardMembers[1].gameBoardMemberId").value(waitingMember.getId()))
                .andExpect(jsonPath("$.data.gameBoardMembers[2].gameBoardMemberId").value(completedMember.getId()))
                .andExpect(jsonPath("$.data.gameBoardMembers[3].gameBoardMemberId").value(idleMember.getId()))
                .andExpect(jsonPath("$.data.gameBoardMembers[0].inGame").value(true))
                .andExpect(jsonPath("$.data.gameBoardMembers[0].waiting").value(false))
                .andExpect(jsonPath("$.data.gameBoardMembers[1].inGame").value(false))
                .andExpect(jsonPath("$.data.gameBoardMembers[1].waiting").value(true))
                .andExpect(jsonPath("$.data.gameBoardMembers[2].inGame").value(false))
                .andExpect(jsonPath("$.data.gameBoardMembers[2].waiting").value(false))
                .andExpect(jsonPath("$.data.gameBoardMembers[0].profileImageUrl")
                        .value("https://storage.googleapis.com/test-bucket/profiles/game-player.jpg"))
                .andExpect(jsonPath("$.data.gameBoardMembers[1].profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.gameBoardMembers[0].ageGroup").value("20대"))
                .andExpect(jsonPath("$.data.gameBoardMembers[2].ageGroup").value(nullValue()))
                .andExpect(jsonPath("$.data.gameBoardMembers[0].level").value("A조"))
                .andExpect(jsonPath("$.data.gameBoardMembers[0].gender").doesNotExist());
    }

    @Test
    @DisplayName("단일 급수 필터를 적용해도 totalCount는 전체 명단 수를 반환한다")
    void getMembers_filtersSingleLevelWithoutChangingTotalCount() throws Exception {
        authenticate();

        mockMvc.perform(get("/api/game-boards/{gameBoardId}/gameBoardMembers", gameBoard.getId())
                        .param("level", "B조"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.gameBoardMembers.length()").value(1))
                .andExpect(jsonPath("$.data.gameBoardMembers[0].gameBoardMemberId").value(waitingMember.getId()));
    }

    @Test
    @DisplayName("복수 급수 필터는 OR 조건으로 적용한다")
    void getMembers_filtersMultipleLevelsWithOrCondition() throws Exception {
        authenticate();

        mockMvc.perform(get("/api/game-boards/{gameBoardId}/gameBoardMembers", gameBoard.getId())
                        .param("level", "A조", "B조"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gameBoardMembers.length()").value(3))
                .andExpect(jsonPath("$.data.gameBoardMembers[0].gameBoardMemberId").value(playingMember.getId()))
                .andExpect(jsonPath("$.data.gameBoardMembers[1].gameBoardMemberId").value(waitingMember.getId()))
                .andExpect(jsonPath("$.data.gameBoardMembers[2].gameBoardMemberId").value(completedMember.getId()));
    }

    @Test
    @DisplayName("급수, 성별, 셔틀콕 필터는 AND 조건으로 적용한다")
    void getMembers_combinesDifferentFiltersWithAndCondition() throws Exception {
        authenticate();

        mockMvc.perform(get("/api/game-boards/{gameBoardId}/gameBoardMembers", gameBoard.getId())
                        .param("level", "A조")
                        .param("gender", "남성")
                        .param("shuttlecockSubmitted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.gameBoardMembers.length()").value(1))
                .andExpect(jsonPath("$.data.gameBoardMembers[0].gameBoardMemberId").value(completedMember.getId()));
    }

    @Test
    @DisplayName("존재하지 않는 게임판은 404를 반환한다")
    void getMembers_returnsNotFoundForMissingGameBoard() throws Exception {
        authenticate();

        mockMvc.perform(get("/api/game-boards/{gameBoardId}/gameBoardMembers", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("미인증 사용자는 명단을 조회할 수 없다")
    void getMembers_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/game-boards/{gameBoardId}/gameBoardMembers", gameBoard.getId()))
                .andExpect(status().isUnauthorized());
    }

    private GameBoardMember saveMember(
            Member member,
            String name,
            Gender gender,
            Level level,
            AgeGroup ageGroup,
            boolean shuttlecockSubmitted,
            boolean participating,
            int gameCount) {
        return gameBoardMemberRepository.save(GameBoardMember.builder()
                .gameBoard(gameBoard)
                .member(member)
                .name(name)
                .gender(gender)
                .level(level)
                .ageGroup(ageGroup)
                .shuttlecockSubmitted(shuttlecockSubmitted)
                .participating(participating)
                .gameCount(gameCount)
                .build());
    }

    private void saveGame(GameStatus status, GameBoardMember member) {
        Game game = Game.builder()
                .gameBoard(gameBoard)
                .status(status)
                .waitingOrder(status == GameStatus.WAITING ? 1 : null)
                .startedAt(status == GameStatus.PLAYING ? LocalDateTime.now() : null)
                .completedAt(status == GameStatus.COMPLETED ? LocalDateTime.now() : null)
                .build();
        game.addPlayer(GamePlayer.create(member, 0));
        gameRepository.save(game);
    }

    private void authenticate() {
        SecurityContextHelper.setAuthentication(999L, "명단 조회자");
    }
}
