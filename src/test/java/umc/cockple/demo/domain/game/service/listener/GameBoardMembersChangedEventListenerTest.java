package umc.cockple.demo.domain.game.service.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardMemberDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMapper;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMemberMapper;
import umc.cockple.demo.domain.game.service.query.GameBoardMemberQueryService;
import umc.cockple.demo.domain.game.service.query.GameBoardQueryService;
import umc.cockple.demo.domain.game.service.query.model.GameBoardMemberSearchQuery;
import umc.cockple.demo.domain.game.service.query.result.GameBoardMemberResult;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.game.service.websocket.broadcast.GameBoardBroadcaster;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameBoardMembersChangedEventListener")
class GameBoardMembersChangedEventListenerTest {

    private static final Long GAME_BOARD_ID = 1L;
    private static final Long ACTOR_MEMBER_ID = 2L;
    private static final GameBoardMemberSearchQuery NO_FILTERS =
            new GameBoardMemberSearchQuery(List.of(), null, null);

    @InjectMocks private GameBoardMembersChangedEventListener listener;
    @Mock private GameBoardMemberQueryService gameBoardMemberQueryService;
    @Mock private GameBoardMemberMapper gameBoardMemberMapper;
    @Mock private GameBoardQueryService gameBoardQueryService;
    @Mock private GameBoardMapper gameBoardMapper;
    @Mock private GameBoardBroadcaster gameBoardBroadcaster;

    private GameBoardMembersChangedEvent event;
    private GameBoardMemberDTO.Response membersDto;
    private GameBoardDTO.Response boardDto;

    @BeforeEach
    void setUp() {
        event = new GameBoardMembersChangedEvent(GAME_BOARD_ID, ACTOR_MEMBER_ID);
        GameBoardMemberResult members = new GameBoardMemberResult(0, List.of());
        membersDto = new GameBoardMemberDTO.Response(0, List.of());
        GameBoardResult board = new GameBoardResult(0, List.of(), List.of());
        boardDto = new GameBoardDTO.Response(0, List.of(), List.of());

        given(gameBoardMemberQueryService.getMembers(GAME_BOARD_ID, NO_FILTERS)).willReturn(members);
        given(gameBoardMemberMapper.toResponse(members)).willReturn(membersDto);
        given(gameBoardQueryService.getBoard(ACTOR_MEMBER_ID, GAME_BOARD_ID)).willReturn(board);
        given(gameBoardMapper.toResponse(board)).willReturn(boardDto);
    }

    @Test
    @DisplayName("필터 없는 최신 명단과 게임판 snapshot을 전체 구독 세션에 전파한다")
    void handleMembersChanged_broadcastsMembersAndBoardSnapshots() {
        listener.handleMembersChanged(event);

        then(gameBoardBroadcaster).should()
                .broadcastMembersUpdate(GAME_BOARD_ID, membersDto, null);
        then(gameBoardBroadcaster).should()
                .broadcastBoardUpdate(GAME_BOARD_ID, boardDto, null);
    }

    @Test
    @DisplayName("명단 snapshot 전파가 실패해도 게임판 snapshot 전파를 계속한다")
    void handleMembersChanged_continuesBoardBroadcastAfterMembersFailure() {
        willThrow(new RuntimeException("명단 전파 실패"))
                .given(gameBoardBroadcaster).broadcastMembersUpdate(GAME_BOARD_ID, membersDto, null);

        assertThatCode(() -> listener.handleMembersChanged(event)).doesNotThrowAnyException();

        then(gameBoardBroadcaster).should()
                .broadcastBoardUpdate(GAME_BOARD_ID, boardDto, null);
    }
}
