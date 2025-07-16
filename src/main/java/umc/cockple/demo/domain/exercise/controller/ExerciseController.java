package umc.cockple.demo.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.exercise.service.ExerciseCommandService;
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
    public BaseResponse<ExerciseCreateDTO.Response> createExercise(
            @PathVariable Long partyId,
            @Valid @RequestBody ExerciseCreateDTO.Request request,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        // 서비스 호출
        ExerciseCreateDTO.Response response = exerciseCommandService.createExercise(
                partyId, memberId, request);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @PostMapping("/exercises/{exerciseId}/participants")
    @Operation(summary = "운동 신청",
            description = "모임에서 생성한 운동에 신청합니다. 외부 게스트 허용일 경우 모임 멤버가 아니어도 가능합니다.")
    @ApiResponse(responseCode = "200", description = "운동 신청 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    public BaseResponse<ExerciseJoinDTO.Response> JoinExercise(
            @PathVariable Long exerciseId,
            Authentication authentication
    ){

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseJoinDTO.Response response = exerciseCommandService.joinExercise(
                exerciseId, memberId);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @PostMapping("/exercises/{exerciseId}/guests")
    @Operation(summary = "게스트 초대",
            description = "파티 멤버가 게스트를 운동에 초대합니다. 운동의 게스트 허용 정책을 확인합니다.")
    @ApiResponse(responseCode = "201", description = "게스트 초대 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    @ApiResponse(responseCode = "404", description = "운동을 찾을 수 없음")
    public BaseResponse<ExerciseGuestInviteDTO.Response> inviteGuest(
            @PathVariable Long exerciseId,
            @Valid @RequestBody ExerciseGuestInviteDTO.Request request,
            Authentication authentication
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long inviterId = 1L; // 임시값

        ExerciseGuestInviteDTO.Response response = exerciseCommandService.inviteGuest(
                exerciseId, inviterId, request);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @DeleteMapping("/exercises/{exerciseId}/participants/my")
    @Operation(summary = "운동 참여 취소",
            description = "사용자가 본인의 운동 참여를 취소합니다.")
    @ApiResponse(responseCode = "200", description = "운동 참여 취소 성공")
    @ApiResponse(responseCode = "400", description = "취소할 수 없는 상태 (이미 시작됨, 참여하지 않음 등)")
    @ApiResponse(responseCode = "404", description = "운동 또는 참여 기록을 찾을 수 없음")
    public BaseResponse<ExerciseCancelResponseDTO> cancelParticipation(
            @PathVariable Long exerciseId,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseCancelResponseDTO response = exerciseCommandService.cancelParticipation(
                exerciseId, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @DeleteMapping("/exercises/{exerciseId}/guests/{guestId}")
    @Operation(summary = "게스트 초대 취소",
            description = "사용자가 본인이 초대한 게스트를 취소합니다.")
    @ApiResponse(responseCode = "200", description = "게스트 초대 취소 성공")
    @ApiResponse(responseCode = "400", description = "취소할 수 없는 상태 (이미 시작됨)")
    @ApiResponse(responseCode = "403", description = "본인이 초대한 게스트가 아닌 경우 취소할 수 없음")
    @ApiResponse(responseCode = "404", description = "운동 또는 참여 기록을 찾을 수 없음")
    public BaseResponse<ExerciseCancelResponseDTO> cancelGuestInvitation(
            @PathVariable Long exerciseId,
            @PathVariable Long guestId,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseCancelResponseDTO response = exerciseCommandService.cancelGuestInvitation(
                exerciseId, guestId, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @DeleteMapping("/exercises/{exerciseId}/participants/{participantId}")
    @Operation(summary = "특정 참여자 운동 취소",
            description = "모임장이나 부모임장이 특정 참여자의 운동 참여를 취소합니다.")
    @ApiResponse(responseCode = "200", description = "운동 참여 취소 성공")
    @ApiResponse(responseCode = "400", description = "취소할 수 없는 상태 (이미 시작됨, 참여하지 않음 등)")
    @ApiResponse(responseCode = "403", description = "권한 없음 (매니저가 아님)")
    @ApiResponse(responseCode = "404", description = "운동 또는 참여 기록을 찾을 수 없음")
    public BaseResponse<ExerciseCancelResponseDTO> cancelParticipationByManager(
            @PathVariable Long exerciseId,
            @PathVariable Long participantId,
            @Valid @RequestBody ExerciseManagerCancelRequestDTO request,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseCancelResponseDTO response = exerciseCommandService.cancelParticipationByManager(
                exerciseId, participantId, memberId, request);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @DeleteMapping("/exercises/{exerciseId}")
    @Operation(summary = "운동 삭제",
            description = "모임장이 운동을 삭제합니다. 삭제된 운동의 모든 참여자와 게스트도 함께 삭제됩니다.")
    @ApiResponse(responseCode = "200", description = "운동 삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (모임장이 아님)")
    @ApiResponse(responseCode = "404", description = "운동을 찾을 수 없음")
    public BaseResponse<ExerciseDeleteResponseDTO> deleteExercise(
            @PathVariable Long exerciseId,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseDeleteResponseDTO response = exerciseCommandService.deleteExercise(
                exerciseId, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @PatchMapping("/exercises/{exerciseId}")
    @Operation(summary = "운동 수정",
            description = "모임장이 생성한 운동의 정보를 수정합니다. 이미 시작된 운동은 수정할 수 없습니다.")
    @ApiResponse(responseCode = "200", description = "운동 수정 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    @ApiResponse(responseCode = "403", description = "권한 없음 (모임장이 아님)")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 운동")
    public BaseResponse<ExerciseUpdateResponseDTO> updateExercise(
            @PathVariable Long exerciseId,
            @Valid @RequestBody ExerciseUpdateRequestDTO request,
            Authentication authentication
    ){

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseUpdateResponseDTO response = exerciseCommandService.updateExercise(
                exerciseId, memberId, request);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }
}
