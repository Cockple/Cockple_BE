package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.party.domain.Party;

import java.time.LocalDate;
import java.time.LocalTime;

public class ExerciseFixture {

    public static Exercise createExercise(Party party, LocalDate date) {
        return Exercise.builder()
                .party(party)
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .maxCapacity(10)
                .partyGuestAccept(true)
                .outsideGuestAccept(false)
                .build();
    }
}
