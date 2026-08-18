package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.domain.game.enums.GameStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("게임판 명단 정보 수정 통합 테스트")
class GameBoardMemberUpdateIntegrationTest extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PartyAddrRepository partyAddrRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private GameBoardMemberRepository gameBoardMemberRepository;
    @Autowired private GameRepository gameRepository;

    private Member gameHost;
    private Member otherMember;
    private Party party;
    private Exercise exercise;
    private GameBoardMember gameBoardMember;

    @BeforeEach
    void setUp() {
        gameHost = memberRepository.save(MemberFixture.createMemberWithName(
                "게임 진행자", "진행자", Gender.FEMALE, Level.A, 73001L));
        otherMember = memberRepository.save(MemberFixture.createMemberWithName(
                "일반 회원", "일반", Gender.MALE, Level.B, 73002L));

        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(
                PartyFixture.createParty("명단 수정 테스트 모임", gameHost.getId(), partyAddr));
        exercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31)));

        gameBoardMember = gameBoardMemberRepository.save(GameBoardMember.builder()
                .gameBoard(exercise.getGameBoard())
                .name("수정 전")
                .gender(Gender.MALE)
                .level(Level.D)
                .ageGroup(AgeGroup.THIRTIES)
                .shuttlecockSubmitted(false)
                .participating(true)
                .gameCount(0)
                .build());

        Game waitingGame = Game.builder()
                .gameBoard(exercise.getGameBoard())
                .status(GameStatus.WAITING)
                .waitingOrder(1)
                .build();
        waitingGame.addPlayer(GamePlayer.create(gameBoardMember, 0));
        gameRepository.save(waitingGame);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHelper.clearAuthentication();
    }

    @Test
    @DisplayName("대기 게임 선수도 정보를 수정하고 보드 조회에 즉시 반영한다")
    void updateMember_updatesActivePlayerAndBoardView() throws Exception {
        authenticate(gameHost);
        String request = """
                {"name":"  수정 후  ","gender":"여성","level":"A조"}
                """;

        mockMvc.perform(patch("/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}",
                        exercise.getGameBoard().getId(), gameBoardMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        GameBoardMember updated = gameBoardMemberRepository.findById(gameBoardMember.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("수정 후");
        assertThat(updated.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(updated.getLevel()).isEqualTo(Level.A);
        assertThat(updated.getAgeGroup()).isNull();

        mockMvc.perform(get("/api/game-boards/{gameBoardId}", exercise.getGameBoard().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waitings[0].players[0].name").value("수정 후"))
                .andExpect(jsonPath("$.data.waitings[0].players[0].level").value("A조"));
    }

    @Test
    @DisplayName("다른 게임판 소속 명단은 수정할 수 없다")
    void updateMember_rejectsMemberFromOtherGameBoard() throws Exception {
        Exercise otherExercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 30)));
        GameBoardMember otherBoardMember = gameBoardMemberRepository.save(GameBoardMember.builder()
                .gameBoard(otherExercise.getGameBoard())
                .name("다른 선수")
                .gender(Gender.MALE)
                .level(Level.C)
                .shuttlecockSubmitted(false)
                .participating(true)
                .gameCount(0)
                .build());
        authenticate(gameHost);

        mockMvc.perform(patch("/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}",
                        exercise.getGameBoard().getId(), otherBoardMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("게임 진행자가 아니면 명단 정보를 수정할 수 없다")
    void updateMember_deniesNonGameHost() throws Exception {
        authenticate(otherMember);

        mockMvc.perform(patch("/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}",
                        exercise.getGameBoard().getId(), gameBoardMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(GameErrorCode.GAME_BOARD_ACCESS_DENIED.getCode()));

        assertThat(gameBoardMember.getName()).isEqualTo("수정 전");
    }

    @Test
    @DisplayName("필수값이 비어 있거나 정의되지 않은 한글값이면 400을 반환한다")
    void updateMember_rejectsInvalidInput() throws Exception {
        authenticate(gameHost);

        mockMvc.perform(patch("/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}",
                        exercise.getGameBoard().getId(), gameBoardMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"gender\":\"기타\",\"level\":\"프로\"}"))
                .andExpect(status().isBadRequest());

        assertThat(gameBoardMember.getName()).isEqualTo("수정 전");
    }

    private String validRequest() {
        return "{\"name\":\"수정 후\",\"gender\":\"여성\",\"level\":\"A조\",\"ageGroup\":\"20대\"}";
    }

    private void authenticate(Member member) {
        SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());
    }
}
