package umc.cockple.demo.domain.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.cockple.demo.domain.game.domain.Court;

import java.util.List;

public interface CourtRepository extends JpaRepository<Court, Long> {

    List<Court> findByGameBoardId(Long gameBoardId);

    List<Court> findByGameBoardIdOrderByCourtNoAsc(Long gameBoardId);
}
