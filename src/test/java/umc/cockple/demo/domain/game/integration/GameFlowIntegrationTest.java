package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.CourtStatus;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameBoardRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.GameCommandService;
import umc.cockple.demo.domain.game.service.command.GameCourtCommandService;
import umc.cockple.demo.domain.game.service.command.model.GameCourtMoveCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameDeleteCommand;
import umc.cockple.demo.domain.game.service.command.model.GameStartCommand;
import umc.cockple.demo.domain.game.service.command.model.GameToWaitingCommand;
import umc.cockple.demo.domain.game.service.command.result.GameDeleteResult;
import umc.cockple.demo.domain.game.service.query.GameBoardQueryService;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("게임 플로우 통합 테스트")
class GameFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private GameCommandService gameCommandService;
    @Autowired private GameCourtCommandService gameCourtCommandService;
    @Autowired private GameBoardQueryService gameBoardQueryService;

    @Autowired private GameBoardRepository gameBoardRepository;
    @Autowired private CourtRepository courtRepository;
    @Autowired private GameBoardMemberRepository gameBoardMemberRepository;
    @Autowired private GameRepository gameRepository;

    // 권한 검증이 아직 없으므로 임의의 요청자 ID 사용
    private static final Long ACTOR = 999L;

    @Test
    @DisplayName("대기 생성(#8) → 게임 시작(#4) → 보드 조회(#2) → 코트 이동(#3)이 실제 DB에서 연결된다")
    void gameFlow_createStartViewMove() {
        // --- setup: 게임판 + 코트 2개 + 명단 4명 ---
        GameBoard board = gameBoardRepository.save(GameBoard.create());
        Court court1 = courtRepository.save(Court.create(board, 1, "1번 코트"));
        Court court2 = courtRepository.save(Court.create(board, 2, "2번 코트"));
        List<Long> memberIds = saveMembers(board, "김A", "김B", "김C", "김D");

        // --- #8 게임 대기 생성 ---
        Long gameId = gameCommandService.createGame(ACTOR, new GameCreateCommand(board.getId(), memberIds));

        Game created = gameRepository.findById(gameId).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(created.getWaitingOrder()).isEqualTo(1);
        assertThat(created.getPlayers()).hasSize(4);

        GameBoardResult afterCreate = gameBoardQueryService.getBoard(ACTOR, board.getId());
        assertThat(afterCreate.courtCount()).isEqualTo(2);
        assertThat(afterCreate.courts())
                .allSatisfy(court -> assertThat(court.status()).isEqualTo(CourtStatus.EMPTY));
        assertThat(afterCreate.waitings())
                .extracting(GameBoardResult.WaitingView::gameId)
                .containsExactly(gameId);

        // --- #4 게임 시작: 1번 코트에 배치 ---
        gameCommandService.startGame(ACTOR, new GameStartCommand(board.getId(), gameId, court1.getId()));

        GameBoardResult afterStart = gameBoardQueryService.getBoard(ACTOR, board.getId());
        GameBoardResult.CourtView startedCourt = courtOf(afterStart, court1.getId());
        assertThat(startedCourt.status()).isEqualTo(CourtStatus.PLAYING);
        assertThat(startedCourt.game()).isNotNull();
        assertThat(startedCourt.game().gameId()).isEqualTo(gameId);
        assertThat(courtOf(afterStart, court2.getId()).status()).isEqualTo(CourtStatus.EMPTY);
        assertThat(afterStart.waitings()).isEmpty();

        // --- #3 코트 이동: 1번 코트 게임 → 2번(빈) 코트 ---
        gameCourtCommandService.moveCourt(ACTOR, new GameCourtMoveCommand(board.getId(), court1.getId(), 2));

        GameBoardResult afterMove = gameBoardQueryService.getBoard(ACTOR, board.getId());
        assertThat(courtOf(afterMove, court1.getId()).status()).isEqualTo(CourtStatus.EMPTY);
        GameBoardResult.CourtView movedCourt = courtOf(afterMove, court2.getId());
        assertThat(movedCourt.status()).isEqualTo(CourtStatus.PLAYING);
        assertThat(movedCourt.game().gameId()).isEqualTo(gameId);
    }

    @Test
    @DisplayName("대기 삭제(#6): 대기 게임을 삭제하면 남은 대기열 순서가 재정렬되고 restore 플레이어를 반환한다")
    void deleteWaitingGame_resequencesAndRestores() {
        // --- setup: 게임판 + 대기 게임 2개 ---
        GameBoard board = gameBoardRepository.save(GameBoard.create());
        List<Long> members1 = saveMembers(board, "김A", "김B");
        List<Long> members2 = saveMembers(board, "김C", "김D");

        Long game1 = gameCommandService.createGame(ACTOR, new GameCreateCommand(board.getId(), members1)); // 대기 1번
        Long game2 = gameCommandService.createGame(ACTOR, new GameCreateCommand(board.getId(), members2)); // 대기 2번

        // --- #6 대기 1번(game1) 삭제 + 복원 ---
        GameDeleteResult result = gameCommandService.deleteGame(
                ACTOR, new GameDeleteCommand(board.getId(), game1, true));

        assertThat(result.gameId()).isEqualTo(game1);
        assertThat(result.players())
                .extracting(GameDeleteResult.PlayerView::gameBoardMemberId)
                .containsExactly(members1.get(0), members1.get(1));

        // game1 삭제됨, game2가 대기 1번으로 재정렬
        assertThat(gameRepository.findById(game1)).isEmpty();
        GameBoardResult afterDelete = gameBoardQueryService.getBoard(ACTOR, board.getId());
        assertThat(afterDelete.waitings())
                .extracting(GameBoardResult.WaitingView::gameId)
                .containsExactly(game2);
        assertThat(afterDelete.waitings().get(0).waitingOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("대기열 이동(#7): 진행 게임을 완료 처리하고 같은 팀으로 새 대기 게임을 맨 앞에 만든다 (완료 흡수)")
    void moveToWaiting_completesAndRequeuesSameTeamAtFront() {
        // --- setup: 게임판 + 코트 1개 + 대기 게임 B, 진행 게임 A ---
        GameBoard board = gameBoardRepository.save(GameBoard.create());
        Court court1 = courtRepository.save(Court.create(board, 1, "1번 코트"));
        List<Long> membersB = saveMembers(board, "김B1", "김B2");
        List<Long> membersA = saveMembers(board, "김A1", "김A2");

        Long gameB = gameCommandService.createGame(ACTOR, new GameCreateCommand(board.getId(), membersB)); // 대기 1번
        Long gameA = gameCommandService.createGame(ACTOR, new GameCreateCommand(board.getId(), membersA)); // 대기 2번
        gameCommandService.startGame(ACTOR, new GameStartCommand(board.getId(), gameA, court1.getId()));   // A 진행 (B는 대기 1번)

        // --- #7 대기열 이동: A 완료 + 같은 팀 새 대기 게임 ---
        gameCommandService.moveGameToWaiting(ACTOR, new GameToWaitingCommand(board.getId(), gameA));

        // 원 게임 A는 완료 처리됨
        assertThat(gameRepository.findById(gameA).orElseThrow().getStatus()).isEqualTo(GameStatus.COMPLETED);

        GameBoardResult result = gameBoardQueryService.getBoard(ACTOR, board.getId());
        // 코트 비워짐
        assertThat(courtOf(result, court1.getId()).status()).isEqualTo(CourtStatus.EMPTY);
        // 대기열: 맨 앞 = A팀 새 게임(gameA 아님), 2번 = B
        assertThat(result.waitings()).hasSize(2);
        GameBoardResult.WaitingView front = result.waitings().get(0);
        assertThat(front.waitingOrder()).isEqualTo(1);
        assertThat(front.gameId()).isNotEqualTo(gameA); // 완료된 원 게임이 아니라 새로 만든 게임
        assertThat(front.players())
                .extracting(GameBoardResult.PlayerView::gameBoardMemberId)
                .containsExactlyElementsOf(membersA);
        GameBoardResult.WaitingView second = result.waitings().get(1);
        assertThat(second.gameId()).isEqualTo(gameB);
        assertThat(second.waitingOrder()).isEqualTo(2);

        // A 참여 멤버 게임횟수 +1 (완료), B는 아직 0
        membersA.forEach(memberId -> assertThat(
                gameBoardMemberRepository.findById(memberId).orElseThrow().getGameCount()).isEqualTo(1));
        membersB.forEach(memberId -> assertThat(
                gameBoardMemberRepository.findById(memberId).orElseThrow().getGameCount()).isZero());
    }

    private List<Long> saveMembers(GameBoard board, String... names) {
        return Arrays.stream(names)
                .map(name -> gameBoardMemberRepository.save(GameBoardMember.builder()
                        .gameBoard(board)
                        .name(name)
                        .gender(Gender.MALE)
                        .level(Level.A)
                        .shuttlecockSubmitted(false)
                        .participating(true)
                        .gameCount(0)
                        .build()).getId())
                .toList();
    }

    private GameBoardResult.CourtView courtOf(GameBoardResult board, Long courtId) {
        return board.courts().stream()
                .filter(court -> court.courtId().equals(courtId))
                .findFirst()
                .orElseThrow();
    }
}
