package umc.cockple.demo.domain.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.game.domain.GameBoard;

public interface GameBoardRepository extends JpaRepository<GameBoard, Long> {
}
