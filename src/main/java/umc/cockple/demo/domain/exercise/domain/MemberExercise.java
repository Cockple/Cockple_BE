package umc.cockple.demo.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_member_exercise_exercise_member",
        columnNames = {"exercise_id", "member_id"}))
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

    static MemberExercise create(Member member, Exercise exercise, ExerciseMemberShipStatus status) {
        return MemberExercise.builder()
                .member(member)
                .exercise(exercise)
                .exerciseMemberShipStatus(status)
                .build();
    }
}
