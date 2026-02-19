package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.member.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDateTime;

public class MemberFixture {

    public static Member createMember(String nickname, Gender gender, Level level, Long socialId) {
        return Member.builder()
                .nickname(nickname)
                .gender(gender)
                .level(level)
                .isActive(MemberStatus.ACTIVE)
                .socialId(socialId)
                .build();
    }

    public static MemberParty createMemberParty(Party party, Member member, Role role) {
        return MemberParty.builder()
                .party(party)
                .member(member)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .status(MemberPartyStatus.ACTIVE)
                .build();
    }

    public static MemberExercise createMemberExercise(Member member, Exercise exercise) {
        return MemberExercise.builder()
                .member(member)
                .exercise(exercise)
                .exerciseMemberShipStatus(ExerciseMemberShipStatus.PARTY_MEMBER)
                .build();
    }
}
