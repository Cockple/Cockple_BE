package umc.cockple.demo.domain.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.enums.GameStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByCourtIdAndStatus(Long courtId, GameStatus status);

    boolean existsByCourtIdInAndStatus(Collection<Long> courtIds, GameStatus status);

    List<Game> findByGameBoardIdAndStatusOrderByWaitingOrderAsc(Long gameBoardId, GameStatus status);

    long countByGameBoardIdAndStatus(Long gameBoardId, GameStatus status);

    @Query("select distinct g from Game g " +
            "left join fetch g.court " +
            "left join fetch g.players p " +
            "left join fetch p.gameBoardMember " +
            "where g.gameBoard.id = :gameBoardId and g.status in :statuses")
    List<Game> findByGameBoardIdAndStatusInWithPlayers(
            @Param("gameBoardId") Long gameBoardId,
            @Param("statuses") Collection<GameStatus> statuses);
}
