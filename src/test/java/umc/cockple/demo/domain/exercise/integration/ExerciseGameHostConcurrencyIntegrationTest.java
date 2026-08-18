package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.exception.ExerciseErrorCode;
import umc.cockple.demo.domain.exercise.exception.ExerciseException;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGameHostCommandService;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGameHostRecoveryService;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGameHostChangeCommand;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExerciseGameHostConcurrencyIntegrationTest extends IntegrationTestBase {

    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ExerciseGameHostCommandService exerciseGameHostCommandService;
    @Autowired ExerciseGameHostRecoveryService exerciseGameHostRecoveryService;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;

    private TransactionTemplate transactionTemplate;
    private Member manager;
    private Member targetMember;
    private Party party;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        manager = memberRepository.save(
                MemberFixture.createMember("모임장", Gender.MALE, Level.A, 4001L));
        targetMember = memberRepository.save(
                MemberFixture.createMember("진행자 후보", Gender.FEMALE, Level.B, 4002L));

        PartyAddr partyAddr = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "송파구"));
        party = partyRepository.save(
                PartyFixture.createParty("진행자 동시성 테스트 모임", manager.getId(), partyAddr));

        memberPartyRepository.save(
                MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER));
        memberPartyRepository.save(
                MemberFixture.createMemberParty(party, targetMember, Role.PARTY_MEMBER));
        exercise = exerciseRepository.save(
                ExerciseFixture.createExerciseWithAddr(party, LocalDate.of(2099, 12, 31)));
    }

    @AfterEach
    void tearDown() {
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("PATCH가 membership을 먼저 잠그면 이탈은 대기한 뒤 진행자를 모임장으로 복구한다")
    void departureWaitsForPatchAndRestoresOwner() throws Exception {
        CountDownLatch patchLocked = new CountDownLatch(1);
        CountDownLatch allowPatchToFinish = new CountDownLatch(1);
        CountDownLatch departureAttempted = new CountDownLatch(1);
        CountDownLatch departureFinished = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> patchFuture = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                memberPartyRepository.findByPartyIdAndMemberIdAndStatusForUpdate(
                                party.getId(), targetMember.getId(), MemberPartyStatus.ACTIVE)
                        .orElseThrow();
                patchLocked.countDown();
                await(allowPatchToFinish);

                exerciseGameHostCommandService.changeGameHost(
                        exercise.getId(),
                        manager.getId(),
                        new ExerciseGameHostChangeCommand(targetMember.getId()));
            }));

            assertThat(patchLocked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> departureFuture = executor.submit(() -> {
                departureAttempted.countDown();
                try {
                    transactionTemplate.executeWithoutResult(status -> removeMembershipAndRecover());
                } finally {
                    departureFinished.countDown();
                }
            });

            assertThat(departureAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(departureFinished.await(300, TimeUnit.MILLISECONDS)).isFalse();

            allowPatchToFinish.countDown();
            patchFuture.get(5, TimeUnit.SECONDS);
            departureFuture.get(5, TimeUnit.SECONDS);
        } finally {
            allowPatchToFinish.countDown();
            executor.shutdownNow();
        }

        assertThat(memberPartyRepository.existsByPartyIdAndMemberId(
                party.getId(), targetMember.getId())).isFalse();
        assertThat(exerciseRepository.findById(exercise.getId()).orElseThrow().getGameHostId())
                .isEqualTo(manager.getId());
    }

    @Test
    @DisplayName("membership 이탈이 먼저 완료되면 PATCH는 후보 오류로 실패한다")
    void patchFailsAfterDepartureCommitted() {
        transactionTemplate.executeWithoutResult(status -> removeMembershipAndRecover());

        assertThatThrownBy(() -> exerciseGameHostCommandService.changeGameHost(
                exercise.getId(),
                manager.getId(),
                new ExerciseGameHostChangeCommand(targetMember.getId())))
                .isInstanceOf(ExerciseException.class)
                .satisfies(exception -> assertThat(((ExerciseException) exception).getCode())
                        .isEqualTo(ExerciseErrorCode.INVALID_GAME_HOST_CANDIDATE));

        assertThat(exerciseRepository.findById(exercise.getId()).orElseThrow().getGameHostId())
                .isEqualTo(manager.getId());
    }

    private void removeMembershipAndRecover() {
        MemberParty memberParty = memberPartyRepository.findByPartyIdAndMemberIdForUpdate(
                        party.getId(), targetMember.getId())
                .orElseThrow();
        memberPartyRepository.delete(memberParty);
        memberPartyRepository.flush();
        exerciseGameHostRecoveryService.recoverAfterPartyMemberLeft(
                party.getId(), targetMember.getId());
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
