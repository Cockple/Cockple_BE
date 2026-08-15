package umc.cockple.demo.domain.exercise.presentation.dto.guest;

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
            Boolean isWaiting,
            Integer participantNumber,
            String name,
            Gender gender,
            Level level,
            String inviterName
    ) {
    }

    @Builder
    public record GuestStatistics(
            int totalCount,     // 전체 게스트 수
            int maleCount,      // 남자 게스트 수
            int femaleCount     // 여자 게스트 수
    ) {

        public static GuestStatistics empty() {
            return new GuestStatistics(0, 0, 0);
        }

        public static GuestStatistics of(int totalCount, int maleCount, int femaleCount) {
            return new GuestStatistics(totalCount, maleCount, femaleCount);
        }
    }


}
