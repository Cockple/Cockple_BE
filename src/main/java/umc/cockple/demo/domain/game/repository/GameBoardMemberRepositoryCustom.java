package umc.cockple.demo.domain.game.repository;

import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;

import java.util.List;

public interface GameBoardMemberRepositoryCustom {

    List<GameBoardMember> findAllByFilters(
            Long gameBoardId,
            List<Level> levels,
            Gender gender,
            Boolean shuttlecockSubmitted);
}
