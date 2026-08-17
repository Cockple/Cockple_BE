package umc.cockple.demo.domain.exercise.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.exercise.repository.GuestRepository;
import umc.cockple.demo.domain.exercise.repository.MemberExerciseRepository;
import umc.cockple.demo.domain.exercise.service.command.ExerciseGuestCommandService;
import umc.cockple.demo.domain.exercise.service.command.ExerciseParticipationCommandService;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseCancelByManagerCommand;
import umc.cockple.demo.domain.exercise.service.command.model.ExerciseGuestInviteCommand;
import umc.cockple.demo.domain.exercise.service.command.result.ExerciseGuestInviteResult;
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

@DisplayName("운동 참가자-게임판 명단 동기화")
class ExerciseGameBoardRosterIntegrationTest extends IntegrationTestBase {

    @Autowired ExerciseParticipationCommandService participationCommandService;
    @Autowired ExerciseGuestCommandService guestCommandService;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberPartyRepository memberPartyRepository;
    @Autowired PartyAddrRepository partyAddrRepository;
    @Autowired PartyRepository partyRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired MemberExerciseRepository memberExerciseRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private Member manager;
    private Member participant;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        manager = memberRepository.save(MemberFixture.createMember(
                "모임장", Gender.MALE, Level.A, 92001L, LocalDate.of(1995, 1, 1)));
        participant = memberRepository.save(MemberFixture.createMember(
                "참여자", Gender.FEMALE, Level.B, 92002L, LocalDate.of(2000, 1, 1)));

        PartyAddr address = partyAddrRepository.save(
                PartyFixture.createPartyAddr("서울특별시", "명단동기화구"));
        Party party = partyRepository.save(
                PartyFixture.createParty("명단 동기화 모임", manager.getId(), address));

        memberPartyRepository.save(MemberFixture.createMemberParty(party, manager, Role.PARTY_MANAGER));
        memberPartyRepository.save(MemberFixture.createMemberParty(party, participant, Role.PARTY_MEMBER));

        party.addLevel(Gender.FEMALE, Level.B);
        partyRepository.save(party);

        exercise = exerciseRepository.save(
                ExerciseFixture.createExercise(party, LocalDate.of(2035, 6, 30)));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM game_board_member");
        guestRepository.deleteAll();
        memberExerciseRepository.deleteAll();
        exerciseRepository.deleteAll();
        memberPartyRepository.deleteAll();
        partyRepository.deleteAll();
        partyAddrRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원 참여와 본인 취소가 회원 명단을 생성하고 삭제한다")
    void memberJoinAndSelfCancel_syncRoster() {
        participationCommandService.joinExercise(exercise.getId(), participant.getId());

        RosterRow roster = findOnlyRoster();
        assertMemberRoster(roster);

        participationCommandService.cancelParticipation(exercise.getId(), participant.getId());

        assertThat(rosterCount()).isZero();
    }

    @Test
    @DisplayName("관리자에 의한 회원 참여 취소도 회원 명단을 삭제한다")
    void managerMemberCancel_syncRoster() {
        participationCommandService.joinExercise(exercise.getId(), participant.getId());

        participationCommandService.cancelParticipationByManager(
                exercise.getId(), participant.getId(), manager.getId(),
                new ExerciseCancelByManagerCommand(false));

        assertThat(rosterCount()).isZero();
    }

    @Test
    @DisplayName("게스트 초대와 초대 취소가 게스트 명단을 생성하고 삭제한다")
    void guestInviteAndCancel_syncRoster() {
        ExerciseGuestInviteResult invited = inviteGuest();

        RosterRow roster = findOnlyRoster();
        assertGuestRoster(roster, invited.guestId());

        guestCommandService.cancelGuestInvitation(
                exercise.getId(), invited.guestId(), manager.getId());

        assertThat(rosterCount()).isZero();
    }

    @Test
    @DisplayName("관리자에 의한 게스트 참여 취소도 게스트 명단을 삭제한다")
    void managerGuestCancel_syncRoster() {
        ExerciseGuestInviteResult invited = inviteGuest();

        participationCommandService.cancelParticipationByManager(
                exercise.getId(), invited.guestId(), manager.getId(),
                new ExerciseCancelByManagerCommand(true));

        assertThat(rosterCount()).isZero();
    }

    private ExerciseGuestInviteResult inviteGuest() {
        return guestCommandService.inviteGuest(
                exercise.getId(),
                ExerciseGuestInviteCommand.builder()
                        .guestName("초대 게스트")
                        .gender(Gender.MALE)
                        .level(Level.C)
                        .inviterId(manager.getId())
                        .build());
    }

    private void assertMemberRoster(RosterRow roster) {
        assertThat(roster.memberId()).isEqualTo(participant.getId());
        assertThat(roster.guestId()).isNull();
        assertThat(roster.name()).isEqualTo(participant.getMemberName());
        assertThat(roster.gender()).isEqualTo(Gender.FEMALE.name());
        assertThat(roster.level()).isEqualTo(Level.B.name());
        assertThat(roster.ageGroup()).isEqualTo("THIRTIES");
        assertDefaultState(roster);
    }

    private void assertGuestRoster(RosterRow roster, Long guestId) {
        assertThat(roster.memberId()).isNull();
        assertThat(roster.guestId()).isEqualTo(guestId);
        assertThat(roster.name()).isEqualTo("초대 게스트");
        assertThat(roster.gender()).isEqualTo(Gender.MALE.name());
        assertThat(roster.level()).isEqualTo(Level.C.name());
        assertThat(roster.ageGroup()).isNull();
        assertDefaultState(roster);
    }

    private void assertDefaultState(RosterRow roster) {
        assertThat(roster.shuttlecockSubmitted()).isFalse();
        assertThat(roster.participating()).isTrue();
        assertThat(roster.gameCount()).isZero();
    }

    private RosterRow findOnlyRoster() {
        return jdbcTemplate.queryForObject("""
                SELECT member_id, guest_id, name, gender, level, age_group,
                       shuttlecock_submitted, participating, game_count
                FROM game_board_member
                WHERE game_board_id = ?
                """, (resultSet, rowNum) -> new RosterRow(
                resultSet.getObject("member_id", Long.class),
                resultSet.getObject("guest_id", Long.class),
                resultSet.getString("name"),
                resultSet.getString("gender"),
                resultSet.getString("level"),
                resultSet.getString("age_group"),
                resultSet.getBoolean("shuttlecock_submitted"),
                resultSet.getBoolean("participating"),
                resultSet.getInt("game_count")),
                exercise.getGameBoard().getId());
    }

    private int rosterCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM game_board_member WHERE game_board_id = ?",
                Integer.class,
                exercise.getGameBoard().getId());
        return count == null ? 0 : count;
    }

    private record RosterRow(
            Long memberId,
            Long guestId,
            String name,
            String gender,
            String level,
            String ageGroup,
            boolean shuttlecockSubmitted,
            boolean participating,
            int gameCount
    ) {
    }
}
