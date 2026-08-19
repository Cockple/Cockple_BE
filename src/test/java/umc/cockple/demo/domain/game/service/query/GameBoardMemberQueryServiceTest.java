package umc.cockple.demo.domain.game.service.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.file.service.ImageUrlResolver;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.service.query.model.GameBoardMemberSearchQuery;
import umc.cockple.demo.domain.game.service.query.result.GameBoardMemberResult;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardMemberReader;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.reader.GameReader;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("게임판 명단 조회 서비스")
class GameBoardMemberQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GAME_BOARD_ID = 2L;
    private static final GameBoardMemberSearchQuery NO_FILTERS =
            new GameBoardMemberSearchQuery(List.of(), null, null);

    @InjectMocks private GameBoardMemberQueryService gameBoardMemberQueryService;
    @Mock private GameBoardReader gameBoardReader;
    @Mock private GameBoardMemberReader gameBoardMemberReader;
    @Mock private GameReader gameReader;
    @Mock private GameBoardAccessValidator gameBoardAccessValidator;
    @Mock private ImageUrlResolver imageUrlResolver;

    @Test
    @DisplayName("운동 참가자 검증 후 명단을 조회한다")
    void getMembers_validatesParticipantBeforeQuery() {
        given(gameBoardReader.read(GAME_BOARD_ID)).willReturn(GameBoard.create());
        given(gameBoardMemberReader.countByGameBoard(GAME_BOARD_ID)).willReturn(0L);
        given(gameBoardMemberReader.readAllByFilters(GAME_BOARD_ID, List.of(), null, null))
                .willReturn(List.of());
        given(gameReader.readAllByGameBoardAndStatuses(
                GAME_BOARD_ID, List.of(GameStatus.PLAYING, GameStatus.WAITING)))
                .willReturn(List.of());

        GameBoardMemberResult result = gameBoardMemberQueryService.getMembers(
                MEMBER_ID, GAME_BOARD_ID, NO_FILTERS);

        assertThat(result.totalCount()).isZero();
        assertThat(result.gameBoardMembers()).isEmpty();
        then(gameBoardAccessValidator).should().validateViewer(GAME_BOARD_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("조회 권한이 없는 회원은 명단 데이터를 조회하지 않는다")
    void getMembers_rejectsUnauthorizedMemberBeforeQuery() {
        given(gameBoardReader.read(GAME_BOARD_ID)).willReturn(GameBoard.create());
        willThrow(new GameException(GameErrorCode.GAME_BOARD_VIEW_ACCESS_DENIED))
                .given(gameBoardAccessValidator).validateViewer(GAME_BOARD_ID, MEMBER_ID);

        assertThatThrownBy(() -> gameBoardMemberQueryService.getMembers(
                MEMBER_ID, GAME_BOARD_ID, NO_FILTERS))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(GameErrorCode.GAME_BOARD_VIEW_ACCESS_DENIED));

        verifyNoInteractions(gameBoardMemberReader, gameReader, imageUrlResolver);
    }

    @Test
    @DisplayName("내부 이벤트 스냅샷은 요청자 권한 검증 없이 명단을 조회한다")
    void getMembersSnapshot_loadsWithoutViewerValidation() {
        given(gameBoardReader.read(GAME_BOARD_ID)).willReturn(GameBoard.create());
        given(gameBoardMemberReader.countByGameBoard(GAME_BOARD_ID)).willReturn(0L);
        given(gameBoardMemberReader.readAllByFilters(GAME_BOARD_ID, List.of(), null, null))
                .willReturn(List.of());
        given(gameReader.readAllByGameBoardAndStatuses(
                GAME_BOARD_ID, List.of(GameStatus.PLAYING, GameStatus.WAITING)))
                .willReturn(List.of());

        GameBoardMemberResult result = gameBoardMemberQueryService.getMembersSnapshot(
                GAME_BOARD_ID, NO_FILTERS);

        assertThat(result.gameBoardMembers()).isEmpty();
        then(gameBoardAccessValidator).shouldHaveNoInteractions();
    }
}
