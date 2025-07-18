package umc.cockple.demo.domain.exercise.domain;

import jakarta.persistence.*;
import lombok.*;
import umc.cockple.demo.domain.exercise.dto.ExerciseGuestInviteDTO;
import umc.cockple.demo.global.common.BaseEntity;
import umc.cockple.demo.global.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

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

    public static Guest create(ExerciseGuestInviteDTO.Command command) {
        return Guest.builder()
                .guestName(command.guestName())
                .gender(command.gender())
                .level(command.level())
                .inviterId(command.inviterId())
                .build();
    }

    public ExerciseMemberShipStatus getExerciseMemberShipStatus() {
        return ExerciseMemberShipStatus.GUEST;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
        if (exercise != null && !exercise.getGuests().contains(this)) {
            exercise.getGuests().add(this);
        }
    }
}
