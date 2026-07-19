package umc.cockple.demo.domain.exercise.service.query.model;

import lombok.Builder;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

@Builder
public record ExerciseRecommendationFilterCondition(
        String addr1,
        String addr2,
        List<Level> levels,
        List<ParticipationType> participationTypes,
        List<ActivityTime> activityTimes
) {
}
