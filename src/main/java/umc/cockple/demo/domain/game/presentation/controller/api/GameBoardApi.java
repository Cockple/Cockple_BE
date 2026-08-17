package umc.cockple.demo.domain.game.presentation.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardDTO;
import umc.cockple.demo.global.response.BaseResponse;

@RequestMapping("/api/game-boards")
@GameApiTag
public interface GameBoardApi {

    @GetMapping("/{gameBoardId}")
    @Operation(summary = "코트 보드 조회 (#2)", description = """
            게임판의 코트와 대기열을 조회합니다. (명단은 제외)

            - `courtCount` 가 0 이면 코트가 없는 상태이므로 프론트에서 코트 관리 화면으로 유도합니다.
            - `courts[].status` 는 `EMPTY`(빈 코트) / `PLAYING`(게임 진행 중) 이며, `PLAYING` 일 때만 `game` 이 채워집니다.
            - `waitings[]` 는 대기열의 게임 목록으로 `waitingOrder` 오름차순입니다.
            - 각 게임의 `players[]` 는 `playerOrder` 순서입니다.
            """)
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "게임판을 찾을 수 없음")
    ResponseEntity<BaseResponse<GameBoardDTO.Response>> getBoard(
            @PathVariable Long gameBoardId
    );
}
