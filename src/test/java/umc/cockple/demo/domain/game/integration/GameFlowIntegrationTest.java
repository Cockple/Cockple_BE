package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.CourtStatus;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.GameCommandService;
import umc.cockple.demo.domain.game.service.command.GameCourtCommandService;
import umc.cockple.demo.domain.game.service.command.model.GameCompleteCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCourtMoveCommand;
import umc.cockple.demo.domain.game.service.command.model.GameCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameDeleteCommand;
import umc.cockple.demo.domain.game.service.command.model.GameStartCommand;
import umc.cockple.demo.domain.game.service.command.model.GameToWaitingCommand;
import umc.cockple.demo.domain.game.service.command.result.GameDeleteResult;
import umc.cockple.demo.domain.game.service.query.GameBoardQueryService;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("게임 플로우 통합 테스트")
class GameFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private GameCommandService gameCommandService;
    @Autowired private GameCourtCommandService gameCourtCommandService;
    @Autowired private GameBoardQueryService gameBoardQueryService;

    @Autowired private CourtRepository courtRepository;
    @Autowired private GameBoardMemberRepository gameBoardMemberRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PartyAddrRepository partyAddrRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private ExerciseRepository exerciseRepository;

    // 게임 진행자만 조작할 수 있으므로 운동의 게임 진행자를 요청자로 사용한다.
    private Long actor;
    private GameBoard board;

    @BeforeEach
    void setUp() {
        Member gameHost = memberRepository.save(MemberFixture.createMemberWithName(
                "게임 진행자", "진행자", Gender.FEMALE, Level.A, 75001L));
        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        Party party = partyRepository.save(
                PartyFixture.createParty("게임 플로우 테스트 모임", gameHost.getId(), partyAddr));
        Exercise exercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31)));

        actor = gameHost.getId();
        board = exercise.getGameBoard();
    }

    @Test
    @DisplayName("대기 생성(#8) → 게임 시작(#4) → 보드 조회(#2) → 코트 이동(#3)이 실제 DB에서 연결된다")
    void gameFlow_createStartViewMove() {
        // --- setup: 게임판 기본 코트 2개 + 명단 4명 ---
        List<Court> courts = courtRepository.findByGameBoardIdOrderByCourtNoAsc(board.getId());
        Court court1 = courts.get(0);
        Court court2 = courts.get(1);
        List<Long> memberIds = saveMembers(board, "김A", "김B", "김C", "김D");

        // --- #8 게임 대기 생성 ---
        Long gameId = gameCommandService.createGame(actor, new GameCreateCommand(board.getId(), memberIds));

        Game created = gameRepository.findById(gameId).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(created.getWaitingOrder()).isEqualTo(1);
        assertThat(created.getPlayers()).hasSize(4);

        GameBoardResult afterCreate = gameBoardQueryService.getBoard(actor, board.getId());
        assertThat(afterCreate.isGameHost()).isTrue();
        assertThat(afterCreate.courtCount()).isEqualTo(2);
        assertThat(afterCreate.courts())
                .allSatisfy(court -> assertThat(court.status()).isEqualTo(CourtStatus.EMPTY));
        assertThat(afterCreate.waitings())
                .extracting(GameBoardResult.WaitingView::gameId)
                .containsExactly(gameId);

        // --- #4 게임 시작: 1번 코트에 배치 ---
        gameCommandService.startGame(actor, new GameStartCommand(board.getId(), gameId, court1.getId()));

        GameBoardResult afterStart = gameBoardQueryService.getBoard(actor, board.getId());
        GameBoardResult.CourtView startedCourt = courtOf(afterStart, court1.getId());
        assertThat(startedCourt.status()).isEqualTo(CourtStatus.PLAYING);
        assertThat(startedCourt.game()).isNotNull();
        assertThat(startedCourt.game().gameId()).isEqualTo(gameId);
        assertThat(courtOf(afterStart, court2.getId()).status()).isEqualTo(CourtStatus.EMPTY);
        assertThat(afterStart.waitings()).isEmpty();

        // --- #3 코트 이동: 1번 코트 게임 → 2번(빈) 코트 ---
        gameCourtCommandService.moveCourt(actor, new GameCourtMoveCommand(board.getId(), court1.getId(), court2.getCourtNo()));

        GameBoardResult afterMove = gameBoardQueryService.getBoard(actor, board.getId());
        assertThat(courtOf(afterMove, court1.getId()).status()).isEqualTo(CourtStatus.EMPTY);
        GameBoardResult.CourtView movedCourt = courtOf(afterMove, court2.getId());
        assertThat(movedCourt.status()).isEqualTo(CourtStatus.PLAYING);
        assertThat(movedCourt.game().gameId()).isEqualTo(gameId);
    }

    @Test
    @DisplayName("대기 삭제(#6): 대기 게임을 삭제하면 남은 대기열 순서가 재정렬되고 restore 플레이어를 반환한다")
    void deleteWaitingGame_resequencesAndRestores() {
        // --- setup: 대기 게임 2개 ---
        List<Long> members1 = saveMembers(board, "김A", "김B");
        List<Long> members2 = saveMembers(board, "김C", "김D");

        Long game1 = gameCommandService.createGame(actor, new GameCreateCommand(board.getId(), members1)); // 대기 1번
        Long game2 = gameCommandService.createGame(actor, new GameCreateCommand(board.getId(), members2)); // 대기 2번

        // --- #6 대기 1번(game1) 삭제 + 복원 ---
        GameDeleteResult result = gameCommandService.deleteGame(
                actor, new GameDeleteCommand(board.getId(), game1, true));

        assertThat(result.gameId()).isEqualTo(game1);
        assertThat(result.players())
                .extracting(GameDeleteResult.PlayerView::gameBoardMemberId)
                .containsExactly(members1.get(0), members1.get(1));

        // game1 삭제됨, game2가 대기 1번으로 재정렬
        assertThat(gameRepository.findById(game1)).isEmpty();
        GameBoardResult afterDelete = gameBoardQueryService.getBoard(actor, board.getId());
        assertThat(afterDelete.waitings())
                .extracting(GameBoardResult.WaitingView::gameId)
                .containsExactly(game2);
        assertThat(afterDelete.waitings().get(0).waitingOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("대기열 이동(#7): 진행 게임을 기록 없이 같은 게임 그대로 대기열 맨 앞으로 되돌리고 코트를 비운다")
    void moveToWaiting_returnsSameGameToFrontWithoutRecording() {
        // --- setup: 기본 코트(1번) + 대기 게임 B, 진행 게임 A ---
        Court court1 = courtRepository.findByGameBoardIdOrderByCourtNoAsc(board.getId()).get(0);
        List<Long> membersB = saveMembers(board, "김B1", "김B2");
        List<Long> membersA = saveMembers(board, "김A1", "김A2");

        Long gameB = gameCommandService.createGame(actor, new GameCreateCommand(board.getId(), membersB)); // 대기 1번
        Long gameA = gameCommandService.createGame(actor, new GameCreateCommand(board.getId(), membersA)); // 대기 2번
        gameCommandService.startGame(actor, new GameStartCommand(board.getId(), gameA, court1.getId()));   // A 진행 (B는 대기 1번)

        // --- #7 대기열 이동: A를 기록 없이 대기열 맨 앞으로 ---
        gameCommandService.moveGameToWaiting(actor, new GameToWaitingCommand(board.getId(), gameA));

        // 원 게임 A는 완료가 아니라 다시 대기 상태 (기록/완료시각 없음)
        Game movedA = gameRepository.findById(gameA).orElseThrow();
        assertThat(movedA.getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(movedA.getCompletedAt()).isNull();
        assertThat(movedA.getStartedAt()).isNull();

        GameBoardResult result = gameBoardQueryService.getBoard(actor, board.getId());
        // 코트 비워짐
        assertThat(courtOf(result, court1.getId()).status()).isEqualTo(CourtStatus.EMPTY);
        // 대기열: 맨 앞 = A(같은 게임), 2번 = B
        assertThat(result.waitings()).hasSize(2);
        GameBoardResult.WaitingView front = result.waitings().get(0);
        assertThat(front.waitingOrder()).isEqualTo(1);
        assertThat(front.gameId()).isEqualTo(gameA); // 새 게임이 아니라 원 게임 그대로
        assertThat(front.players())
                .extracting(GameBoardResult.PlayerView::gameBoardMemberId)
                .containsExactlyElementsOf(membersA);
        GameBoardResult.WaitingView second = result.waitings().get(1);
        assertThat(second.gameId()).isEqualTo(gameB);
        assertThat(second.waitingOrder()).isEqualTo(2);

        // 기록으로 남지 않으므로 아무도 게임횟수가 늘지 않는다
        membersA.forEach(memberId -> assertThat(
                gameBoardMemberRepository.findById(memberId).orElseThrow().getGameCount()).isZero());
        membersB.forEach(memberId -> assertThat(
                gameBoardMemberRepository.findById(memberId).orElseThrow().getGameCount()).isZero());
    }

    @Test
    @DisplayName("게임 완료: 진행 게임을 완료 처리하면 코트가 비고 참여자 게임횟수가 +1 되며 시작시각이 보존된다")
    void completeGame_emptiesCourtAndIncrementsCount() {
        // --- setup: 기본 코트(1번) + 진행 게임 A ---
        Court court1 = courtRepository.findByGameBoardIdOrderByCourtNoAsc(board.getId()).get(0);
        List<Long> membersA = saveMembers(board, "김A1", "김A2");

        Long gameA = gameCommandService.createGame(actor, new GameCreateCommand(board.getId(), membersA));
        gameCommandService.startGame(actor, new GameStartCommand(board.getId(), gameA, court1.getId()));

        // --- 게임 완료 ---
        gameCommandService.completeGame(actor, new GameCompleteCommand(board.getId(), gameA));

        // 완료 처리 + 완료시각 저장, 경과시각 계산용 시작시각 보존
        Game completed = gameRepository.findById(gameA).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getStartedAt()).isNotNull();

        // 코트가 비고, 완료 게임은 진행/대기 보드에 나타나지 않는다
        GameBoardResult result = gameBoardQueryService.getBoard(actor, board.getId());
        assertThat(courtOf(result, court1.getId()).status()).isEqualTo(CourtStatus.EMPTY);
        assertThat(result.waitings()).isEmpty();

        // 참여 멤버 게임횟수 +1
        membersA.forEach(memberId -> assertThat(
                gameBoardMemberRepository.findById(memberId).orElseThrow().getGameCount()).isEqualTo(1));
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
