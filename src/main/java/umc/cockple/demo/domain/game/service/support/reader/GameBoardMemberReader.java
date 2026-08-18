package umc.cockple.demo.domain.game.service.support.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameBoardMemberReader {

    private final GameBoardMemberRepository gameBoardMemberRepository;

    public GameBoardMember read(Long gameBoardId, Long gameBoardMemberId) {
        return gameBoardMemberRepository.findByIdAndGameBoardId(gameBoardMemberId, gameBoardId)
                .orElseThrow(() -> new GameException(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND));
    }
}
