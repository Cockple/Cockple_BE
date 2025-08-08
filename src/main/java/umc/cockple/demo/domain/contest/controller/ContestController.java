package umc.cockple.demo.domain.contest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.contest.dto.*;
import umc.cockple.demo.domain.contest.service.ContestCommandService;
import umc.cockple.demo.domain.contest.service.ContestQueryService;
import umc.cockple.demo.domain.contest.enums.MedalType;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
@Tag(name = "Contest", description = "대회 기록 관리 API")
public class ContestController {

    private final ContestCommandService contestCommandService;
    private final ContestQueryService contestQueryService;

    @PostMapping(value = "/contests/my")
    @Operation(summary = "대회 기록 등록", description = "회원이 자신의 대회 기록을 등록합니다.")
    @ApiResponse(responseCode = "201", description = "대회 기록 등록 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    public BaseResponse<ContestRecordCreateDTO.Response> createContestRecord(
            @RequestBody @Valid ContestRecordCreateDTO.Request request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ContestRecordCreateDTO.Response response = contestCommandService.createContestRecord(memberId, request);
        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @PatchMapping(value = "/contests/my/{contestId}")
    @Operation(summary = "대회 기록 수정", description = "회원이 자신의 대회 기록을 수정합니다.")
    @ApiResponse(responseCode = "201", description = "대회 기록 수정 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    public BaseResponse<ContestRecordUpdateDTO.Response> updateContestRecord(
            @PathVariable Long contestId,
            @RequestBody @Valid ContestRecordUpdateDTO.Request request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ContestRecordUpdateDTO.Response response = contestCommandService.updateContestRecord(memberId, contestId, request);
        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @DeleteMapping(value = "/contests/my/{contestId}")
    @Operation(summary = "대회 기록 삭제", description = "회원이 자신의 대회 기록을 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "대회 기록 삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    public BaseResponse<ContestRecordDeleteDTO.Response> deleteContestRecord(
            @PathVariable Long contestId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ContestRecordDeleteDTO.Response response = contestCommandService.deleteContestRecord(memberId, contestId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/contests/my/{contestId}")
    @Operation(summary = "내 대회 기록 상세 조회", description = "회원이 자신의 대회 기록 하나를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ContestRecordDetailDTO.Response> getMyContestRecordDetail(
            @PathVariable Long contestId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ContestRecordDetailDTO.Response response = contestQueryService.getContestRecordDetail(memberId, memberId, contestId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/contests/my")
    @Operation(summary = "내 대회 기록 리스트 조회", description = "회원이 자신의 전체 또는 미입상(NONE) 대회 기록 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<List<ContestRecordSimpleDTO.Response>> getMyContestRecord(
            @Parameter(
                    name = "medalType",
                    description = "전체 or NONE",
                    schema = @Schema(allowableValues = {"NONE"})
            )
            @RequestParam(required = false) MedalType medalType
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<ContestRecordSimpleDTO.Response> response = contestQueryService.getMyContestRecordsByMedalType(memberId, medalType);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/contests/my/medals")
    @Operation(summary = "내 대회 메달 조회", description = "회원이 자신의 메달 개수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ContestMedalSummaryDTO.Response> getMyMedals(
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ContestMedalSummaryDTO.Response response = contestQueryService.getMyMedalSummary(memberId);
        return BaseResponse.success(CommonSuccessCode.OK,response);
    }

    @GetMapping(value = "/members/{memberId}/contests/{contestId}")
    @Operation(summary = "다른 사람의 대회 기록 상세 조회", description = "다른 사람의 대회 기록 하나를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ContestRecordDetailDTO.Response> getOtherMemberContestRecordDetail(
            @PathVariable Long targetMemberId,
            @PathVariable Long contestId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ContestRecordDetailDTO.Response response = contestQueryService.getContestRecordDetail(memberId, targetMemberId, contestId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/members/{memberId}/contests")
    @Operation(summary = "다른 사람의 대회 기록 리스트 조회", description = "회원이 다른 사람의 전체 또는 미입상(NONE) 대회 기록 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<List<ContestRecordSimpleDTO.Response>> getOtherMemberContestRecord(
            @RequestParam Long targetMemberId,
            @Parameter(
                    name = "medalType",
                    description = "전체 or NONE",
                    schema = @Schema(allowableValues = {"NONE"})
            )
            @RequestParam(required = false) MedalType medalType
    ) {
        List<ContestRecordSimpleDTO.Response> response = contestQueryService.getMyContestRecordsByMedalType(targetMemberId, medalType);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/members/{memberId}/medals")
    @Operation(summary = "다른 사람의 대회 메달 조회", description = "회원이 다른 사람의 메달 개수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ContestMedalSummaryDTO.Response> getOtherMemberMedals(
            @RequestParam Long targetMemberId
    ) {
        ContestMedalSummaryDTO.Response response = contestQueryService.getMyMedalSummary(targetMemberId);
        return BaseResponse.success(CommonSuccessCode.OK,response);
    }
}
