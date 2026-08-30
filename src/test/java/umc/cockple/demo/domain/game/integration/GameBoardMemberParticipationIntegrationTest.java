package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.GameCommandService;
import umc.cockple.demo.domain.game.service.command.model.GameCreateCommand;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("게임판 명단 참여 상태 변경 통합 테스트")
class GameBoardMemberParticipationIntegrationTest extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PartyAddrRepository partyAddrRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private GameBoardMemberRepository gameBoardMemberRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private GameCommandService gameCommandService;

    private Member gameHost;
    private Member otherMember;
    private Party party;
    private Exercise exercise;
    private GameBoardMember idleParticipatingMember;
    private GameBoardMember idleInactiveMember;
    private GameBoardMember waitingMember;
    private GameBoardMember playingMember;
    private GameBoardMember completedMember;

    @BeforeEach
    void setUp() {
        gameHost = memberRepository.save(MemberFixture.createMemberWithName(
                "게임 진행자", "진행자", Gender.FEMALE, Level.A, 74001L));
        otherMember = memberRepository.save(MemberFixture.createMemberWithName(
                "일반 회원", "일반", Gender.MALE, Level.B, 74002L));

        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        party = partyRepository.save(
                PartyFixture.createParty("참여 상태 테스트 모임", gameHost.getId(), partyAddr));
        exercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31)));

        idleParticipatingMember = saveMember("참여 선수", true);
        idleInactiveMember = saveMember("불참 선수", false);
        waitingMember = saveMember("대기 선수", true);
        playingMember = saveMember("진행 선수", true);
        completedMember = saveMember("완료 선수", true);
        saveGame(GameStatus.WAITING, waitingMember);
        saveGame(GameStatus.PLAYING, playingMember);
        saveGame(GameStatus.COMPLETED, completedMember);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHelper.clearAuthentication();
    }

    @Test
    @DisplayName("활성 게임에 없는 선수는 참여와 불참 상태를 변경할 수 있다")
    void changeParticipation_updatesBothDirections() throws Exception {
        authenticate(gameHost);

        changeParticipation(idleParticipatingMember, false)
                .andExpect(status().isOk());
        assertThat(idleParticipatingMember.getParticipating()).isFalse();

        changeParticipation(idleParticipatingMember, true)
                .andExpect(status().isOk());
        assertThat(idleParticipatingMember.getParticipating()).isTrue();

        changeParticipation(completedMember, false)
                .andExpect(status().isOk());
        assertThat(completedMember.getParticipating()).isFalse();
    }

    @Test
    @DisplayName("현재 값과 같은 참여 상태 요청은 성공한다")
    void changeParticipation_sameValueSucceeds() throws Exception {
        authenticate(gameHost);

        changeParticipation(idleInactiveMember, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(idleInactiveMember.getParticipating()).isFalse();
    }

    @Test
    @DisplayName("불참 상태의 선수는 새 대기 게임에 추가할 수 없다")
    void createGame_rejectsInactiveMember() {
        assertThatThrownBy(() -> gameCommandService.createGame(
                gameHost.getId(),
                new GameCreateCommand(
                        exercise.getGameBoard().getId(), List.of(idleInactiveMember.getId()))))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GameErrorCode.INACTIVE_GAME_PLAYER));
    }

    @Test
    @DisplayName("WAITING 게임 선수는 참여 해제할 수 없다")
    void changeParticipation_rejectsWaitingMemberDeactivation() throws Exception {
        authenticate(gameHost);

        changeParticipation(waitingMember, false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(GameErrorCode.ACTIVE_GAME_MEMBER_CANNOT_BE_INACTIVE.getCode()));

        assertThat(waitingMember.getParticipating()).isTrue();
    }

    @Test
    @DisplayName("PLAYING 게임 선수는 참여 해제할 수 없다")
    void changeParticipation_rejectsPlayingMemberDeactivation() throws Exception {
        authenticate(gameHost);

        changeParticipation(playingMember, false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(GameErrorCode.ACTIVE_GAME_MEMBER_CANNOT_BE_INACTIVE.getCode()));

        assertThat(playingMember.getParticipating()).isTrue();
    }

    @Test
    @DisplayName("다른 게임판 소속 명단의 참여 상태는 변경할 수 없다")
    void changeParticipation_rejectsMemberFromOtherGameBoard() throws Exception {
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

        mockMvc.perform(patch("/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}/participation",
                        exercise.getGameBoard().getId(), otherBoardMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participating\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("게임 진행자가 아니면 참여 상태를 변경할 수 없다")
    void changeParticipation_deniesNonGameHost() throws Exception {
        authenticate(otherMember);

        changeParticipation(idleParticipatingMember, false)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(GameErrorCode.GAME_BOARD_ACCESS_DENIED.getCode()));

        assertThat(idleParticipatingMember.getParticipating()).isTrue();
    }

    @Test
    @DisplayName("participating이 없으면 400을 반환한다")
    void changeParticipation_requiresParticipatingValue() throws Exception {
        authenticate(gameHost);

        mockMvc.perform(patch("/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}/participation",
                        exercise.getGameBoard().getId(), idleParticipatingMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions changeParticipation(
            GameBoardMember member, boolean participating) throws Exception {
        return mockMvc.perform(patch("/api/game-boards/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}/participation",
                        exercise.getGameBoard().getId(), member.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"participating\":" + participating + "}"));
    }

    private GameBoardMember saveMember(String name, boolean participating) {
        return gameBoardMemberRepository.save(GameBoardMember.builder()
                .gameBoard(exercise.getGameBoard())
                .name(name)
                .gender(Gender.MALE)
                .level(Level.D)
                .shuttlecockSubmitted(false)
                .participating(participating)
                .gameCount(0)
                .build());
    }

    private void saveGame(GameStatus status, GameBoardMember member) {
        Game game = Game.builder()
                .gameBoard(exercise.getGameBoard())
                .status(status)
                .waitingOrder(status == GameStatus.WAITING ? 1 : null)
                .build();
        game.addPlayer(GamePlayer.create(member, 0));
        gameRepository.save(game);
    }

    private void authenticate(Member member) {
        SecurityContextHelper.setAuthentication(member.getId(), member.getNickname());
    }
}
