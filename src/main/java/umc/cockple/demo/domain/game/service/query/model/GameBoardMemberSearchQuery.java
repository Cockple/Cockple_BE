package umc.cockple.demo.domain.game.service.query.model;

import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

public record GameBoardMemberSearchQuery(
        List<Level> levels,
        Gender gender,
        Boolean shuttlecockSubmitted
) {
    public GameBoardMemberSearchQuery {
        levels = levels == null ? List.of() : List.copyOf(levels);
    }
}
