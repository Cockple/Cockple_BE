package umc.cockple.demo.domain.game.presentation.dto;

import java.util.List;

public class GameDuplicateCheckDTO {

    public record Response(
            List<PairInfo> pairs
    ) {
    }

    public record PairInfo(
            Long memberIdA,
            Long memberIdB,
            int count,
            boolean playedInLastGame
    ) {
    }
}
