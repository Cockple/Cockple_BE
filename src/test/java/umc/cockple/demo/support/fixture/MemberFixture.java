package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.domain.exercise.domain.MemberExercise;
import umc.cockple.demo.domain.member.domain.MemberParty;
import umc.cockple.demo.domain.member.enums.MemberPartyStatus;
import umc.cockple.demo.domain.member.enums.MemberStatus;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.party.domain.Party;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MemberFixture {

    // 온보딩이 완료된(=isProfileCompleted) 회원을 의미하도록 기본 birth를 채워둔다.
    private static final LocalDate DEFAULT_BIRTH = LocalDate.of(2000, 1, 1);

    public static Member createMember(String nickname, Gender gender, Level level, Long socialId) {
        return Member.builder()
                .memberName(nickname)
                .nickname(nickname)
                .gender(gender)
                .level(level)
                .birth(DEFAULT_BIRTH)
                .isActive(MemberStatus.ACTIVE)
                .socialId(socialId)
                .build();
    }

    public static Member createMember(String nickname, Gender gender, Level level, Long socialId, LocalDate birth) {
        return Member.builder()
                .memberName(nickname)
                .nickname(nickname)
                .gender(gender)
                .level(level)
                .birth(birth)
                .isActive(MemberStatus.ACTIVE)
                .socialId(socialId)
                .build();
    }

    public static Member createMemberWithName(String memberName, String nickname, Gender gender, Level level, Long socialId) {
        return Member.builder()
                .memberName(memberName)
                .nickname(nickname)
                .gender(gender)
                .level(level)
                .birth(DEFAULT_BIRTH)
                .isActive(MemberStatus.ACTIVE)
                .socialId(socialId)
                .build();
    }

    public static Member createWithdrawnMember(String memberName, String nickname, Long socialId) {
        return Member.builder()
                .memberName(memberName)
                .nickname(nickname)
                .gender(Gender.MALE)
                .level(Level.C)
                .birth(DEFAULT_BIRTH)
                .isActive(MemberStatus.INACTIVE)
                .socialId(socialId)
                .build();
    }

    /**
     * 소셜로그인 직후, 상세정보(이름/성별/생년월일/급수)를 아직 입력하지 않은 회원.
     * isProfileCompleted() == false 인 온보딩 미완료 상태 검증용.
     */
    public static Member createOnboardingPendingMember(String nickname, Long socialId) {
        return Member.builder()
                .nickname(nickname)
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

    public static MemberExercise createExternalMemberExercise(Member member, Exercise exercise) {
        return MemberExercise.builder()
                .member(member)
                .exercise(exercise)
                .exerciseMemberShipStatus(ExerciseMemberShipStatus.EXTERNAL_PARTICIPANT)
                .build();
    }
}
