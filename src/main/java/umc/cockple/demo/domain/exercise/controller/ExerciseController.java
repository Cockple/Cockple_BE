package umc.cockple.demo.domain.exercise.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.exercise.dto.*;
import umc.cockple.demo.domain.exercise.enums.MyExerciseFilterType;
import umc.cockple.demo.domain.exercise.enums.MyExerciseOrderType;
import umc.cockple.demo.domain.exercise.enums.MyPartyExerciseOrderType;
import umc.cockple.demo.domain.exercise.service.*;
import umc.cockple.demo.domain.exercise.service.command.ExerciseCommandService;
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

    @PostMapping("/exercises/{exerciseId}/guests")
    @Operation(summary = "게스트 초대",
            description = "파티 멤버가 게스트를 운동에 초대합니다. 운동의 게스트 허용 정책을 확인합니다.")
    @ApiResponse(responseCode = "201", description = "게스트 초대 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 비즈니스 룰 위반")
    @ApiResponse(responseCode = "404", description = "운동을 찾을 수 없음")
    public ResponseEntity<BaseResponse<ExerciseGuestInviteDTO.Response>> inviteGuest(
            @PathVariable Long exerciseId,
            @Valid @RequestBody ExerciseGuestInviteDTO.Request request
    ) {
        Long inviterId = SecurityUtil.getCurrentMemberId();

        ExerciseGuestInviteDTO.Response response = exerciseCommandService.inviteGuest(
                exerciseId, inviterId, request);

        return BaseResponse.of(CommonSuccessCode.CREATED, response);
    }

    @DeleteMapping("/exercises/{exerciseId}/guests/{guestId}")
    @Operation(summary = "게스트 초대 취소",
            description = "사용자가 본인이 초대한 게스트를 취소합니다.")
    @ApiResponse(responseCode = "200", description = "게스트 초대 취소 성공")
    @ApiResponse(responseCode = "400", description = "취소할 수 없는 상태 (이미 시작됨)")
    @ApiResponse(responseCode = "403", description = "본인이 초대한 게스트가 아닌 경우 취소할 수 없음")
    @ApiResponse(responseCode = "404", description = "운동 또는 참여 기록을 찾을 수 없음")
    public ResponseEntity<BaseResponse<ExerciseCancelDTO.Response>> cancelGuestInvitation(
            @PathVariable Long exerciseId,
            @PathVariable Long guestId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        ExerciseCancelDTO.Response response = exerciseCommandService.cancelGuestInvitation(
                exerciseId, guestId, memberId);

        return BaseResponse.of(CommonSuccessCode.OK, response);
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
        ExerciseMapBuildingsDTO.Query query = ExerciseMapBuildingsDTO.Query.of(date, latitude, longitude, radiusKm);

        ExerciseMapBuildingsDTO.Response response = exerciseQueryService
                .getExerciseMapCalendarSummary(query, memberId);

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
