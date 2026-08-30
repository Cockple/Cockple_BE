package umc.cockple.demo.domain.game.presentation.dto;

import java.util.List;

public class GameRandomMatchDTO {

    public record Response(
            List<Long> gameBoardMemberIds
    ) {
    }
}
