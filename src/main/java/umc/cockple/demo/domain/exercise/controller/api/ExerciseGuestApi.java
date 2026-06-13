package umc.cockple.demo.domain.exercise.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import umc.cockple.demo.domain.exercise.dto.ExerciseCancelDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseGuestInviteDTO;
import umc.cockple.demo.domain.exercise.dto.ExerciseMyGuestListDTO;
import umc.cockple.demo.global.response.BaseResponse;

@RequestMapping("/api/exercises/{exerciseId}/guests")
@Tag(name = "Exercise", description = "운동 관리 API")
public interface ExerciseGuestApi {

    @PostMapping
    @Operation(summary = "게스트 초대",
            description = "파티 멤버가 게스트를 운동에 초대합니다. 운동의 게스트 허용 정책을 확인합니다.")
    @ApiResponse(responseCode = "201", description = "게스트 초대 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    @ApiResponse(responseCode = "404", description = "운동을 찾을 수 없음")
    ResponseEntity<BaseResponse<ExerciseGuestInviteDTO.Response>> inviteGuest(
            @PathVariable Long exerciseId,
            @Valid @RequestBody ExerciseGuestInviteDTO.Request request
    );

    @DeleteMapping("/{guestId}")
    @Operation(summary = "게스트 초대 취소",
            description = "사용자가 본인이 초대한 게스트를 취소합니다.")
    @ApiResponse(responseCode = "200", description = "게스트 초대 취소 성공")
    @ApiResponse(responseCode = "400", description = "취소할 수 없는 상태 (이미 시작됨)")
    @ApiResponse(responseCode = "403", description = "본인이 초대한 게스트가 아닌 경우 취소할 수 없음")
    @ApiResponse(responseCode = "404", description = "운동 또는 참여 기록을 찾을 수 없음")
    ResponseEntity<BaseResponse<ExerciseCancelDTO.Response>> cancelGuestInvitation(
            @PathVariable Long exerciseId,
            @PathVariable Long guestId
    );

    @GetMapping
    @Operation(summary = "내가 초대한 운동 게스트 조회",
            description = "내가 초대한 운동 게스트 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "내가 초대한 운동 게스트 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 운동")
    ResponseEntity<BaseResponse<ExerciseMyGuestListDTO.Response>> getMyInvitedGuests(
            @PathVariable Long exerciseId
    );
}
