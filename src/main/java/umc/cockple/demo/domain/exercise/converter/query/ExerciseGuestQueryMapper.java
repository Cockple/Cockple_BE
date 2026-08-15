package umc.cockple.demo.domain.exercise.converter.query;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.dto.guest.ExerciseMyGuestListDTO;
import umc.cockple.demo.domain.exercise.service.query.result.ExerciseMyGuestListResult;

import java.util.List;

@Component
public class ExerciseGuestQueryMapper {

    public ExerciseMyGuestListDTO.Response toGuestListResponse(ExerciseMyGuestListResult result) {
        return ExerciseMyGuestListDTO.Response.builder()
                .totalCount(result.totalCount())
                .maleCount(result.maleCount())
                .femaleCount(result.femaleCount())
                .list(toGuestInfos(result.list()))
                .build();
    }

    private List<ExerciseMyGuestListDTO.GuestInfo> toGuestInfos(
            List<ExerciseMyGuestListResult.GuestInfo> results) {
        return results.stream()
                .map(this::toGuestInfo)
                .toList();
    }

    private ExerciseMyGuestListDTO.GuestInfo toGuestInfo(ExerciseMyGuestListResult.GuestInfo result) {
        return ExerciseMyGuestListDTO.GuestInfo.builder()
                .guestId(result.guestId())
                .isWaiting(result.waiting())
                .participantNumber(result.participantNumber())
                .name(result.name())
                .gender(result.gender())
                .level(result.level())
                .inviterName(result.inviterName())
                .build();
    }
}
