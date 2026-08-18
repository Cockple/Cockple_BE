package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGameHostRecoveryService;
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

import static org.assertj.core.api.Assertions.assertThat;

class ExerciseGameHostRecoveryIntegrationTest extends IntegrationTestBase {

    @Autowired ExerciseGameHostRecoveryService exerciseGameHostRecoveryService;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;

    private Member ownerA;
    private Member ownerB;
    private Member departingMember;
    private Member otherMember;
    private Party partyA;
    private Party partyB;
    private Exercise partyADepartingHost1;
    private Exercise partyADepartingHost2;
    private Exercise partyAOtherHost;
    private Exercise partyBDepartingHost;
    private Exercise partyBOwnerHost;

    @BeforeEach
    void setUp() {
        ownerA = memberRepository.save(
                MemberFixture.createMember("A 모임장", Gender.MALE, Level.A, 3001L));
        ownerB = memberRepository.save(
                MemberFixture.createMember("B 모임장", Gender.FEMALE, Level.B, 3002L));
        departingMember = memberRepository.save(
                MemberFixture.createMember("이탈 회원", Gender.MALE, Level.C, 3003L));
        otherMember = memberRepository.save(
                MemberFixture.createMember("다른 진행자", Gender.FEMALE, Level.D, 3004L));

        PartyAddr addrA = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "강남구"));
        PartyAddr addrB = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "서초구"));
        partyA = partyRepository.save(PartyFixture.createParty("A 모임", ownerA.getId(), addrA));
        partyB = partyRepository.save(PartyFixture.createParty("B 모임", ownerB.getId(), addrB));

        memberPartyRepository.save(MemberFixture.createMemberParty(partyA, ownerA, Role.PARTY_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(partyB, ownerB, Role.PARTY_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(partyA, departingMember, Role.PARTY_MEMBER));
        memberPartyRepository.save(MemberFixture.createMemberParty(partyB, departingMember, Role.PARTY_MEMBER));
        memberPartyRepository.save(MemberFixture.createMemberParty(partyA, otherMember, Role.PARTY_MEMBER));

        partyADepartingHost1 = saveExercise(partyA, departingMember.getId(), LocalDate.of(2099, 1, 1));
        partyADepartingHost2 = saveExercise(partyA, departingMember.getId(), LocalDate.of(2099, 1, 2));
        partyAOtherHost = saveExercise(partyA, otherMember.getId(), LocalDate.of(2099, 1, 3));
        partyBDepartingHost = saveExercise(partyB, departingMember.getId(), LocalDate.of(2099, 1, 4));
        partyBOwnerHost = saveExercise(partyB, ownerB.getId(), LocalDate.of(2099, 1, 5));
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
    @DisplayName("모임 이탈 시 해당 모임에서 이탈 회원이 진행자인 운동만 모임장으로 복구한다")
    void recoversOnlyDepartedHostsInParty() {
        int recoveredCount = exerciseGameHostRecoveryService
                .recoverAfterPartyMemberLeft(partyA.getId(), departingMember.getId());

        assertThat(recoveredCount).isEqualTo(2);
        assertGameHost(partyADepartingHost1, ownerA);
        assertGameHost(partyADepartingHost2, ownerA);
        assertGameHost(partyAOtherHost, otherMember);
        assertGameHost(partyBDepartingHost, departingMember);
        assertGameHost(partyBOwnerHost, ownerB);
    }

    @Test
    @DisplayName("회원 탈퇴 시 모든 모임에서 탈퇴 회원이 진행자인 운동을 각 모임장으로 복구한다")
    void recoversWithdrawnHostAcrossParties() {
        int recoveredCount = exerciseGameHostRecoveryService
                .recoverAfterMemberWithdrawn(departingMember.getId());

        assertThat(recoveredCount).isEqualTo(3);
        assertGameHost(partyADepartingHost1, ownerA);
        assertGameHost(partyADepartingHost2, ownerA);
        assertGameHost(partyAOtherHost, otherMember);
        assertGameHost(partyBDepartingHost, ownerB);
        assertGameHost(partyBOwnerHost, ownerB);
    }

    private Exercise saveExercise(Party party, Long gameHostId, LocalDate date) {
        Exercise exercise = ExerciseFixture.createExerciseWithAddr(party, date);
        ReflectionTestUtils.setField(exercise, "gameHostId", gameHostId);
        return exerciseRepository.save(exercise);
    }

    private void assertGameHost(Exercise exercise, Member expectedGameHost) {
        assertThat(exerciseRepository.findById(exercise.getId()).orElseThrow().getGameHostId())
                .isEqualTo(expectedGameHost.getId());
    }
}
