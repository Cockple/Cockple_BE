package umc.cockple.demo.domain.game.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.service.command.GameBoardMemberCommandService;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberShuttlecockSubmissionCommand;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Import(GameBoardMemberShuttlecockSubmissionConcurrencyIntegrationTest.EventRecorderConfig.class)
@DisplayName("게임판 명단 셔틀콕 제출 상태 변경 동시성 통합 테스트")
class GameBoardMemberShuttlecockSubmissionConcurrencyIntegrationTest extends IntegrationTestBase {

    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private GameBoardMemberCommandService gameBoardMemberCommandService;
    @Autowired private GameBoardMemberRepository gameBoardMemberRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private PartyAddrRepository partyAddrRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private EventRecorder eventRecorder;

    private TransactionTemplate transactionTemplate;
    private Long gameHostId;
    private Long gameBoardId;
    private Long gameBoardMemberId;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        eventRecorder.reset();

        Member gameHost = memberRepository.save(MemberFixture.createMemberWithName(
                "게임 진행자", "진행자", Gender.FEMALE, Level.A, 76001L));
        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        Party party = partyRepository.save(
                PartyFixture.createParty("셔틀콕 제출 동시성 테스트", gameHost.getId(), partyAddr));
        Exercise exercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31)));
        GameBoardMember gameBoardMember = gameBoardMemberRepository.save(GameBoardMember.builder()
                .gameBoard(exercise.getGameBoard())
                .name("선수")
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
        gameBoardMemberRepository.deleteAll();
        exerciseRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("동일한 제출 요청은 앞선 트랜잭션 커밋을 기다린 뒤 변경 이벤트를 한 번만 발행한다")
    void concurrentSameValueRequestsPublishOneChangeEvent() throws Exception {
        CountDownLatch firstChanged = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        CountDownLatch secondAttempted = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        GameBoardMemberShuttlecockSubmissionCommand command =
                new GameBoardMemberShuttlecockSubmissionCommand(
                        gameBoardId, gameBoardMemberId, true);

        try {
            Future<?> firstRequest = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        gameBoardMemberCommandService.changeShuttlecockSubmission(gameHostId, command);
                        firstChanged.countDown();
                        await(allowFirstCommit);
                    }));

            assertThat(firstChanged.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> secondRequest = executor.submit(() -> {
                secondAttempted.countDown();
                try {
                    gameBoardMemberCommandService.changeShuttlecockSubmission(gameHostId, command);
                } finally {
                    secondFinished.countDown();
                }
            });

            assertThat(secondAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondFinished.await(300, TimeUnit.MILLISECONDS)).isFalse();

            allowFirstCommit.countDown();
            firstRequest.get(5, TimeUnit.SECONDS);
            secondRequest.get(5, TimeUnit.SECONDS);
        } finally {
            allowFirstCommit.countDown();
            executor.shutdownNow();
        }

        assertThat(gameBoardMemberRepository.findById(gameBoardMemberId).orElseThrow()
                .getShuttlecockSubmitted()).isTrue();
        assertThat(eventRecorder.membersOnlyEventCount()).isOne();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트 대기가 중단되었습니다.", e);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EventRecorderConfig {

        @Bean
        EventRecorder eventRecorder() {
            return new EventRecorder();
        }
    }

    static class EventRecorder {

        private final AtomicInteger membersOnlyEventCount = new AtomicInteger();

        @EventListener
        public void record(GameBoardMembersChangedEvent event) {
            if (!event.includeBoardSnapshot()) {
                membersOnlyEventCount.incrementAndGet();
            }
        }

        int membersOnlyEventCount() {
            return membersOnlyEventCount.get();
        }

        void reset() {
            membersOnlyEventCount.set(0);
        }
    }
}
