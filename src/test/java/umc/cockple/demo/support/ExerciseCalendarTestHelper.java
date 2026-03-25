package umc.cockple.demo.support;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class ExerciseCalendarTestHelper {

    private ExerciseCalendarTestHelper() {
    }

    public static LocalDate expectedDefaultStartDate() {
        LocalDate today = LocalDate.now();
        LocalDate thisWeekMonday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        return thisWeekMonday.minusWeeks(1);
    }

    public static LocalDate expectedDefaultEndDate() {
        LocalDate today = LocalDate.now();
        LocalDate thisWeekMonday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        return thisWeekMonday.plusWeeks(3).plusDays(6);
    }

    public static int weekIndexFor(LocalDate expectedStart, LocalDate targetDate) {
        return (int) (ChronoUnit.DAYS.between(expectedStart, targetDate) / 7);
    }

    public static int dayIndexFor(LocalDate targetDate) {
        return targetDate.getDayOfWeek().getValue() - 1;
    }
}
