package umc.cockple.demo.domain.bookmark.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.ActivityTime;
import umc.cockple.demo.global.enums.Level;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record GetAllPartyBookmarkResponseDTO(
        Long partyId,
        String partyName,
        String addr1,
        String addr2,
        List<Level> maleLevel,
        List<Level> femaleLevel,
        LocalDate latestExerciseDate,
        ActivityTime latestExerciseTime,
        Integer exerciseCnt,
        String profileImgUrl

        ) {
}
