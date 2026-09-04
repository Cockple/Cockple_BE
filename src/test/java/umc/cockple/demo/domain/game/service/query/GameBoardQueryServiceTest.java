package umc.cockple.demo.domain.game.service.query;

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
import umc.cockple.demo.domain.game.repository.CourtRepository;
import umc.cockple.demo.domain.game.repository.GameRepository;
import umc.cockple.demo.domain.game.service.query.result.GameBoardResult;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;
import umc.cockple.demo.domain.member.domain.Member;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameBoardQueryService")
class GameBoardQueryServiceTest {

    @Mock private GameBoardReader gameBoardReader;
    @Mock private CourtRepository courtRepository;
    @Mock private GameRepository gameRepository;
    @Mock private GameBoardAccessValidator gameBoardAccessValidator;
    @Mock private ImageUrlResolver imageUrlResolver;

    @InjectMocks private GameBoardQueryService gameBoardQueryService;

    private static final Long MEMBER_ID = 100L;
    private static final Long BOARD_ID = 1L;
    private GameBoard board;

    @BeforeEach
    void setUp() {
        board = GameFixture.gameBoard(BOARD_ID);
    }

    @Test
    @DisplayName("PLAYING 코트는 status PLAYING+game, 빈 코트는 EMPTY+null 로 반환하고 courtCount를 채운다")
    void getBoard_courtsWithStatus() {
        // given
        Court court1 = GameFixture.court(10L, board, 1, "1번");
        Court court2 = GameFixture.court(11L, board, 2, "2번");
        GameBoardMember member = GameFixture.member(7L, board, "김세익", Level.SEMI_EXPERT);
        Game playing = GameFixture.playingGame(50L, board, court1, LocalDateTime.now(),
                GameFixture.player(member, 0));

        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(gameBoardAccessValidator.isGameHost(BOARD_ID, MEMBER_ID)).willReturn(true);
        given(courtRepository.findByGameBoardIdOrderByCourtNoAsc(BOARD_ID)).willReturn(List.of(court1, court2));
        given(gameRepository.findByGameBoardIdAndStatusInWithPlayers(eq(BOARD_ID), anyList()))
                .willReturn(List.of(playing));

        // when
        GameBoardResult result = gameBoardQueryService.getBoard(MEMBER_ID, BOARD_ID);

        // then
        assertThat(result.isGameHost()).isTrue();
        assertThat(result.courtCount()).isEqualTo(2);
        assertThat(result.courts()).extracting(GameBoardResult.CourtView::status)
                .containsExactly(CourtStatus.PLAYING, CourtStatus.EMPTY);

        GameBoardResult.CourtView playingCourt = result.courts().get(0);
        assertThat(playingCourt.game()).isNotNull();
        assertThat(playingCourt.game().gameId()).isEqualTo(50L);

        assertThat(result.courts().get(1).game()).isNull();
    }

    @Test
    @DisplayName("players는 playerOrder 순서로, gameBoardMemberId·이름·급수를 담아 반환한다")
    void getBoard_playersOrderedWithInfo() {
        // given
        Court court1 = GameFixture.court(10L, board, 1, "1번");
        GameBoardMember a = GameFixture.member(7L, board, "선수A", Level.A);
        GameBoardMember b = GameFixture.member(8L, board, "선수B", Level.SEMI_EXPERT);
        // 입력 순서를 일부러 뒤집어 정렬을 검증
        Game playing = GameFixture.playingGame(50L, board, court1, LocalDateTime.now(),
                GameFixture.player(b, 1), GameFixture.player(a, 0));

        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(courtRepository.findByGameBoardIdOrderByCourtNoAsc(BOARD_ID)).willReturn(List.of(court1));
        given(gameRepository.findByGameBoardIdAndStatusInWithPlayers(eq(BOARD_ID), anyList()))
                .willReturn(List.of(playing));

        // when
        GameBoardResult result = gameBoardQueryService.getBoard(MEMBER_ID, BOARD_ID);

        // then
        List<GameBoardResult.PlayerView> players = result.courts().get(0).game().players();
        assertThat(players).extracting(GameBoardResult.PlayerView::playerOrder).containsExactly(0, 1);
        assertThat(players.get(0).gameBoardMemberId()).isEqualTo(7L);
        assertThat(players.get(0).name()).isEqualTo("선수A");
        assertThat(players.get(0).level()).isEqualTo(Level.A);
        // 회원 계정이 없는(게스트성) 명단 멤버는 프로필 이미지가 null
        assertThat(players.get(0).profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("회원 계정이 연결된 플레이어는 프로필 이미지 URL을 담아 반환한다")
    void getBoard_resolvesProfileImageForMemberPlayer() {
        // given
        Court court1 = GameFixture.court(10L, board, 1, "1번");
        Member account = Member.builder().id(200L).build();
        GameBoardMember withAccount = GameBoardMember.builder()
                .id(9L)
                .gameBoard(board)
                .member(account)
                .name("회원선수")
                .gender(Gender.MALE)
                .level(Level.A)
                .shuttlecockSubmitted(false)
                .participating(true)
                .gameCount(0)
                .build();
        Game playing = GameFixture.playingGame(51L, board, court1, LocalDateTime.now(),
                GameFixture.player(withAccount, 0));

        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(courtRepository.findByGameBoardIdOrderByCourtNoAsc(BOARD_ID)).willReturn(List.of(court1));
        given(gameRepository.findByGameBoardIdAndStatusInWithPlayers(eq(BOARD_ID), anyList()))
                .willReturn(List.of(playing));
        given(imageUrlResolver.resolve(any(), any())).willReturn("https://img/p.jpg");

        // when
        GameBoardResult result = gameBoardQueryService.getBoard(MEMBER_ID, BOARD_ID);

        // then
        GameBoardResult.PlayerView player = result.courts().get(0).game().players().get(0);
        assertThat(player.profileImageUrl()).isEqualTo("https://img/p.jpg");
    }

    @Test
    @DisplayName("대기열은 waitingOrder 오름차순으로 정렬해 반환한다")
    void getBoard_waitingsSortedByOrder() {
        // given
        Game waitingSecond = GameFixture.waitingGame(52L, board, 2);
        Game waitingFirst = GameFixture.waitingGame(53L, board, 1);

        given(gameBoardReader.read(BOARD_ID)).willReturn(board);
        given(courtRepository.findByGameBoardIdOrderByCourtNoAsc(BOARD_ID)).willReturn(List.of());
        given(gameRepository.findByGameBoardIdAndStatusInWithPlayers(eq(BOARD_ID), anyList()))
                .willReturn(List.of(waitingSecond, waitingFirst));

        // when
        GameBoardResult result = gameBoardQueryService.getBoard(MEMBER_ID, BOARD_ID);

        // then
        assertThat(result.courtCount()).isZero();
        assertThat(result.waitings()).extracting(GameBoardResult.WaitingView::gameId)
                .containsExactly(53L, 52L);
        assertThat(result.waitings()).extracting(GameBoardResult.WaitingView::waitingOrder)
                .containsExactly(1, 2);
    }
}
