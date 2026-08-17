package umc.cockple.demo.domain.member.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.member.repository.MemberPartyRepository;
import umc.cockple.demo.domain.member.repository.MemberRepository;
import umc.cockple.demo.domain.member.service.MemberCommandService;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.domain.party.domain.PartyAddr;
import umc.cockple.demo.domain.party.repository.PartyAddrRepository;
import umc.cockple.demo.domain.party.repository.PartyRepository;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;
import umc.cockple.demo.global.oauth2.service.KakaoOauthService;
import umc.cockple.demo.support.IntegrationTestBase;
import umc.cockple.demo.support.fixture.ExerciseFixture;
import umc.cockple.demo.support.fixture.MemberFixture;
import umc.cockple.demo.support.fixture.PartyFixture;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("회원 탈퇴 시 게임판 명단 정리")
class MemberGameBoardRosterWithdrawalIntegrationTest extends IntegrationTestBase {

    @Autowired MemberCommandService memberCommandService;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockitoBean KakaoOauthService kakaoOauthService;

    private Member member;

    @BeforeEach
    void setUp() {
        Member owner = memberRepository.save(
                MemberFixture.createMember("모임장", Gender.MALE, Level.A, 94001L));
        member = memberRepository.save(
                MemberFixture.createMember("탈퇴 회원", Gender.FEMALE, Level.B, 94002L));

        PartyAddr address = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "탈퇴명단구"));
        Party party = partyRepository.save(
                PartyFixture.createParty("탈퇴 명단 모임", owner.getId(), address));

        memberPartyRepository.save(MemberFixture.createMemberParty(party, owner, Role.PARTY_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, member, Role.PARTY_MEMBER));

        Exercise pastExercise = ExerciseFixture.createExercise(party, LocalDate.now().minusDays(7));
        pastExercise.addParticipation(member, ExerciseMemberShipStatus.PARTY_MEMBER);
        exerciseRepository.save(pastExercise);

        Exercise futureExercise = ExerciseFixture.createExercise(party, LocalDate.now().plusDays(7));
        futureExercise.addParticipation(member, ExerciseMemberShipStatus.PARTY_MEMBER);
        exerciseRepository.saveAndFlush(futureExercise);
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
    @DisplayName("미래 참가와 명단만 삭제하고 과거 참가 스냅샷은 보존한다")
    void withdraw_removesOnlyFutureParticipationAndRoster() {
        assertThat(countMemberExercises(true)).isEqualTo(1);
        assertThat(countMemberExercises(false)).isEqualTo(1);
        assertThat(countRosters(true)).isEqualTo(1);
        assertThat(countRosters(false)).isEqualTo(1);

        memberCommandService.withdrawMember(member.getId());

        assertThat(countMemberExercises(true)).isZero();
        assertThat(countRosters(true)).isZero();
        assertThat(countMemberExercises(false)).isEqualTo(1);
        assertThat(countRosters(false)).isEqualTo(1);
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getIsActive())
                .isEqualTo(MemberStatus.INACTIVE);
    }

    private int countMemberExercises(boolean future) {
        return countByDate("""
                SELECT COUNT(*)
                FROM member_exercise
                INNER JOIN exercise ON exercise.id = member_exercise.exercise_id
                WHERE member_exercise.member_id = ?
                """, future);
    }

    private int countRosters(boolean future) {
        return countByDate("""
                SELECT COUNT(*)
                FROM game_board_member
                INNER JOIN exercise ON exercise.game_board_id = game_board_member.game_board_id
                WHERE game_board_member.member_id = ?
                """, future);
    }

    private int countByDate(String baseQuery, boolean future) {
        String dateCondition = future
                ? " AND exercise.date > CURRENT_DATE"
                : " AND exercise.date < CURRENT_DATE";
        Integer count = jdbcTemplate.queryForObject(
                baseQuery + dateCondition,
                Integer.class,
                member.getId());
        return count == null ? 0 : count;
    }
}
