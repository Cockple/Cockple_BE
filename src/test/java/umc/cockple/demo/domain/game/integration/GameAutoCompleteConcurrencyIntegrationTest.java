package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import umc.cockple.demo.support.IntegrationTestConfig;
import umc.cockple.demo.domain.game.domain.*;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.*;
import umc.cockple.demo.domain.game.service.command.GameAutoCompleteBoardService;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.global.enums.*;
import umc.cockple.demo.global.config.QuerydslConfig;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.Tag("integration")
@DataJpaTest
@ActiveProfiles("integrationtest")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({IntegrationTestConfig.class, QuerydslConfig.class, GameAutoCompleteBoardService.class, GameBoardReader.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GameAutoCompleteConcurrencyIntegrationTest {
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired GameBoardRepository boards;
    @Autowired GameRepository games;
    @Autowired GameBoardMemberRepository members;
    @Autowired GameAutoCompleteBoardService service;

    @Test
    void skipsGameCompletedWhileWaitingForBoardLock() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long[] ids = tx.execute(status -> {
            GameBoard board = boards.save(GameBoard.create());
            GameBoardMember member = members.save(GameBoardMember.builder().gameBoard(board)
                    .name("player").gender(Gender.MALE).level(Level.A).participating(true)
                    .shuttlecockSubmitted(false).gameCount(0).build());
            Game game = Game.createWaiting(board, 1);
            game.start(board.getCourts().get(0), LocalDateTime.now().minusMinutes(40));
            game.addPlayer(GamePlayer.create(member, 0));
            games.saveAndFlush(game);
            return new Long[]{board.getId(), game.getId(), member.getId()};
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch attempted = new CountDownLatch(1);
        try {
            tx.executeWithoutResult(status -> {
                GameBoard board = boards.findByIdForUpdate(ids[0]).orElseThrow();
                Game game = games.findById(ids[1]).orElseThrow();
                game.complete(LocalDateTime.now());
                game.getPlayers().forEach(player -> player.getGameBoardMember().increaseGameCount());
                board.advanceRevision();
                Future<Integer> completion = executor.submit(() -> {
                    attempted.countDown();
                    return service.completeStaleGames(ids[0], LocalDateTime.now().minusMinutes(30));
                });
                try {
                    assertThat(attempted.await(5, TimeUnit.SECONDS)).isTrue();
                    org.junit.jupiter.api.Assertions.assertThrows(TimeoutException.class,
                            () -> completion.get(300, TimeUnit.MILLISECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                pending = completion;
            });
            assertThat(pending.get(10, TimeUnit.SECONDS)).isZero();
            assertThat(members.findById(ids[2]).orElseThrow().getGameCount()).isEqualTo(1);
            assertThat(games.findById(ids[1]).orElseThrow().getStatus()).isEqualTo(GameStatus.COMPLETED);
            assertThat(boards.findById(ids[0]).orElseThrow().getRevision()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                games.deleteById(ids[1]);
                members.deleteById(ids[2]);
                boards.deleteById(ids[0]);
            });
        }
    }
    private Future<Integer> pending;
}
