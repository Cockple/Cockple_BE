package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.exercise.repository.ExerciseParticipationRepository;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.service.ExerciseParticipationCleanupService;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("운동 참여 생명주기")
class ExerciseParticipationLifecycleIntegrationTest extends IntegrationTestBase {

    @Autowired ExerciseParticipationCleanupService cleanupService;
    @Autowired ExerciseParticipationRepository exerciseParticipationRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;

    private Member member;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        Member owner = memberRepository.save(
                MemberFixture.createMember("모임장", Gender.MALE, Level.A, 95001L));
        member = memberRepository.save(
                MemberFixture.createMember("참여 회원", Gender.FEMALE, Level.B, 95002L));

        PartyAddr address = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "참여생명주기구"));
        Party party = partyRepository.save(
                PartyFixture.createParty("참여 생명주기 모임", owner.getId(), address));

        exercise = ExerciseFixture.createExercise(party, LocalDate.now().plusDays(7));
        exercise.addParticipation(member, ExerciseMemberShipStatus.PARTY_MEMBER);
        exerciseRepository.saveAndFlush(exercise);
    }

    @AfterEach
    void tearDown() {
        exerciseParticipationRepository.deleteAll();
        exerciseRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("hard delete 전처리 후 회원을 삭제하면 FK 오류 없이 참여도 삭제된다")
    void memberHardDelete_removesParticipationsBeforeMember() {
        assertThat(exerciseParticipationRepository.countByMember_Id(member.getId())).isEqualTo(1);

        cleanupService.prepareMemberHardDelete(member.getId());
        memberRepository.delete(member);
        memberRepository.flush();

        assertThat(memberRepository.findById(member.getId())).isEmpty();
        assertThat(exerciseParticipationRepository.countByMember_Id(member.getId())).isZero();
    }

    @Test
    @DisplayName("운동을 삭제하면 운동이 소유한 참여 데이터도 함께 삭제된다")
    void exerciseDelete_cascadesParticipations() {
        assertThat(exerciseParticipationRepository.countByMember_Id(member.getId())).isEqualTo(1);

        exerciseRepository.delete(exercise);
        exerciseRepository.flush();

        assertThat(exerciseParticipationRepository.countByMember_Id(member.getId())).isZero();
        assertThat(memberRepository.findById(member.getId())).isPresent();
    }
}
