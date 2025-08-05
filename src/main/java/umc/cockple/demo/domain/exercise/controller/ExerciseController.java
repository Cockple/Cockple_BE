package umc.cockple.demo.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.service.ExerciseCommandService;
import umc.cockple.demo.domain.exercise.service.ExerciseQueryService;
import umc.cockple.demo.domain.party.enums.ActivityTime;
import umc.cockple.demo.domain.party.enums.ParticipationType;
import umc.cockple.demo.global.enums.Level;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.time.LocalDate;
import java.util.List;

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
            @Valid @RequestBody ExerciseCreateDTO.Request request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

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
            @PathVariable Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

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
            @Valid @RequestBody ExerciseGuestInviteDTO.Request request
    ) {
        Long inviterId = SecurityUtil.getCurrentMemberId();

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
            @PathVariable Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

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
            @PathVariable Long guestId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

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
            @Valid @RequestBody ExerciseCancelDTO.ByManagerRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

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
            @PathVariable Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

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
            @Valid @RequestBody ExerciseUpdateDTO.Request request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

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
            @PathVariable Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

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
            @PathVariable Long exerciseId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseMyGuestListDTO.Response response = exerciseQueryService.getMyInvitedGuests(
                exerciseId, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }


    @GetMapping("/parties/{partyId}/exercises/calender")
    @Operation(summary = "모임 운동 캘린더 조회",
            description = "모임 운동 캘린더를 조회합니다. 시작 날짜 ~ 종료 날짜까지의 데이터를 불러옵니다. 파라미터가 없으면 과거 1주 ~ 미래 3주까지의 데이터를 불러옵니다.")
    @ApiResponse(responseCode = "200", description = "모임 운동 캘린더 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    public BaseResponse<PartyExerciseCalendarDTO.Response> getPartyExerciseCalender(
            @PathVariable Long partyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        PartyExerciseCalendarDTO.Response response = exerciseQueryService.getPartyExerciseCalendar(
                partyId, memberId, startDate, endDate);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/my/calender")
    @Operation(summary = "내 운동 캘린더 조회",
            description = "내 운동 캘린더를 조회합니다. 시작 날짜 ~ 종료 날짜까지의 데이터를 불러옵니다. 파라미터가 없으면 과거 1주 ~ 미래 3주까지의 데이터를 불러옵니다.")
    @ApiResponse(responseCode = "200", description = "내 운동 캘린더 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    public BaseResponse<MyExerciseCalendarDTO.Response> getMyExerciseCalender(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyExerciseCalendarDTO.Response response = exerciseQueryService.getMyExerciseCalendar(
                memberId, startDate, endDate);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/parties/my")
    @Operation(summary = "내 모임 운동 조회",
            description = "내 모임의 운동 목록을 조회합니다. 시작하지 않은 운동만 표시되며, 최대 6개의 운동만 반환합니다.")
    @ApiResponse(responseCode = "200", description = "내 모임 운동 조회 성공")
    public BaseResponse<MyPartyExerciseDTO.Response> getMyPartyExercise() {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyPartyExerciseDTO.Response response = exerciseQueryService.getMyPartyExercise(memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/parties/my/calendar")
    @Operation(summary = "내 모임 운동 캘린더 조회",
            description = """
                    내 모임의 운동 캘린더를 조회합니다.
                    시작 날짜 ~ 종료 날짜까지의 데이터를 불러옵니다. 파라미터가 없으면 과거 1주 ~ 미래 3주까지의 데이터를 불러옵니다.
                    정렬 방식은 최신순(LATEST)과 참여인원이 많은 순(POPULARITY) 2가지로 구분됩니다. 파라미터를 없으면 최신순으로 불러옵니다.
                    """)
    @ApiResponse(responseCode = "200", description = "내 운동 캘린더 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    public BaseResponse<MyPartyExerciseCalendarDTO.Response> getMyPartyExerciseCalendar(
            @RequestParam(defaultValue = "LATEST") MyPartyExerciseOrderType orderType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyPartyExerciseCalendarDTO.Response response = exerciseQueryService.getMyPartyExerciseCalendar(
                memberId, orderType, startDate, endDate);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/recommendations")
    @Operation(summary = "사용자 추천 운동 조회",
            description = """
                    사용자가 속하지 않은 모임의 운동을 추천합니다.
                    조회되는 운동의 최대 개수는 10개입니다.
                    시작하지 않은 운동만 조회됩니다.
                    참여하지 않은 운동만 조회됩니다.
                    운동의 급수와 나이 조건이 사용자와 맞는 운동만 조회됩니다.
                    정렬 기준은 위치, 날짜, 시간 순입니다.
                    """)
    @ApiResponse(responseCode = "200", description = "내 운동 캘린더 성공")
    public BaseResponse<ExerciseRecommendationDTO.Response> getRecommendedExercises() {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseRecommendationDTO.Response response = exerciseQueryService.getRecommendedExercises(memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/my")
    @Operation(summary = "내 참여 운동 조회",
            description = """
                    내가 참여한 운동 목록을 조회합니다.
                    필터: 전체(ALL), 참여 예정(UPCOMING), 참여 완료(COMPLETED)
                    정렬: 최신순(LATEST), 오래된순(OLDEST)
                    페이징을 지원합니다.
                    """)
    @ApiResponse(responseCode = "200", description = "내 참여 운동 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 필터 타입 또는 정렬 타입")
    public BaseResponse<MyExerciseListDTO.Response> getMyExercises(
            @RequestParam(defaultValue = "ALL") MyExerciseFilterType filterType,
            @RequestParam(defaultValue = "LATEST") MyExerciseOrderType orderType,
            @PageableDefault(size = 15) Pageable pageable
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        MyExerciseListDTO.Response response = exerciseQueryService.getMyExercises(
                memberId, filterType, orderType, pageable);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/buildings/exercises/{date}")
    @Operation(summary = "건물 운동 상세 조회",
            description = "특정 날짜 및 건물의 운동 상세 정보를 조회합니다.")
    public BaseResponse<ExerciseBuildingDetailDTO.Response> getBuildingExerciseDetails(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam String buildingName,
            @RequestParam String streetAddr
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseBuildingDetailDTO.Response response = exerciseQueryService
                .getBuildingExerciseDetails(buildingName, streetAddr, date, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/buildings/map/monthly")
    @Operation(summary = "월간 운동 건물 지도 데이터 조회",
            description = "특정 날짜가 속한 월에 운동이 개최되는 반경 내 건물들의 위치 정보를 지도 표시용으로 반환")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ExerciseMapBuildingsDTO.Response> getMonthlyExerciseBuildings(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "3.0") Double radiusKm
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseMapBuildingsDTO.Response response = exerciseQueryService
                .getExerciseMapCalendarSummary(date, latitude, longitude, radiusKm, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/exercises/recommendations/calendar")
    @Operation(summary = "사용자 추천 운동 캘린더 조회",
            description = """
                    사용자가 속하지 않은 모임의 운동 중 참여하지 않은 운동을 캘린더 형식으로 조회합니다.
                    - isCockpleRecommend=true: 콕플 추천 (급수 일치, 위치+시간순 정렬)
                    - isCockpleRecommend=false: 필터 + 정렬 방식
                    기본 기간: 과거 1주 ~ 미래 3주
                    """)
    @ApiResponse(responseCode = "200", description = "사용자 추천 운동 캘린더 조회 성공")
    public BaseResponse<ExerciseRecommendationCalendarDTO.Response> getRecommendedExerciseCalendar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "true") Boolean isCockpleRecommend,
            @RequestParam(required = false) String addr1,
            @RequestParam(required = false) String addr2,
            @RequestParam(required = false) List<Level> levels,
            @RequestParam(required = false) List<ParticipationType> participationTypes,
            @RequestParam(required = false) List<ActivityTime> activityTimes,
            @RequestParam(defaultValue = "LATEST") MyPartyExerciseOrderType sortType
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseRecommendationCalendarDTO.FilterSortType filterSortType =
                ExerciseRecommendationCalendarDTO.FilterSortType.builder()
                        .addr1(addr1)
                        .addr2(addr2)
                        .levels(levels)
                        .participationTypes(participationTypes)
                        .activityTimes(activityTimes)
                        .sortType(sortType)
                        .build();

        ExerciseRecommendationCalendarDTO.Response response = exerciseQueryService
                .getRecommendedExerciseCalendar(memberId, startDate, endDate, isCockpleRecommend, filterSortType);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }
}
