package umc.cockple.demo.domain.game.presentation.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.game.presentation.dto.GameCompletedGameDTO;
import umc.cockple.demo.domain.game.presentation.dto.GameDuplicateCheckDTO;
import umc.cockple.demo.domain.game.presentation.dto.GameRandomMatchDTO;
import umc.cockple.demo.global.response.BaseResponse;

import java.util.List;

@RequestMapping("/api/game-boards")
@GameApiTag
public interface GameApi {

    @PostMapping("/{gameBoardId}/games/random-match")
    @Operation(summary = "게임 랜덤 매칭", description = """
            현재 게임판 명단에서 바로 게임에 참여할 수 있는 선수 4명을 추천합니다.

            - 게임 진행자만 호출할 수 있습니다.
            - 불참, 대기 중, 경기 시작 후 10분 미만인 선수와 급수없는 선수는 후보에서 제외합니다.
            - 혼복·남복·여복 중 가능한 타입 하나를 내부에서 무작위로 선택합니다.
            - 응답 ID는 팀 구분 없이 오름차순이며 매치 타입은 노출하지 않습니다.
            - 추천 결과는 저장하지 않습니다. 확정 시 기존 WebSocket `CREATE_GAME`을 호출해야 합니다.
            """)
    @ApiResponse(responseCode = "200", description = "랜덤 매칭 성공")
    @ApiResponse(responseCode = "400", description = "가용 인원·성별 구성 부족 또는 조합 생성 실패")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "게임 진행자가 아닌 회원")
    @ApiResponse(responseCode = "404", description = "게임판을 찾을 수 없음")
    ResponseEntity<BaseResponse<GameRandomMatchDTO.Response>> randomMatch(
            @PathVariable Long gameBoardId
    );

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
