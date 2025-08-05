package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class ExerciseDetailDTO {

    @Builder
    public record Response(
            Boolean isManager,
            ExerciseInfo info,
            ParticipantGroup participants,
            WaitingGroup waiting
    ) {
    }

    @Builder
    public record ExerciseInfo(
            String notice,
            String buildingName,
            String location
    ) {
    }

    @Builder
    public record ParticipantGroup(
            Integer currentParticipantCount,
            Integer totalCount,
            Integer manCount,
            Integer womenCount,
            List<ParticipantInfo> list
    ) {
    }

    @Builder
    public record WaitingGroup(
            Integer currentWaitingCount,
            Integer manCount,
            Integer womenCount,
            List<ParticipantInfo> list
    ) {
    }

    @Builder
    public record ParticipantInfo(
            Long participantId,
            Integer participantNumber,
            String profileImageUrl,
            String name,
            String gender,
            String level,
            String participantType,
            String partyPosition,
            String inviterName,
            LocalDateTime joinedAt
    ) {
    }
}
