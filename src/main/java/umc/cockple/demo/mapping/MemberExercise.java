package umc.cockple.demo.mapping;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.ExerciseOrderType;
import umc.cockple.demo.global.common.BaseEntity;

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
    private LocalDateTime joinedAt;

    @Column(nullable = false)
    private Integer participantNum;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExerciseOrderType orderType;
}
