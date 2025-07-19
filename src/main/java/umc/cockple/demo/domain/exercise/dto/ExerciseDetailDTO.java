package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;

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
            ParticipantInfo list
    ) {
    }

    @Builder
    public record WaitingGroup(
            Integer currentWaitingCount,
            Integer manCount,
            Integer womenCount,
            ParticipantInfo list
    ) {
    }

    @Builder
    public record ParticipantInfo(
            Long participantId,
            Integer participantNumber,
            String imgUrl,
            String name,
            String gender,
            String level,
            String participantType,
            String partyPosition,
            String inviterName
    ) {
    }
}
