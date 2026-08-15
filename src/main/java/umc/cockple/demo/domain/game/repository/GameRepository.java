package umc.cockple.demo.domain.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.enums.GameStatus;

import java.util.Collection;
import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * 게임판의 특정 상태 게임들을 코트/플레이어/멤버까지 함께 조회한다. (보드 조회 N+1 방지)
     */
    @Query("select distinct g from Game g " +
            "left join fetch g.court " +
            "left join fetch g.players p " +
            "left join fetch p.gameBoardMember " +
            "where g.gameBoard.id = :gameBoardId and g.status in :statuses")
    List<Game> findByGameBoardIdAndStatusInWithPlayers(
            @Param("gameBoardId") Long gameBoardId,
            @Param("statuses") Collection<GameStatus> statuses);
}
