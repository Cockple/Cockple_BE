package umc.cockple.demo.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateRequestDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateResponseDTO;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Exercise", description = "운동 관리 API")
public class ExerciseController {

    private final ExerciseCommandService exerciseCommandService;

    @PostMapping("/parties/{partyId}/exercises")
    @Operation(summary = "운동 생성",
            description = "모임 내에서 새로운 운동을 생성합니다. 모임장만 생성 가능합니다.")
    @ApiResponse(responseCode = "201", description = "운동 생성 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    public BaseResponse<ExerciseCreateResponseDTO> createExercise(
            @PathVariable Long partyId,
            @Valid @RequestBody ExerciseCreateRequestDTO request,
            Authentication authentication
    ){

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        // 서비스 호출
        ExerciseCreateResponseDTO response = exerciseCommandService.createExercise(
                partyId, memberId, request);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }
}
