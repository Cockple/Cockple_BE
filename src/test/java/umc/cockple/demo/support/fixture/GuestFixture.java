package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

public class GuestFixture {

    public static Guest createGuest(Exercise exercise, Long inviterId) {
        Guest guest = Guest.builder()
                .guestName("게스트")
                .gender(Gender.MALE)
                .level(Level.B)
                .inviterId(inviterId)
                .build();
        guest.setExercise(exercise);
        return guest;
    }
}