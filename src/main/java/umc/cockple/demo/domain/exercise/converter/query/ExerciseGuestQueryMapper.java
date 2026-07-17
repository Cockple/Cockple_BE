package umc.cockple.demo.domain.exercise.converter.query;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.exercise.domain.Guest;
import umc.cockple.demo.domain.exercise.dto.guest.ExerciseMyGuestListDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ExerciseGuestQueryMapper {

    public ExerciseMyGuestListDTO.Response toEmptyGuestListResponse() {
        return ExerciseMyGuestListDTO.Response.builder()
                .totalCount(0)
                .maleCount(0)
                .femaleCount(0)
                .list(Collections.emptyList())
                .build();
    }

    public ExerciseMyGuestListDTO.Response toMyGuestListResponse(
            ExerciseMyGuestListDTO.GuestStatistics statistics,
            List<ExerciseMyGuestListDTO.GuestInfo> guestInfoList) {

        return ExerciseMyGuestListDTO.Response.builder()
                .totalCount(statistics.totalCount())
                .maleCount(statistics.maleCount())
                .femaleCount(statistics.femaleCount())
                .list(guestInfoList)
                .build();
    }

    public ExerciseMyGuestListDTO.GuestInfo toGuestInfo(
            Guest guest,
            Map<Long, ExerciseMyGuestListDTO.GuestGroups> guestStatusMap,
            String inviterName) {

        ExerciseMyGuestListDTO.GuestGroups guestGroup = guestStatusMap.get(guest.getId());

        return ExerciseMyGuestListDTO.GuestInfo.builder()
                .guestId(guest.getId())
                .isWaiting(guestGroup.isWaiting())
                .participantNumber(guestGroup.participantNumber())
                .name(guest.getGuestName())
                .gender(guest.getGender())
                .level(guest.getLevel())
                .inviterName(inviterName)
                .build();
    }
}
