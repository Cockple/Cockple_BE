package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.party.domain.Party;

import java.time.LocalDate;
import java.time.LocalTime;

public class ExerciseFixture {

    public static ExerciseAddr createExerciseAddr() {
        return createExerciseAddr("테스트 체육관", "서울특별시 강남구 테헤란로 1");
    }

    public static ExerciseAddr createExerciseAddr(String buildingName, String streetAddr) {
        return ExerciseAddr.builder()
                .addr1("서울특별시")
                .addr2("강남구")
                .streetAddr(streetAddr)
                .buildingName(buildingName)
                .latitude(37.5)
                .longitude(127.0)
                .build();
    }

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

    public static Exercise createExercise(Party party, LocalDate date, LocalTime endTime,
                                          boolean partyGuestAccept, boolean outsideGuestAccept) {
        return Exercise.builder()
                .party(party)
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(endTime)
                .maxCapacity(10)
                .partyGuestAccept(partyGuestAccept)
                .outsideGuestAccept(outsideGuestAccept)
                .build();
    }

    public static Exercise createExerciseWithAddr(Party party, LocalDate date) {
        return createExerciseWithAddr(party, date, 10);
    }

    public static Exercise createExerciseWithAddr(Party party, LocalDate date, int maxCapacity) {
        return Exercise.builder()
                .party(party)
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .maxCapacity(maxCapacity)
                .partyGuestAccept(true)
                .outsideGuestAccept(false)
                .exerciseAddr(createExerciseAddr())
                .build();
    }
}
