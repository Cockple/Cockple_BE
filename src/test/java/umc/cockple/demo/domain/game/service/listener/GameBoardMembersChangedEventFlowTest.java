package umc.cockple.demo.domain.game.service.listener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardMemberDTO;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMapper;
import umc.cockple.demo.domain.game.presentation.mapper.GameBoardMemberMapper;
import umc.cockple.demo.domain.game.repository.GameBoardRepository;
import umc.cockple.demo.domain.game.service.query.GameBoardMemberQueryService;
import umc.cockple.demo.domain.game.service.query.GameBoardQueryService;
import umc.cockple.demo.domain.game.service.query.model.GameBoardMemberSearchQuery;
import umc.cockple.demo.domain.game.service.query.result.GameBoardMemberResult;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.game.service.websocket.broadcast.GameBoardBroadcaster;
import umc.cockple.demo.support.IntegrationTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;

@DisplayName("게임판 명단 변경 after-commit 실시간 동기화")
class GameBoardMembersChangedEventFlowTest extends IntegrationTestBase {

    private static final Long GAME_BOARD_ID = 100L;
    private static final Long ACTOR_MEMBER_ID = 200L;
    private static final GameBoardMemberSearchQuery NO_FILTERS =
            new GameBoardMemberSearchQuery(List.of(), null, null);

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private GameBoardRepository gameBoardRepository;

    @MockitoBean private GameBoardMemberQueryService gameBoardMemberQueryService;
    @MockitoBean private GameBoardMemberMapper gameBoardMemberMapper;
    @MockitoBean private GameBoardQueryService gameBoardQueryService;
    @MockitoBean private GameBoardMapper gameBoardMapper;
    @MockitoBean private GameBoardBroadcaster gameBoardBroadcaster;

    private GameBoardMemberDTO.Response membersDto;
    private GameBoardDTO.Response boardDto;

    @BeforeEach
    void setUp() {
        GameBoardMemberResult members = new GameBoardMemberResult(0, List.of());
        membersDto = new GameBoardMemberDTO.Response(0, List.of());
        GameBoardResult board = new GameBoardResult(0, List.of(), List.of());
        boardDto = new GameBoardDTO.Response(0, List.of(), List.of());

        given(gameBoardMemberQueryService.getMembersSnapshot(anyLong(), eq(NO_FILTERS))).willReturn(members);
        given(gameBoardMemberMapper.toResponse(members)).willReturn(membersDto);
        given(gameBoardQueryService.getBoard(eq(ACTOR_MEMBER_ID), anyLong())).willReturn(board);
        given(gameBoardMapper.toResponse(board)).willReturn(boardDto);
    }

    @AfterEach
    void tearDown() {
        gameBoardRepository.deleteAll();
    }

    @Test
    @DisplayName("트랜잭션이 커밋되면 두 snapshot을 전파한다")
    void committedEvent_broadcastsMembersAndBoard() {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(
                        GameBoardMembersChangedEvent.membersAndBoard(GAME_BOARD_ID, ACTOR_MEMBER_ID)));

        then(gameBoardBroadcaster).should(timeout(3000))
                .broadcastMembersUpdate(GAME_BOARD_ID, membersDto, null);
        then(gameBoardBroadcaster).should(timeout(3000))
                .broadcastBoardUpdate(GAME_BOARD_ID, boardDto, null);
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 snapshot을 조회하거나 전파하지 않는다")
    void rolledBackEvent_doesNotBroadcast() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(
                    GameBoardMembersChangedEvent.membersAndBoard(GAME_BOARD_ID, ACTOR_MEMBER_ID));
            status.setRollbackOnly();
        });

        then(gameBoardMemberQueryService).should(after(700).never())
                .getMembersSnapshot(anyLong(), eq(NO_FILTERS));
        then(gameBoardBroadcaster).should(never())
                .broadcastMembersUpdate(anyLong(), eq(membersDto), isNull());
        then(gameBoardBroadcaster).should(never())
                .broadcastBoardUpdate(anyLong(), eq(boardDto), isNull());
    }

    @Test
    @DisplayName("WebSocket 전파가 실패해도 명단 변경 트랜잭션은 커밋되고 다른 snapshot 전파를 시도한다")
    void broadcastFailure_doesNotRollbackCommittedChange() {
        willThrow(new RuntimeException("명단 전파 실패"))
                .given(gameBoardBroadcaster)
                .broadcastMembersUpdate(anyLong(), eq(membersDto), isNull());

        Long savedGameBoardId = transactionTemplate.execute(status -> {
            GameBoard saved = gameBoardRepository.save(GameBoard.create());
            eventPublisher.publishEvent(
                    GameBoardMembersChangedEvent.membersAndBoard(saved.getId(), ACTOR_MEMBER_ID));
            return saved.getId();
        });

        then(gameBoardBroadcaster).should(timeout(3000))
                .broadcastMembersUpdate(savedGameBoardId, membersDto, null);
        then(gameBoardBroadcaster).should(timeout(3000))
                .broadcastBoardUpdate(savedGameBoardId, boardDto, null);
        assertThat(gameBoardRepository.existsById(savedGameBoardId)).isTrue();
    }
}
