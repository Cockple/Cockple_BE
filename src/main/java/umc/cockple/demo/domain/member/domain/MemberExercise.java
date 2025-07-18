package umc.cockple.demo.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.global.common.BaseEntity;
import umc.cockple.demo.global.enums.ExerciseMemberShipStatus;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class MemberExercise extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExerciseMemberShipStatus exerciseMemberShipStatus;

    public static MemberExercise create(boolean isPartyMember) {
        ExerciseMemberShipStatus status = isPartyMember ?
                ExerciseMemberShipStatus.PARTY_MEMBER : ExerciseMemberShipStatus.EXTERNAL_MEMBER;

        return MemberExercise.builder()
                .exerciseMemberShipStatus(status)
                .build();
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
        if (exercise != null && !exercise.getMemberExercises().contains(this)) {
            exercise.getMemberExercises().add(this);
        }
    }

    public void setMember(Member member) {
        this.member = member;
        if (member != null && !member.getMemberExercises().contains(this)) {
            member.getMemberExercises().add(this);
        }
    }
}
