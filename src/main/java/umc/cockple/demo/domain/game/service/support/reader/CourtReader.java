package umc.cockple.demo.domain.game.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.repository.CourtRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourtReader {

    private final CourtRepository courtRepository;

    public List<Court> readAllByGameBoard(Long gameBoardId) {
        return courtRepository.findByGameBoardId(gameBoardId);
    }
}
