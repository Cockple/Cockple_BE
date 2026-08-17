package umc.cockple.demo.domain.exercise.presentation.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import umc.cockple.demo.domain.exercise.presentation.dto.gamehost.ExerciseGameHostDTO;
import umc.cockple.demo.global.response.BaseResponse;

@RequestMapping("/api")
@ExerciseApiTag
public interface ExerciseGameHostApi {

    @GetMapping("/exercises/{exerciseId}/game-host")
    @Operation(summary = "게임 진행자 조회",
            description = "모임장 또는 부모임장이 게임 진행자와 지정 가능한 활성 모임원 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "게임 진행자 조회 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (모임장/부모임장이 아님)")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 운동")
    ResponseEntity<BaseResponse<ExerciseGameHostDTO.Response>> getGameHost(
            @PathVariable Long exerciseId
    );
}
