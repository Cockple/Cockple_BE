package umc.cockple.demo.domain.exercise.service.query.result;

import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

public record ExerciseMyGuestListResult(
        int totalCount,
        int maleCount,
        int femaleCount,
        List<GuestInfo> list
) {

    public static ExerciseMyGuestListResult empty() {
        return new ExerciseMyGuestListResult(0, 0, 0, List.of());
    }

    public record GuestInfo(
            Long guestId,
            boolean waiting,
            int participantNumber,
            String name,
            Gender gender,
            Level level,
            String inviterName
    ) {
    }

    public record GuestGroup(
            int participantNumber,
            boolean waiting
    ) {

        public static GuestGroup participant(int number) {
            return new GuestGroup(number, false);
        }

        public static GuestGroup waiting(int number) {
            return new GuestGroup(number, true);
        }
    }
}
