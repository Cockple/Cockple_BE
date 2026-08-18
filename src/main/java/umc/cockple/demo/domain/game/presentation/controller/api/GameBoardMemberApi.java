package umc.cockple.demo.domain.game.presentation.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardMemberDTO;
import umc.cockple.demo.global.response.BaseResponse;

import java.util.List;

@RequestMapping("/api/game-boards")
@GameApiTag
public interface GameBoardMemberApi {

    @GetMapping("/{gameBoardId}/gameBoardMembers")
    @Operation(summary = "게임판 명단 조회", description = """
            게임판 전체 명단 수와 필터된 명단을 조회합니다.

            - `level`: 한글 급수를 반복 전달하며 여러 값은 OR 조건입니다. (예: `level=A조&level=B조`)
            - `gender`: 한글 성별입니다. (예: `남성`)
            - `shuttlecockSubmitted`: 셔틀콕 제출 여부입니다.
            - 서로 다른 종류의 필터는 AND 조건입니다.
            - `totalCount`는 필터와 무관한 전체 명단 수입니다.
            """)
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "404", description = "게임판을 찾을 수 없음")
    ResponseEntity<BaseResponse<GameBoardMemberDTO.Response>> getMembers(
            @PathVariable Long gameBoardId,
            @RequestParam(value = "level", required = false) List<String> levels,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "shuttlecockSubmitted", required = false) Boolean shuttlecockSubmitted
    );
}
