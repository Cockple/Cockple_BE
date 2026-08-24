package umc.cockple.demo.domain.game.presentation.mapper;

import org.springframework.stereotype.Component;
import umc.cockple.demo.domain.game.presentation.dto.GameRandomMatchDTO;
import umc.cockple.demo.domain.game.service.query.result.GameRandomMatchResult;

@Component
public class GameRandomMatchMapper {

    public GameRandomMatchDTO.Response toResponse(GameRandomMatchResult result) {
        return new GameRandomMatchDTO.Response(result.gameBoardMemberIds());
    }
}
