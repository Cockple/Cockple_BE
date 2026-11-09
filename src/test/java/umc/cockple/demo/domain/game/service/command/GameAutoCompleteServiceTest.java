package umc.cockple.demo.domain.game.service.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.cockple.demo.domain.game.enums.GameStatus;
import umc.cockple.demo.domain.game.repository.GameRepository;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameAutoCompleteServiceTest {
    @Mock GameRepository gameRepository;
    @Mock GameAutoCompleteBoardService boardService;
    @InjectMocks GameAutoCompleteService service;

    @Test
    void continuesWithOtherBoardsAfterFailure() {
        when(gameRepository.findStaleGameBoardIds(eq(GameStatus.PLAYING), any()))
                .thenReturn(List.of(1L, 2L));
        when(boardService.completeStaleGames(eq(1L), any())).thenThrow(new IllegalStateException("commit failed"));
        when(boardService.completeStaleGames(eq(2L), any())).thenReturn(2);
        assertThat(service.autoCompleteStaleGames()).isEqualTo(2);
    }
}
