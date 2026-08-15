package umc.cockple.demo.domain.exercise.service.query.result;

import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;

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
            Gender gender,
            Level level,
            ExerciseMemberShipStatus participantType,
            Role partyPosition,
            String inviterName,
            LocalDateTime joinedAt,
            boolean withdrawn
    ) {
    }
}
