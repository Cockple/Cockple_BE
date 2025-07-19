package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

public class ExerciseMyGuestListDTO {

    @Builder
    public record Response(
            Integer totalCount,
            Integer maleCount,
            Integer femaleCount,
            List<GuestInfo> list
    ) {
    }

    @Builder
    public record GuestInfo(
            Long guestId,
            Integer participantNumber,
            String name,
            Gender gender,
            Level level,
            String inviterName
    ) {
    }
}
