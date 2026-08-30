package umc.cockple.demo.domain.game.presentation.mapper;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.presentation.dto.GameDuplicateCheckDTO;
import umc.cockple.demo.domain.game.service.query.result.GameDuplicateCheckResult;

@Component
public class GameDuplicateCheckMapper {

    public GameDuplicateCheckDTO.Response toResponse(GameDuplicateCheckResult result) {
        return new GameDuplicateCheckDTO.Response(
                result.pairs().stream()
                        .map(pair -> new GameDuplicateCheckDTO.PairInfo(
                                pair.memberIdA(), pair.memberIdB(), pair.count(), pair.playedInLastGame()))
                        .toList());
    }
}
