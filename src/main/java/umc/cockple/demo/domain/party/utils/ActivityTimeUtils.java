package umc.cockple.demo.domain.party.utils;

import umc.cockple.demo.domain.party.enums.ActivityTime;

import java.util.List;

import static umc.cockple.demo.domain.party.enums.ActivityTime.*;

public class ActivityTimeUtils {
    public static boolean shouldAddAlways(List<ActivityTime> times) {
        boolean hasTimeSpecific = times.stream()
                .anyMatch(time -> time == MORNING || time == AFTERNOON);
        return hasTimeSpecific && !times.contains(ALWAYS);
    }
}
