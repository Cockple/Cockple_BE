package umc.cockple.demo.support.fixture;

import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.domain.ExerciseAddr;
import umc.cockple.demo.domain.party.domain.Party;

import java.time.LocalDate;
import java.time.LocalTime;

public class ExerciseFixture {

    public static ExerciseAddr createExerciseAddr(String buildingName, String streetAddr,
                                                  double latitude, double longitude) {
        return ExerciseAddr.builder()
                .addr1("서울특별시")
                .addr2("강남구")
                .streetAddr(streetAddr)
                .buildingName(buildingName)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    public static ExerciseAddr createExerciseAddr() {
        return createExerciseAddr("테스트 체육관", "서울특별시 강남구 테헤란로 1");
    }

    public static ExerciseAddr createExerciseAddr(String buildingName, String streetAddr) {
        return createExerciseAddr(buildingName, streetAddr, 37.5, 127.0);
    }

    private static Exercise createExercise(Party party, LocalDate date, LocalTime endTime,
                                           int maxCapacity, boolean partyGuestAccept,
                                           boolean outsideGuestAccept, ExerciseAddr exerciseAddr,
                                           String notice) {
        return Exercise.builder()
                .party(party)
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(endTime)
                .maxCapacity(maxCapacity)
                .partyGuestAccept(partyGuestAccept)
                .outsideGuestAccept(outsideGuestAccept)
                .exerciseAddr(exerciseAddr)
                .notice(notice)
                .gameHostId(party != null ? party.getOwnerId() : null)
                .build();
    }

    public static Exercise createExercise(Party party, LocalDate date) {
        return createExercise(party, date, null, true, false);
    }

    public static Exercise createExercise(Party party, LocalDate date, LocalTime endTime,
                                          boolean partyGuestAccept, boolean outsideGuestAccept) {
        return createExercise(party, date, endTime, 10, partyGuestAccept, outsideGuestAccept,
                null, null);
    }

    public static Exercise createExerciseWithAddr(Party party, LocalDate date) {
        return createExerciseWithAddr(party, date, 10);
    }

    public static Exercise createExerciseWithAddr(Party party, LocalDate date, int maxCapacity) {
        return createExercise(party, date, null, maxCapacity, true, false,
                createExerciseAddr(), null);
    }

    public static Exercise createRecommendableExercise(Party party, LocalDate date,
                                                       double latitude, double longitude,
                                                       String buildingName) {
        return createExercise(party, date, LocalTime.of(12, 0), 10, true, true,
                createExerciseAddr(buildingName, "테헤란로 1", latitude, longitude), null);
    }

    public static Exercise createExerciseForEdit(Party party, LocalDate date) {
        return createExercise(party, date, LocalTime.of(12, 30), 18, true, false,
                createExerciseAddr(), "수정 공지사항");
    }
}
