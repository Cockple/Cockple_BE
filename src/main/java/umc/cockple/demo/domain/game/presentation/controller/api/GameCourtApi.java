package umc.cockple.demo.domain.game.presentation.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import umc.cockple.demo.domain.game.presentation.dto.GameCourtDTO;
import umc.cockple.demo.global.response.BaseResponse;

@RequestMapping("/api/game-boards")
@GameApiTag
public interface GameCourtApi {

    @PutMapping("/{gameBoardId}/courts")
    @Operation(summary = "게임 코트 관리 (#1)", description = """
            게임판의 코트를 일괄 관리합니다. (PUT으로 전체 교체 방식)

            - `courts` 는 관리 후의 **최종 코트 목록**이며, **배열 순서가 곧 코트 순서(courtNo, 1부터)** 가 됩니다.
            - `courts[].courtId` 가 **null 이면 새 코트 생성**, 값이 있으면 **해당 코트 유지(이름 변경)** 입니다.
            - 목록에서 **빠진 기존 코트는 삭제**됩니다.
            - `courts[].courtName` 이 비어 있으면 **'N번 코트' 기본값**이 부여됩니다.

            응답으로 최종 코트 목록을 반환하며, **새로 생성된 코트는 서버가 발급한 `courtId`** 가 채워집니다.
            (별도로 최신 상태가 필요하면 보드 조회(GET /api/game-boards/{gameBoardId})를 사용하세요.)
            """)
    @ApiResponse(responseCode = "200", description = "코트 관리 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류")
    @ApiResponse(responseCode = "404", description = "게임판 또는 코트를 찾을 수 없음")
    ResponseEntity<BaseResponse<GameCourtDTO.Response>> manageCourts(
            @PathVariable Long gameBoardId,
            @Valid @RequestBody GameCourtDTO.Request request
    );
}
