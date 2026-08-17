package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseParticipationCommandService;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("운동 동시 참가")
class ExerciseParticipationConcurrencyIntegrationTest extends IntegrationTestBase {

    @Autowired ExerciseParticipationCommandService participationCommandService;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private Member participant;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        Member owner = memberRepository.save(
                MemberFixture.createMember("모임장", Gender.MALE, Level.A, 95001L));
        participant = memberRepository.save(
                MemberFixture.createMember("동시 참가자", Gender.FEMALE, Level.B, 95002L));

        PartyAddr address = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "동시참가구"));
        Party party = partyRepository.save(
                PartyFixture.createParty("동시 참가 모임", owner.getId(), address));

        memberPartyRepository.save(MemberFixture.createMemberParty(party, owner, Role.PARTY_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, participant, Role.PARTY_MEMBER));
        party.addLevel(Gender.FEMALE, Level.B);
        partyRepository.save(party);

        exercise = exerciseRepository.save(
                ExerciseFixture.createExercise(party, LocalDate.now().plusDays(7)));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM game_board_member");
        memberExerciseRepository.deleteAll();
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 두 번 참가해도 한 건만 저장되고 패배 요청은 이미 참여 오류를 받는다")
    void concurrentJoin_persistsOnceAndReturnsDomainConflict() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<JoinAttempt> join = () -> {
            ready.countDown();
            start.await();
            try {
                participationCommandService.joinExercise(exercise.getId(), participant.getId());
                return JoinAttempt.success();
            } catch (ExerciseException exception) {
                return JoinAttempt.domainFailure((ExerciseErrorCode) exception.getCode());
            } catch (Throwable throwable) {
                return JoinAttempt.unexpectedFailure(throwable);
            }
        };

        try {
            Future<JoinAttempt> first = executor.submit(join);
            Future<JoinAttempt> second = executor.submit(join);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<JoinAttempt> attempts = List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS));

            assertThat(attempts).filteredOn(JoinAttempt::succeeded).hasSize(1);
            assertThat(attempts)
                    .filteredOn(attempt -> attempt.errorCode() == ExerciseErrorCode.ALREADY_JOINED_EXERCISE)
                    .hasSize(1);
            assertThat(attempts).allSatisfy(attempt -> assertThat(attempt.unexpected()).isNull());
            assertThat(memberExerciseRepository.count()).isEqualTo(1);
            assertThat(rosterCount()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private int rosterCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM game_board_member WHERE game_board_id = ?",
                Integer.class,
                exercise.getGameBoard().getId());
        return count == null ? 0 : count;
    }

    private record JoinAttempt(
            boolean succeeded,
            ExerciseErrorCode errorCode,
            Throwable unexpected
    ) {
        private static JoinAttempt success() {
            return new JoinAttempt(true, null, null);
        }

        private static JoinAttempt domainFailure(ExerciseErrorCode errorCode) {
            return new JoinAttempt(false, errorCode, null);
        }

        private static JoinAttempt unexpectedFailure(Throwable throwable) {
            return new JoinAttempt(false, null, throwable);
        }
    }
}
