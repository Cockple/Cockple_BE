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
        return createExerciseAddr(buildingName, streetAddr, 37.5, 127.0);
    }

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

    public static Exercise createRecommendableExercise(Party party, LocalDate date,
                                                       double latitude, double longitude,
                                                       String buildingName) {
        return Exercise.builder()
                .party(party)
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .maxCapacity(10)
                .partyGuestAccept(true)
                .outsideGuestAccept(true)
                .exerciseAddr(createExerciseAddr(buildingName, "테헤란로 1", latitude, longitude))
                .build();
    }

    public static Exercise createExerciseForEdit(Party party, LocalDate date) {
        return Exercise.builder()
                .party(party)
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 30))
                .maxCapacity(18)
                .partyGuestAccept(true)
                .outsideGuestAccept(false)
                .notice("수정 공지사항")
                .exerciseAddr(createExerciseAddr())
                .build();
    }
}
