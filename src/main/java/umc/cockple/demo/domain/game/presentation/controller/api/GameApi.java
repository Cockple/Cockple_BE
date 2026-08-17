package umc.cockple.demo.domain.game.presentation.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.game.presentation.dto.GameCompletedGameDTO;
import umc.cockple.demo.domain.game.presentation.dto.GameDuplicateCheckDTO;
import umc.cockple.demo.global.response.BaseResponse;

import java.util.List;

@RequestMapping("/api/game-boards")
@GameApiTag
public interface GameApi {

    @GetMapping("/{gameBoardId}/games/duplicate-check")
    @Operation(summary = "게임 중복 체크", description = """
            새 게임 인원을 고를 때, 선택한 멤버들의 쌍별 대전 이력을 반환합니다. (반복 매칭 회피용)

            - `gameBoardMemberId` 쿼리 파라미터를 여러 번 전달합니다. (예: `?gameBoardMemberId=7&gameBoardMemberId=8`)
            - 응답 `pairs` 는 선택 인원의 모든 쌍(4명이면 6쌍)입니다.
            - `count`: 이 게임판에서 **완료된 게임 중** 두 멤버가 함께 참여한 횟수
            - `playedInLastGame`: **직전(가장 최근) 완료 게임**에 두 멤버가 함께 있었는지
            """)
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "게임판 또는 멤버를 찾을 수 없음")
    ResponseEntity<BaseResponse<GameDuplicateCheckDTO.Response>> checkDuplicates(
            @PathVariable Long gameBoardId,
            @RequestParam("gameBoardMemberId") List<Long> gameBoardMemberIds
    );

    @GetMapping("/{gameBoardId}/games/completed")
    @Operation(summary = "완료된 게임 조회", description = """
            게임판에서 완료된 게임을 커서 기반으로 조회합니다.

            - `courtNo`(선택): 특정 코트에서 진행된 게임만 필터. 생략 시 전체.
            - `cursor`(선택): 이전 응답의 `nextCursor`. 생략 시 첫 페이지.
            - `size`: 페이지 크기.
            - 각 게임: `gameId`, `courtNo`, `durationMin`(완료-시작, 분), `players[]`.
            - `nextCursor`(다음 페이지 없으면 null), `hasNext`.
            """)
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "게임판을 찾을 수 없음")
    ResponseEntity<BaseResponse<GameCompletedGameDTO.Response>> getCompletedGames(
            @PathVariable Long gameBoardId,
            @RequestParam(value = "courtNo", required = false) Integer courtNo,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    );
}
