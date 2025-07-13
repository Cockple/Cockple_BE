package umc.cockple.demo.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.dto.GuestInviteCommand;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.common.BaseEntity;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Guest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "exercise_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Exercise exercise;

    @Column(nullable = false)
    private String guestName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Level level;

    @Column(nullable = false)
    private Long inviterId;

    @Column(nullable = false)
    private Integer participantNum;

    public static Guest createForExercise(Exercise exercise, GuestInviteCommand command, Integer participantNum) {
        return Guest.builder()
                .exercise(exercise)
                .guestName(command.guestName())
                .gender(command.gender())
                .level(command.level())
                .inviterId(command.inviterId())
                .participantNum(participantNum)
                .build();
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
        if (!exercise.getGuests().contains(this)) {
            exercise.getGuests().add(this);
        }
    }
}
