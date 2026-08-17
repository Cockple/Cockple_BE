package umc.cockple.demo.domain.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.game.domain.GameBoardMember;

import java.util.Collection;
import java.util.List;

public interface GameBoardMemberRepository extends JpaRepository<GameBoardMember, Long> {

    List<GameBoardMember> findByGameBoardIdAndIdIn(Long gameBoardId, Collection<Long> ids);
}
