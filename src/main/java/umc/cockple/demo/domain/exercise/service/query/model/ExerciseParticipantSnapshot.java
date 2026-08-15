package umc.cockple.demo.domain.exercise.service.query.model;

import umc.cockple.demo.domain.exercise.enums.ExerciseMemberShipStatus;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.enums.Role;

import java.time.LocalDateTime;

public record ExerciseParticipantSnapshot(
        Long participantId,
        String profileImageUrl,
        String name,
        Gender gender,
        Level level,
        ExerciseMemberShipStatus membershipStatus,
        Role partyPosition,
        String inviterName,
        LocalDateTime joinedAt,
        boolean withdrawn
) {

    public boolean isGuest() {
        return membershipStatus == ExerciseMemberShipStatus.GUEST;
    }
}
