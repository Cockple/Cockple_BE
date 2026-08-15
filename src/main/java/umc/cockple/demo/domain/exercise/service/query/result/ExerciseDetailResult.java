package umc.cockple.demo.domain.exercise.service.query.result;

import java.time.LocalDateTime;
import java.util.List;

public record ExerciseDetailResult(
        boolean isManager,
        ExerciseInfo info,
        ParticipantGroup participants,
        WaitingGroup waiting
) {

    public record ExerciseInfo(
            String notice,
            String buildingName,
            String location
    ) {
    }

    public record ParticipantGroup(
            int currentParticipantCount,
            int totalCount,
            int manCount,
            int womenCount,
            List<ParticipantInfo> list
    ) {
    }

    public record WaitingGroup(
            int currentWaitingCount,
            int manCount,
            int womenCount,
            List<ParticipantInfo> list
    ) {
    }

    public record ParticipantInfo(
            Long participantId,
            int participantNumber,
            String profileImageUrl,
            String name,
            String gender,
            String level,
            String participantType,
            String partyPosition,
            String inviterName,
            LocalDateTime joinedAt,
            boolean withdrawn
    ) {
    }
}
