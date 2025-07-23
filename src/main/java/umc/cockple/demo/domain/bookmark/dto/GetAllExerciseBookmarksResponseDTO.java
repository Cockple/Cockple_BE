package umc.cockple.demo.domain.bookmark.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record GetAllExerciseBookmarksResponseDTO(

        Long exerciseId,
        String partyName,
        String buildingName,
        String streetAddr,
        List<Level> femaleLevel,
        List<Level> maleLevel,
        LocalDate date,
        LocalTime startExerciseTime,
        LocalTime endExerciseTime,
        Integer maxMemberCnt,
        Integer nowMemberCnt,
        Boolean includeParty,
        Boolean includeExercise

) {
}
