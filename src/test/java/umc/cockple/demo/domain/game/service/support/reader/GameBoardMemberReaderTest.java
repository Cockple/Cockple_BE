package umc.cockple.demo.domain.game.service.support.reader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameBoardMemberReader")
class GameBoardMemberReaderTest {

    private static final Long GAME_BOARD_ID = 1L;
    private static final Long GAME_BOARD_MEMBER_ID = 2L;

    @InjectMocks private GameBoardMemberReader gameBoardMemberReader;
    @Mock private GameBoardMemberRepository gameBoardMemberRepository;
    @Mock private GameBoardMember gameBoardMember;

    @Test
    @DisplayName("게임판과 명단 ID가 모두 일치하면 명단을 반환한다")
    void read_returnsMemberBelongingToGameBoard() {
        given(gameBoardMemberRepository.findByIdAndGameBoardId(GAME_BOARD_MEMBER_ID, GAME_BOARD_ID))
                .willReturn(Optional.of(gameBoardMember));

        assertThat(gameBoardMemberReader.read(GAME_BOARD_ID, GAME_BOARD_MEMBER_ID))
                .isSameAs(gameBoardMember);
    }

    @Test
    @DisplayName("해당 게임판에 명단 ID가 없으면 GAME_BOARD_MEMBER_NOT_FOUND 예외를 던진다")
    void read_throwsWhenMemberDoesNotBelongToGameBoard() {
        given(gameBoardMemberRepository.findByIdAndGameBoardId(GAME_BOARD_MEMBER_ID, GAME_BOARD_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> gameBoardMemberReader.read(GAME_BOARD_ID, GAME_BOARD_MEMBER_ID))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GameErrorCode.GAME_BOARD_MEMBER_NOT_FOUND));
    }
}
