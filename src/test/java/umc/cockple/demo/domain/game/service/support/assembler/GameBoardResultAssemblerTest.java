package umc.cockple.demo.domain.game.service.support.assembler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.game.domain.Court;
import umc.cockple.demo.domain.game.domain.Game;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.CourtStatus;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameBoardResultAssembler")
class GameBoardResultAssemblerTest {

    @Mock private ImageUrlResolver imageUrlResolver;

    @InjectMocks private GameBoardResultAssembler assembler;

    private GameBoard board;

    @BeforeEach
    void setUp() {
        board = GameFixture.gameBoard(1L);
    }

    @Test
    @DisplayName("활성 게임 입력 순서와 무관하게 PLAYING은 해당 코트에, WAITING은 waitingOrder 순으로 배치한다")
    void assemble_placesGamesRegardlessOfInputOrder() {
        // given
        Court court1 = GameFixture.court(10L, board, 1, "1번");
        Court court2 = GameFixture.court(11L, board, 2, "2번");
        GameBoardMember member = GameFixture.member(7L, board, "선수A", Level.A);
        Game playingOnCourt2 = GameFixture.playingGame(50L, board, court2, LocalDateTime.now(),
                GameFixture.player(member, 0));
        Game waitingFirst = GameFixture.waitingGame(51L, board, 1);
        // 새로 생성된 게임이 목록 끝에 붙는 경우(명령 트랜잭션 스냅샷)를 가정
        Game newlyCreated = GameFixture.waitingGame(52L, board, 2);

        // when
        GameBoardResult result = assembler.assemble(
                true, List.of(court1, court2), List.of(newlyCreated, playingOnCourt2, waitingFirst));

        // then
        assertThat(result.isGameHost()).isTrue();
        assertThat(result.courtCount()).isEqualTo(2);
        assertThat(result.courts()).extracting(GameBoardResult.CourtView::status)
                .containsExactly(CourtStatus.EMPTY, CourtStatus.PLAYING);
        assertThat(result.courts().get(1).game().gameId()).isEqualTo(50L);
        assertThat(result.waitings()).extracting(GameBoardResult.WaitingView::gameId)
                .containsExactly(51L, 52L);
    }

    @Test
    @DisplayName("코트와 활성 게임이 없으면 빈 보드를 반환한다")
    void assemble_emptyBoard() {
        // when
        GameBoardResult result = assembler.assemble(false, List.of(), List.of());

        // then
        assertThat(result.isGameHost()).isFalse();
        assertThat(result.courtCount()).isZero();
        assertThat(result.courts()).isEmpty();
        assertThat(result.waitings()).isEmpty();
    }
}
