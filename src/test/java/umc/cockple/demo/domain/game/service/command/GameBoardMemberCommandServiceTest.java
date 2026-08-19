package umc.cockple.demo.domain.game.service.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import umc.cockple.demo.domain.game.domain.GameBoard;
import umc.cockple.demo.domain.game.domain.GameBoardMember;
import umc.cockple.demo.domain.game.enums.AgeGroup;
import umc.cockple.demo.domain.game.events.GameBoardMembersChangedEvent;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberCreateCommand;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberParticipationCommand;
import umc.cockple.demo.domain.game.service.command.model.GameBoardMemberUpdateCommand;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardMemberReader;
import umc.cockple.demo.domain.game.service.support.reader.GameBoardReader;
import umc.cockple.demo.domain.game.service.support.reader.GameReader;
import umc.cockple.demo.domain.game.service.support.validator.GameBoardAccessValidator;
import umc.cockple.demo.global.enums.Gender;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.support.fixture.GameFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameBoardMemberCommandService")
class GameBoardMemberCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GAME_BOARD_ID = 2L;
    private static final Long GAME_BOARD_MEMBER_ID = 3L;

    @InjectMocks private GameBoardMemberCommandService gameBoardMemberCommandService;
    @Mock private GameBoardReader gameBoardReader;
    @Mock private GameBoardMemberReader gameBoardMemberReader;
    @Mock private GameReader gameReader;
    @Mock private GameBoardAccessValidator gameBoardAccessValidator;
    @Mock private GameBoardMemberRepository gameBoardMemberRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private GameBoard gameBoard;
    private GameBoardMemberCreateCommand command;

    @BeforeEach
    void setUp() {
        gameBoard = GameFixture.gameBoard(GAME_BOARD_ID);
        command = new GameBoardMemberCreateCommand(
                GAME_BOARD_ID, "김선수", Gender.MALE, Level.D, AgeGroup.THIRTIES);
    }

    @Test
    @DisplayName("게임 진행자가 수동 명단을 기본값과 함께 생성하고 변경 이벤트를 발행한다")
    void createMember_createsManualMemberAndPublishesEvent() {
        GameBoardMember savedMember = GameBoardMember.builder().id(GAME_BOARD_MEMBER_ID).build();
        given(gameBoardReader.read(GAME_BOARD_ID)).willReturn(gameBoard);
        given(gameBoardMemberRepository.save(any(GameBoardMember.class))).willReturn(savedMember);
        ArgumentCaptor<GameBoardMember> memberCaptor = ArgumentCaptor.forClass(GameBoardMember.class);

        Long result = gameBoardMemberCommandService.createMember(MEMBER_ID, command);

        assertThat(result).isEqualTo(GAME_BOARD_MEMBER_ID);
        then(gameBoardAccessValidator).should().validateGameHost(GAME_BOARD_ID, MEMBER_ID);
        then(gameBoardMemberRepository).should().save(memberCaptor.capture());
        GameBoardMember createdMember = memberCaptor.getValue();
        assertThat(createdMember.getGameBoard()).isSameAs(gameBoard);
        assertThat(createdMember.getMember()).isNull();
        assertThat(createdMember.getGuest()).isNull();
        assertThat(createdMember.getName()).isEqualTo("김선수");
        assertThat(createdMember.getGender()).isEqualTo(Gender.MALE);
        assertThat(createdMember.getLevel()).isEqualTo(Level.D);
        assertThat(createdMember.getAgeGroup()).isEqualTo(AgeGroup.THIRTIES);
        assertThat(createdMember.getShuttlecockSubmitted()).isFalse();
        assertThat(createdMember.getParticipating()).isTrue();
        assertThat(createdMember.getGameCount()).isZero();
        then(eventPublisher).should()
                .publishEvent(GameBoardMembersChangedEvent.membersAndBoard(GAME_BOARD_ID, MEMBER_ID));
    }

    @Test
    @DisplayName("게임 진행자가 아니면 명단을 생성하거나 이벤트를 발행하지 않는다")
    void createMember_deniesNonGameHost() {
        willThrow(new GameException(GameErrorCode.GAME_BOARD_ACCESS_DENIED))
                .given(gameBoardAccessValidator).validateGameHost(GAME_BOARD_ID, MEMBER_ID);

        assertThatThrownBy(() -> gameBoardMemberCommandService.createMember(MEMBER_ID, command))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GameErrorCode.GAME_BOARD_ACCESS_DENIED));
        then(gameBoardReader).should(never()).read(any());
        then(gameBoardMemberRepository).should(never()).save(any());
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("게임 진행자가 명단 정보를 수정하고 연령대를 제거한 뒤 변경 이벤트를 발행한다")
    void updateMember_updatesInfoAndPublishesEvent() {
        GameBoardMember gameBoardMember = GameBoardMember.create(
                "수정 전", Gender.MALE, Level.D, AgeGroup.THIRTIES);
        GameBoardMemberUpdateCommand updateCommand = new GameBoardMemberUpdateCommand(
                GAME_BOARD_ID, GAME_BOARD_MEMBER_ID, "수정 후", Gender.FEMALE, Level.A, null);
        given(gameBoardMemberReader.read(GAME_BOARD_ID, GAME_BOARD_MEMBER_ID))
                .willReturn(gameBoardMember);

        gameBoardMemberCommandService.updateMember(MEMBER_ID, updateCommand);

        then(gameBoardAccessValidator).should().validateGameHost(GAME_BOARD_ID, MEMBER_ID);
        assertThat(gameBoardMember.getName()).isEqualTo("수정 후");
        assertThat(gameBoardMember.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(gameBoardMember.getLevel()).isEqualTo(Level.A);
        assertThat(gameBoardMember.getAgeGroup()).isNull();
        then(eventPublisher).should()
                .publishEvent(GameBoardMembersChangedEvent.membersAndBoard(GAME_BOARD_ID, MEMBER_ID));
    }

    @Test
    @DisplayName("활성 게임에 포함되지 않은 선수를 참여 해제하고 이벤트를 발행한다")
    void changeParticipation_deactivatesIdleMemberAndPublishesEvent() {
        GameBoardMember gameBoardMember = GameBoardMember.create(
                "선수", Gender.MALE, Level.D, AgeGroup.THIRTIES);
        GameBoardMemberParticipationCommand participationCommand =
                new GameBoardMemberParticipationCommand(GAME_BOARD_ID, GAME_BOARD_MEMBER_ID, false);
        given(gameBoardMemberReader.read(GAME_BOARD_ID, GAME_BOARD_MEMBER_ID))
                .willReturn(gameBoardMember);
        given(gameReader.existsByGameBoardMemberAndStatuses(
                eq(GAME_BOARD_MEMBER_ID), any()))
                .willReturn(false);

        gameBoardMemberCommandService.changeParticipation(MEMBER_ID, participationCommand);

        assertThat(gameBoardMember.getParticipating()).isFalse();
        then(gameBoardReader).should().readForUpdate(GAME_BOARD_ID);
        then(eventPublisher).should()
                .publishEvent(GameBoardMembersChangedEvent.membersAndBoard(GAME_BOARD_ID, MEMBER_ID));
    }

    @Test
    @DisplayName("게임판 락 획득 후 게임 진행자가 아니면 명단을 조회하지 않는다")
    void changeParticipation_deniesNonGameHostAfterLock() {
        GameBoardMemberParticipationCommand participationCommand =
                new GameBoardMemberParticipationCommand(GAME_BOARD_ID, GAME_BOARD_MEMBER_ID, false);
        willThrow(new GameException(GameErrorCode.GAME_BOARD_ACCESS_DENIED))
                .given(gameBoardAccessValidator).validateGameHost(GAME_BOARD_ID, MEMBER_ID);

        assertThatThrownBy(() -> gameBoardMemberCommandService.changeParticipation(
                MEMBER_ID, participationCommand))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GameErrorCode.GAME_BOARD_ACCESS_DENIED));

        then(gameBoardReader).should().readForUpdate(GAME_BOARD_ID);
        then(gameBoardMemberReader).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("활성 게임 선수는 참여 해제할 수 없다")
    void changeParticipation_rejectsActiveMemberDeactivation() {
        GameBoardMember gameBoardMember = GameBoardMember.create(
                "선수", Gender.MALE, Level.D, AgeGroup.THIRTIES);
        GameBoardMemberParticipationCommand participationCommand =
                new GameBoardMemberParticipationCommand(GAME_BOARD_ID, GAME_BOARD_MEMBER_ID, false);
        given(gameBoardMemberReader.read(GAME_BOARD_ID, GAME_BOARD_MEMBER_ID))
                .willReturn(gameBoardMember);
        given(gameReader.existsByGameBoardMemberAndStatuses(
                eq(GAME_BOARD_MEMBER_ID), any()))
                .willReturn(true);

        assertThatThrownBy(() -> gameBoardMemberCommandService.changeParticipation(MEMBER_ID, participationCommand))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(GameErrorCode.ACTIVE_GAME_MEMBER_CANNOT_BE_INACTIVE));
        assertThat(gameBoardMember.getParticipating()).isTrue();
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("현재 값과 같은 참여 상태 요청은 조회와 이벤트 없이 성공한다")
    void changeParticipation_sameValueIsIdempotent() {
        GameBoardMember gameBoardMember = GameBoardMember.create(
                "선수", Gender.MALE, Level.D, AgeGroup.THIRTIES);
        GameBoardMemberParticipationCommand participationCommand =
                new GameBoardMemberParticipationCommand(GAME_BOARD_ID, GAME_BOARD_MEMBER_ID, true);
        given(gameBoardMemberReader.read(GAME_BOARD_ID, GAME_BOARD_MEMBER_ID))
                .willReturn(gameBoardMember);

        gameBoardMemberCommandService.changeParticipation(MEMBER_ID, participationCommand);

        assertThat(gameBoardMember.getParticipating()).isTrue();
        then(gameReader).shouldHaveNoInteractions();
        then(eventPublisher).should(never()).publishEvent(any());
    }
}
