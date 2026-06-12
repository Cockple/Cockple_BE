package umc.cockple.demo.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.dto.ExerciseCreateDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDeleteDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseEditDetailDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseUpdateDTO;
import umc.cockple.demo.domain.exercise.service.ExerciseQueryService;
import umc.cockple.demo.domain.exercise.service.command.ExerciseCommandService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Exercise Lifecycle", description = "운동 생성/조회/수정/삭제 API")
public class ExerciseLifecycleController {

    private final ExerciseCommandService exerciseCommandService;
    private final ExerciseQueryService exerciseQueryService;

    @PostMapping("/parties/{partyId}/exercises")
    @Operation(summary = "운동 생성",
            description = "모임 내에서 새로운 운동을 생성합니다. 모임장과 부모임장만 생성 가능합니다.")
    @ApiResponse(responseCode = "201", description = "운동 생성 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    public ResponseEntity<BaseResponse<ExerciseCreateDTO.Response>> createExercise(
            @PathVariable Long partyId,
            @Valid @RequestBody ExerciseCreateDTO.Request request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCreateDTO.Response response = exerciseCommandService.createExercise(
                partyId, memberId, request);

        return BaseResponse.of(CommonSuccessCode.CREATED, response);
    }

    @DeleteMapping("/exercises/{exerciseId}")
    @Operation(summary = "운동 삭제",
            description = "모임장 또는 부모임장이 운동을 삭제합니다. 삭제된 운동의 모든 참여자와 게스트도 함께 삭제됩니다.")
    @ApiResponse(responseCode = "200", description = "운동 삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (모임장이 아님)")
    @ApiResponse(responseCode = "404", description = "운동을 찾을 수 없음")
    public ResponseEntity<BaseResponse<ExerciseDeleteDTO.Response>> deleteExercise(
            @PathVariable Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseDeleteDTO.Response response = exerciseCommandService.deleteExercise(
                exerciseId, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @PatchMapping("/exercises/{exerciseId}")
    @Operation(summary = "운동 수정",
            description = "모임장 또는 부모임장이 생성한 운동의 정보를 수정합니다. 이미 시작된 운동은 수정할 수 없습니다.")
    @ApiResponse(responseCode = "200", description = "운동 수정 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    @ApiResponse(responseCode = "403", description = "권한 없음 (모임장이 아님)")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 운동")
    public ResponseEntity<BaseResponse<ExerciseUpdateDTO.Response>> updateExercise(
            @PathVariable Long exerciseId,
            @Valid @RequestBody ExerciseUpdateDTO.Request request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseUpdateDTO.Response response = exerciseCommandService.updateExercise(
                exerciseId, memberId, request);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/{exerciseId}")
    @Operation(summary = "운동 상세 조회",
            description = "운동의 상세 정보를 조회합니다. 권한, 멤버 여부, 게스트 여부에 따라 반환되는 값이 달라집니다.")
    @ApiResponse(responseCode = "200", description = "운동 상세 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 운동")
    public ResponseEntity<BaseResponse<ExerciseDetailDTO.Response>> getExerciseDetail(
            @PathVariable Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                exerciseId, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/{exerciseId}/for-edit")
    @Operation(summary = "운동 수정용 상세 조회",
            description = "운동 수정을 위한 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "운동 수정용 상세 조회 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (모임장/부모임장이 아님)")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 운동")
    public ResponseEntity<BaseResponse<ExerciseEditDetailDTO.Response>> getExerciseForEdit(
            @PathVariable Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ExerciseEditDetailDTO.Response response = exerciseQueryService.getExerciseForEdit(exerciseId, memberId);
        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
