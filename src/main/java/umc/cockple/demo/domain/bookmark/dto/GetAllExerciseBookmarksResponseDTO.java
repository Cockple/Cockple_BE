package umc.cockple.demo.domain.bookmark.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record GetAllExerciseBookmarksResponseDTO(

        Long exerciseId,
        String exerciseName,
        String buildingName,
        String streetAddr,
        Level maxFemaleLevel,
        Level minFemaleLevel,
        Level maxMaleLevel,
        Level minMaleLevel,
        LocalDate date,
        LocalTime startExerciseTime,
        LocalTime endExerciseTime,
        Integer maxMemberCnt,
        Integer nowMemberCnt,
        Boolean includeParty,
        Boolean includeExercise

) {
}
