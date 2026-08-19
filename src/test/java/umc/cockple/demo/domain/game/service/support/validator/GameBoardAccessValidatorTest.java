package umc.cockple.demo.domain.game.service.support.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.exercise.domain.Exercise;
import umc.cockple.demo.domain.exercise.repository.ExerciseRepository;
import umc.cockple.demo.domain.game.exception.GameErrorCode;
import umc.cockple.demo.domain.game.exception.GameException;
import umc.cockple.demo.domain.game.repository.GameBoardMemberRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameBoardAccessValidator")
class GameBoardAccessValidatorTest {

    private static final Long GAME_BOARD_ID = 1L;
    private static final Long GAME_HOST_ID = 2L;

    @InjectMocks private GameBoardAccessValidator gameBoardAccessValidator;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private GameBoardMemberRepository gameBoardMemberRepository;

    private Exercise exercise;

    @BeforeEach
    void setUp() {
        exercise = Exercise.builder().gameHostId(GAME_HOST_ID).build();
    }

    @Test
    @DisplayName("연결된 운동의 게임 진행자는 게임판을 관리할 수 있다")
    void validateGameHost_allowsExerciseGameHost() {
        given(exerciseRepository.findByGameBoardId(GAME_BOARD_ID)).willReturn(Optional.of(exercise));

        assertThatCode(() -> gameBoardAccessValidator.validateGameHost(GAME_BOARD_ID, GAME_HOST_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("게임 진행자가 아니면 GAME_BOARD_ACCESS_DENIED 예외를 던진다")
    void validateGameHost_deniesOtherMember() {
        given(exerciseRepository.findByGameBoardId(GAME_BOARD_ID)).willReturn(Optional.of(exercise));

        assertThatThrownBy(() -> gameBoardAccessValidator.validateGameHost(GAME_BOARD_ID, 999L))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GameErrorCode.GAME_BOARD_ACCESS_DENIED));
    }

    @Test
    @DisplayName("운동과 연결된 게임판이 없으면 GAME_BOARD_NOT_FOUND 예외를 던진다")
    void validateGameHost_throwsWhenGameBoardDoesNotExist() {
        given(exerciseRepository.findByGameBoardId(GAME_BOARD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> gameBoardAccessValidator.validateGameHost(GAME_BOARD_ID, GAME_HOST_ID))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GameErrorCode.GAME_BOARD_NOT_FOUND));
    }

    @Test
    @DisplayName("게임판 명단에 연결된 회원은 운동 참가자로 조회 권한이 있다")
    void validateViewer_allowsExerciseParticipant() {
        given(gameBoardMemberRepository.existsByGameBoardIdAndMemberId(GAME_BOARD_ID, GAME_HOST_ID))
                .willReturn(true);

        assertThatCode(() -> gameBoardAccessValidator.validateViewer(GAME_BOARD_ID, GAME_HOST_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("운동 참가자가 아니어도 게임 진행자는 조회 권한이 있다")
    void validateViewer_allowsGameHost() {
        given(gameBoardMemberRepository.existsByGameBoardIdAndMemberId(GAME_BOARD_ID, GAME_HOST_ID))
                .willReturn(false);
        given(exerciseRepository.findByGameBoardId(GAME_BOARD_ID)).willReturn(Optional.of(exercise));

        assertThatCode(() -> gameBoardAccessValidator.validateViewer(GAME_BOARD_ID, GAME_HOST_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("운동 참가자와 게임 진행자가 아니면 GAME_BOARD_VIEW_ACCESS_DENIED 예외를 던진다")
    void validateViewer_deniesUnauthorizedMember() {
        Long unauthorizedMemberId = 999L;
        given(gameBoardMemberRepository.existsByGameBoardIdAndMemberId(
                GAME_BOARD_ID, unauthorizedMemberId))
                .willReturn(false);
        given(exerciseRepository.findByGameBoardId(GAME_BOARD_ID)).willReturn(Optional.of(exercise));

        assertThatThrownBy(() -> gameBoardAccessValidator.validateViewer(
                GAME_BOARD_ID, unauthorizedMemberId))
                .isInstanceOfSatisfying(GameException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(GameErrorCode.GAME_BOARD_VIEW_ACCESS_DENIED));
    }
}
