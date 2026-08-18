package umc.cockple.demo.domain.game.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.cockple.demo.domain.game.domain.GameBoard;

import java.util.Optional;

public interface GameBoardRepository extends JpaRepository<GameBoard, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select gameBoard from GameBoard gameBoard where gameBoard.id = :gameBoardId")
    Optional<GameBoard> findByIdForUpdate(@Param("gameBoardId") Long gameBoardId);
}
