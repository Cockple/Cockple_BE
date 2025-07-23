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
import umc.cockple.demo.domain.exercise.service.ExerciseQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Exercise", description = "운동 관리 API")
public class ExerciseController {

    private final ExerciseCommandService exerciseCommandService;
    private final ExerciseQueryService exerciseQueryService;

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
    @ApiResponse(responseCode = "403", description = "권한 없음, 급수 위반")
    public BaseResponse<ExerciseJoinDTO.Response> JoinExercise(
            @PathVariable Long exerciseId,
            Authentication authentication
    ) {

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
    public BaseResponse<ExerciseCancelDTO.Response> cancelParticipation(
            @PathVariable Long exerciseId,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseCancelDTO.Response response = exerciseCommandService.cancelParticipation(
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
    public BaseResponse<ExerciseCancelDTO.Response> cancelGuestInvitation(
            @PathVariable Long exerciseId,
            @PathVariable Long guestId,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseCancelDTO.Response response = exerciseCommandService.cancelGuestInvitation(
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
    public BaseResponse<ExerciseCancelDTO.Response> cancelParticipationByManager(
            @PathVariable Long exerciseId,
            @PathVariable Long participantId,
            @Valid @RequestBody ExerciseCancelDTO.ByManagerRequest request,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseCancelDTO.Response response = exerciseCommandService.cancelParticipationByManager(
                exerciseId, participantId, memberId, request);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @DeleteMapping("/exercises/{exerciseId}")
    @Operation(summary = "운동 삭제",
            description = "모임장이 운동을 삭제합니다. 삭제된 운동의 모든 참여자와 게스트도 함께 삭제됩니다.")
    @ApiResponse(responseCode = "200", description = "운동 삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음 (모임장이 아님)")
    @ApiResponse(responseCode = "404", description = "운동을 찾을 수 없음")
    public BaseResponse<ExerciseDeleteDTO.Response> deleteExercise(
            @PathVariable Long exerciseId,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseDeleteDTO.Response response = exerciseCommandService.deleteExercise(
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
    public BaseResponse<ExerciseUpdateDTO.Response> updateExercise(
            @PathVariable Long exerciseId,
            @Valid @RequestBody ExerciseUpdateDTO.Request request,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseUpdateDTO.Response response = exerciseCommandService.updateExercise(
                exerciseId, memberId, request);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/{exerciseId}")
    @Operation(summary = "운동 상세 조회",
            description = "운동의 상세 정보를 조회합니다. 권한, 멤버 여부, 게스트 여부에 따라 반환되는 값이 달라집니다.")
    @ApiResponse(responseCode = "200", description = "운동 상세 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 운동")
    public BaseResponse<ExerciseDetailDTO.Response> getExerciseDetail(
            @PathVariable Long exerciseId,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseDetailDTO.Response response = exerciseQueryService.getExerciseDetail(
                exerciseId, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/{exerciseId}/guests")
    @Operation(summary = "내가 초대한 운동 게스트 조회",
            description = "내가 초대한 운동 게스트 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "내가 초대한 운동 게스트 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 운동")
    public BaseResponse<ExerciseMyGuestListDTO.Response> getMyInvitedGuests(
            @PathVariable Long exerciseId,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ExerciseMyGuestListDTO.Response response = exerciseQueryService.getMyInvitedGuests(
                exerciseId, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }


    @GetMapping("/parties/{partyId}/exercises/calender")
    @Operation(summary = "모임 운동 캘린더 조회",
            description = "모임 운동 캘린더를 조회합니다. 시작 날짜 ~ 종료 날짜까지의 데이터를 불러옵니다. 파라미터가 없으면 과거 1주 ~ 미래 3주까지의 데이터를 불러옵니다")
    @ApiResponse(responseCode = "200", description = "모임 운동 캘린더 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    public BaseResponse<PartyExerciseCalendarDTO.Response> getPartyExerciseCalender(
            @PathVariable Long partyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        PartyExerciseCalendarDTO.Response response = exerciseQueryService.getPartyExerciseCalendar(
                partyId, memberId, startDate, endDate);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/my/calender")
    @Operation(summary = "내 운동 캘린더 조회",
            description = "내 운동 캘린더를 조회합니다. 시작 날짜 ~ 종료 날짜까지의 데이터를 불러옵니다. 파라미터가 없으면 과거 1주 ~ 미래 3주까지의 데이터를 불러옵니다")
    @ApiResponse(responseCode = "200", description = "내 운동 캘린더 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    public BaseResponse<MyExerciseCalendarDTO.Response> getMyExerciseCalender(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        MyExerciseCalendarDTO.Response response = exerciseQueryService.getMyExerciseCalendar(
                memberId, startDate, endDate);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/parties/my")
    @Operation(summary = "내 모임 운동 조회",
            description = "내 모임의 운동 목록을 조회합니다. 시작하지 않은 운동만 표시되며, 최대 6개의 운동만 반환합니다.")
    @ApiResponse(responseCode = "200", description = "내 모임 운동 조회 성공")
    public BaseResponse<MyPartyExerciseDTO.Response> getMyPartyExercise(
            Authentication authentication
    ){

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        MyPartyExerciseDTO.Response response = exerciseQueryService.getMyPartyExercise(memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }
}
