package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.domain.GamePlayer;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.repository.GameBoardRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.command.GameBoardMemberCommandService;
import umc.cockple.demo.domain.game.service.command.GameCommandService;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberParticipationCommand;
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
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("게임판 참여 상태-게임 배정 동시성 통합 테스트")
class GameParticipationAssignmentConcurrencyIntegrationTest extends IntegrationTestBase {

    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private GameBoardRepository gameBoardRepository;
    @Autowired private GameBoardMemberRepository gameBoardMemberRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private GameCommandService gameCommandService;
    @Autowired private GameBoardMemberCommandService gameBoardMemberCommandService;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private PartyAddrRepository partyAddrRepository;

    private TransactionTemplate transactionTemplate;
    private Long gameHostId;
    private Long gameBoardId;
    private Long gameBoardMemberId;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        Member gameHost = memberRepository.save(MemberFixture.createMemberWithName(
                "게임 진행자", "진행자", Gender.FEMALE, Level.A, 75001L));
        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        Party party = partyRepository.save(
                PartyFixture.createParty("게임 배정 동시성 테스트", gameHost.getId(), partyAddr));
        Exercise exercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31)));
        GameBoardMember gameBoardMember = gameBoardMemberRepository.save(GameBoardMember.builder()
                .gameBoard(exercise.getGameBoard())
                .name("동시성 선수")
                .gender(Gender.MALE)
                .level(Level.D)
                .shuttlecockSubmitted(false)
                .participating(true)
                .gameCount(0)
                .build());

        gameHostId = gameHost.getId();
        gameBoardId = exercise.getGameBoard().getId();
        gameBoardMemberId = gameBoardMember.getId();
    }

    @AfterEach
    void tearDown() {
        gameRepository.deleteAll();
        gameBoardMemberRepository.deleteAll();
        exerciseRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("참여 해제가 게임판을 먼저 잠그면 게임 생성은 대기한 뒤 불참 선수 오류로 실패한다")
    void gameCreationWaitsForDeactivationAndRejectsInactiveMember() throws Exception {
        CountDownLatch deactivationLocked = new CountDownLatch(1);
        CountDownLatch allowDeactivationCommit = new CountDownLatch(1);
        CountDownLatch creationAttempted = new CountDownLatch(1);
        CountDownLatch creationFinished = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> deactivationFuture = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        gameBoardRepository.findByIdForUpdate(gameBoardId).orElseThrow();
                        GameBoardMember member = gameBoardMemberRepository.findById(gameBoardMemberId).orElseThrow();
                        member.changeParticipation(false);
                        deactivationLocked.countDown();
                        await(allowDeactivationCommit);
                    }));

            assertThat(deactivationLocked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<String> creationFuture = executor.submit(() -> {
                creationAttempted.countDown();
                try {
                    gameCommandService.createGame(
                            gameHostId, new GameCreateCommand(gameBoardId, List.of(gameBoardMemberId)));
                    return null;
                } catch (GameException exception) {
                    return exception.getErrorReason().getCode();
                } finally {
                    creationFinished.countDown();
                }
            });

            assertThat(creationAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(creationFinished.await(300, TimeUnit.MILLISECONDS)).isFalse();

            allowDeactivationCommit.countDown();
            deactivationFuture.get(5, TimeUnit.SECONDS);
            assertThat(creationFuture.get(5, TimeUnit.SECONDS))
                    .isEqualTo(GameErrorCode.INACTIVE_GAME_PLAYER.getCode());
        } finally {
            allowDeactivationCommit.countDown();
            executor.shutdownNow();
        }

        assertThat(gameRepository.countByGameBoardIdAndStatus(gameBoardId, GameStatus.WAITING)).isZero();
        assertThat(gameBoardMemberRepository.findById(gameBoardMemberId).orElseThrow().getParticipating()).isFalse();
    }

    @Test
    @DisplayName("게임 배정이 게임판을 먼저 잠그면 참여 해제는 대기한 뒤 활성 게임 오류로 실패한다")
    void deactivationWaitsForAssignmentAndRejectsActiveMember() throws Exception {
        CountDownLatch assignmentLocked = new CountDownLatch(1);
        CountDownLatch allowAssignmentCommit = new CountDownLatch(1);
        CountDownLatch deactivationAttempted = new CountDownLatch(1);
        CountDownLatch deactivationFinished = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> assignmentFuture = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        GameBoard gameBoard = gameBoardRepository.findByIdForUpdate(gameBoardId).orElseThrow();
                        GameBoardMember member = gameBoardMemberRepository.findById(gameBoardMemberId).orElseThrow();
                        Game waitingGame = Game.createWaiting(gameBoard, 1);
                        waitingGame.addPlayer(GamePlayer.create(member, 0));
                        gameRepository.saveAndFlush(waitingGame);
                        assignmentLocked.countDown();
                        await(allowAssignmentCommit);
                    }));

            assertThat(assignmentLocked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<String> deactivationFuture = executor.submit(() -> {
                deactivationAttempted.countDown();
                try {
                    gameBoardMemberCommandService.changeParticipation(
                            gameHostId,
                            new GameBoardMemberParticipationCommand(
                                    gameBoardId, gameBoardMemberId, false));
                    return null;
                } catch (GameException exception) {
                    return exception.getErrorReason().getCode();
                } finally {
                    deactivationFinished.countDown();
                }
            });

            assertThat(deactivationAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(deactivationFinished.await(300, TimeUnit.MILLISECONDS)).isFalse();

            allowAssignmentCommit.countDown();
            assignmentFuture.get(5, TimeUnit.SECONDS);
            assertThat(deactivationFuture.get(5, TimeUnit.SECONDS))
                    .isEqualTo(GameErrorCode.ACTIVE_GAME_MEMBER_CANNOT_BE_INACTIVE.getCode());
        } finally {
            allowAssignmentCommit.countDown();
            executor.shutdownNow();
        }

        assertThat(gameBoardMemberRepository.findById(gameBoardMemberId).orElseThrow().getParticipating()).isTrue();
        assertThat(gameRepository.countByGameBoardIdAndStatus(gameBoardId, GameStatus.WAITING)).isEqualTo(1L);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("동시성 테스트 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("동시성 테스트 대기 중 인터럽트가 발생했습니다.", exception);
        }
    }
}
