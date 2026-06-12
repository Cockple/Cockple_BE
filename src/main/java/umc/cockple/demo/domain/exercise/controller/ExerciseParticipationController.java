package umc.cockple.demo.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.exercise.dto.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseJoinDTO;
import umc.cockple.demo.domain.exercise.service.command.ExerciseCommandService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequestMapping("/api/exercises/{exerciseId}/participants")
@RequiredArgsConstructor
@Validated
@Tag(name = "Exercise Participation", description = "운동 참여 신청/취소 API")
public class ExerciseParticipationController {

    private final ExerciseCommandService exerciseCommandService;

    @PostMapping
    @Operation(summary = "운동 신청",
            description = "모임에서 생성한 운동에 신청합니다. 외부 게스트 허용일 경우 모임 멤버가 아니어도 가능합니다.")
    @ApiResponse(responseCode = "200", description = "운동 신청 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    @ApiResponse(responseCode = "403", description = "권한 없음, 급수 위반")
    public ResponseEntity<BaseResponse<ExerciseJoinDTO.Response>> joinExercise(
            @PathVariable Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseJoinDTO.Response response = exerciseCommandService.joinExercise(
                exerciseId, memberId);

        return BaseResponse.of(CommonSuccessCode.CREATED, response);
    }

    @DeleteMapping("/my")
    @Operation(summary = "운동 참여 취소",
            description = "사용자가 본인의 운동 참여를 취소합니다.")
    @ApiResponse(responseCode = "200", description = "운동 참여 취소 성공")
    @ApiResponse(responseCode = "400", description = "취소할 수 없는 상태 (이미 시작됨, 참여하지 않음 등)")
    @ApiResponse(responseCode = "404", description = "운동 또는 참여 기록을 찾을 수 없음")
    public ResponseEntity<BaseResponse<ExerciseCancelDTO.Response>> cancelParticipation(
            @PathVariable Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCancelDTO.Response response = exerciseCommandService.cancelParticipation(
                exerciseId, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }

    @DeleteMapping("/{participantId}")
    @Operation(summary = "특정 참여자 운동 취소",
            description = "모임장이나 부모임장이 특정 참여자의 운동 참여를 취소합니다.")
    @ApiResponse(responseCode = "200", description = "운동 참여 취소 성공")
    @ApiResponse(responseCode = "400", description = "취소할 수 없는 상태 (이미 시작됨, 참여하지 않음 등)")
    @ApiResponse(responseCode = "403", description = "권한 없음 (매니저가 아님)")
    @ApiResponse(responseCode = "404", description = "운동 또는 참여 기록을 찾을 수 없음")
    public ResponseEntity<BaseResponse<ExerciseCancelDTO.Response>> cancelParticipationByManager(
            @PathVariable Long exerciseId,
            @PathVariable Long participantId,
            @Valid @RequestBody ExerciseCancelDTO.ByManagerRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCancelDTO.Response response = exerciseCommandService.cancelParticipationByManager(
                exerciseId, participantId, memberId, request);

        return BaseResponse.of(CommonSuccessCode.OK, response);
    }
}
