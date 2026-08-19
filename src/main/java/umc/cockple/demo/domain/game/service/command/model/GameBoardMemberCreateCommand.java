package umc.cockple.demo.domain.game.service.command.model;

import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

public record GameBoardMemberCreateCommand(
        Long gameBoardId,
        String name,
        Gender gender,
        Level level,
        AgeGroup ageGroup
) {
}
