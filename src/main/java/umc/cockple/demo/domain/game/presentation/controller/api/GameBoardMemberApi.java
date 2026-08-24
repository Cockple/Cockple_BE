package umc.cockple.demo.domain.game.presentation.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.game.presentation.dto.GameBoardMemberDTO;
import umc.cockple.demo.global.response.BaseResponse;

import java.util.List;

@RequestMapping("/api/game-boards")
@GameApiTag
public interface GameBoardMemberApi {

    @PatchMapping("/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}/participation")
    @Operation(summary = "게임판 명단 참여 상태 변경", description = """
            게임 진행자가 명단의 참여/불참 상태를 변경합니다.

            - `participating`은 필수 Boolean입니다.
            - PLAYING 또는 WAITING 게임에 포함된 선수는 참여 해제할 수 없습니다.
            - 현재 값과 같은 요청은 성공하는 멱등 동작입니다.
            """)
    @ApiResponse(responseCode = "200", description = "변경 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 활성 게임 선수 참여 해제")
    @ApiResponse(responseCode = "403", description = "게임판 관리 권한 없음")
    @ApiResponse(responseCode = "404", description = "게임판 또는 명단을 찾을 수 없음")
    ResponseEntity<BaseResponse<Void>> changeParticipation(
            @PathVariable Long gameBoardId,
            @PathVariable Long gameBoardMemberId,
            @Valid @RequestBody GameBoardMemberDTO.ParticipationRequest request
    );

    @PatchMapping("/{gameBoardId}/gameBoardMembers/{gameBoardMemberId}")
    @Operation(summary = "게임판 명단 정보 수정", description = """
            게임 진행자가 명단의 이름, 성별, 급수, 연령대를 수정합니다.

            - `name`, `gender`, `level`은 필수입니다.
            - `ageGroup`을 null로 전달하거나 생략하면 기존 연령대를 제거합니다.
            - 진행 또는 대기 중인 게임에 포함된 선수도 정보를 수정할 수 있습니다.
            """)
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류")
    @ApiResponse(responseCode = "403", description = "게임판 관리 권한 없음")
    @ApiResponse(responseCode = "404", description = "게임판 또는 명단을 찾을 수 없음")
    ResponseEntity<BaseResponse<Void>> updateMember(
            @PathVariable Long gameBoardId,
            @PathVariable Long gameBoardMemberId,
            @Valid @RequestBody GameBoardMemberDTO.UpdateRequest request
    );

    @PostMapping("/{gameBoardId}/gameBoardMembers")
    @Operation(summary = "게임판 명단 추가", description = """
            게임 진행자가 수동 명단을 추가합니다.

            - `name`, `gender`, `level`은 필수입니다.
            - `gender`, `level`, `ageGroup`은 한글 표시값으로 입력합니다.
            - 수동 명단은 회원과 연결되지 않으며 참여 상태로 생성됩니다.
            """)
    @ApiResponse(responseCode = "200", description = "추가 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류")
    @ApiResponse(responseCode = "403", description = "게임판 관리 권한 없음")
    @ApiResponse(responseCode = "404", description = "게임판을 찾을 수 없음")
    ResponseEntity<BaseResponse<GameBoardMemberDTO.CreateResponse>> createMember(
            @PathVariable Long gameBoardId,
            @Valid @RequestBody GameBoardMemberDTO.CreateRequest request
    );

    @GetMapping("/{gameBoardId}/gameBoardMembers")
    @Operation(summary = "게임판 명단 조회", description = """
            게임판 전체 명단 수와 필터된 명단을 조회합니다.

            - `level`: 한글 급수를 반복 전달하며 여러 값은 OR 조건입니다. (예: `level=A조&level=B조`)
            - `gender`: 한글 성별입니다. (예: `남성`)
            - `shuttlecockSubmitted`: 셔틀콕 제출 여부입니다.
            - 서로 다른 종류의 필터는 AND 조건입니다.
            - `totalCount`는 필터와 무관한 전체 명단 수입니다.
            - 명단의 `gender`, `level`, `ageGroup`은 한글 표시값으로 반환합니다.
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
