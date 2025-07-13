package umc.cockple.demo.domain.exercise.dto;

import lombok.Builder;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

@Builder
public record GuestInviteCommand(
        String guestName,
        Gender gender,
        Level level,
        Long inviterId
) {
}
