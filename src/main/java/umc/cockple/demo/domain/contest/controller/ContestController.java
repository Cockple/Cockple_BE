package umc.cockple.demo.domain.contest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.contest.dto.*;
import umc.cockple.demo.domain.contest.service.ContestCommandService;
import umc.cockple.demo.domain.contest.service.ContestQueryService;
import umc.cockple.demo.domain.image.dto.ImageUploadResponseDTO;
import umc.cockple.demo.domain.image.service.ImageService;
import umc.cockple.demo.global.enums.ImgType;
import umc.cockple.demo.global.enums.MedalType;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
@Tag(name = "Contest", description = "대회 기록 관리 API")
public class ContestController {

    private final ContestCommandService contestCommandService;
    private final ContestQueryService contestQueryService;
    private final ImageService imageService;

    @PostMapping(value = "/contests/my", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "대회 기록 등록", description = "회원이 자신의 대회 기록을 등록합니다.")
    @ApiResponse(responseCode = "201", description = "대회 기록 등록 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    public BaseResponse<ContestRecordCreateDTO.Response> createContestRecord(
            //@AuthenticationPrincipal Long memberId,
            @RequestPart("request") @Valid ContestRecordCreateDTO.Request request,
            @RequestPart(value = "contestImg", required = false) List<MultipartFile> contestImgs
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        // 서비스 호출
        ContestRecordCreateDTO.Response response = contestCommandService.createContestRecord(memberId, contestImgs, request);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @PatchMapping(value = "/contests/my/{contestId}", consumes = {"multipart/form-data"})
    @Operation(summary = "대회 기록 수정", description = "회원이 자신의 대회 기록을 수정합니다.")
    @ApiResponse(responseCode = "201", description = "대회 기록 수정 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    public BaseResponse<ContestRecordUpdateDTO.Response> updateContestRecord(
            //@AuthenticationPrincipal Long memberId
            @PathVariable Long contestId,
            @RequestPart("updateRequest") @Valid ContestRecordUpdateDTO.Request request,
            @RequestPart(value = "contestImg", required = false) List<MultipartFile> contestImgsToAdd
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        //서비스 호출
        ContestRecordUpdateDTO.Response response = contestCommandService.updateContestRecord(memberId, contestId, contestImgsToAdd, request);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @DeleteMapping(value = "/contests/my/{contestId}")
    @Operation(summary = "대회 기록 삭제", description = "회원이 자신의 대회 기록을 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "대회 기록 삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    public BaseResponse<ContestRecordDeleteDTO.Response> deleteContestRecord(
            //@AuthenticationPrincipal Long memberId
            @PathVariable Long contestId
    ) {

        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ContestRecordDeleteDTO.Response response =
                contestCommandService.deleteContestRecord(memberId, contestId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/contests/my/{contestId}")
    @Operation(summary = "내 대회 기록 상세 조회", description = "회원이 자신의 대회 기록 하나를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ContestRecordDetailDTO.Response> getMyContestRecordDetail(
            //@AuthenticationPrincipal Long memberId,
            @PathVariable Long contestId
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        ContestRecordDetailDTO.Response response = contestQueryService.getContestRecordDetail(memberId, memberId, contestId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/contests/my")
    @Operation(summary = "내 대회 기록 리스트 조회", description = "회원이 자신의 전체 또는 미입상(NONE) 대회 기록 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<List<ContestRecordSimpleDTO.Response>> getMyContestRecord(
            //@AuthenticationPrincipal Long memberId
            @RequestParam(required = false) MedalType medalType
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L;

        List<ContestRecordSimpleDTO.Response> response = contestQueryService.getMyContestRecordsByMedalType(memberId, medalType);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/contests/my/medals")
    @Operation(summary = "내 대회 메달 조회", description = "회원이 자신의 메달 개수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ContestMedalSummaryDTO.Response> getMyMedals(
            //@AuthenticationPrincipal Long memberId
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L;

        ContestMedalSummaryDTO.Response response = contestQueryService.getMyMedalSummary(memberId);

        return BaseResponse.success(CommonSuccessCode.OK,response);
    }

    @GetMapping(value = "/members/{memberId}/contests/{contestId}")
    @Operation(summary = "다른 사람의 대회 기록 상세 조회", description = "다른 사람의 대회 기록 하나를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ContestRecordDetailDTO.Response> getOtherMemberContestRecordDetail(
            //@AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long memberId,
            @PathVariable Long contestId
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long loginMemberId = 2L; // 임시값

        ContestRecordDetailDTO.Response response = contestQueryService.getContestRecordDetail(loginMemberId, memberId, contestId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/members/{memberId}/contests")
    @Operation(summary = "다른 사람의 대회 기록 리스트 조회", description = "회원이 다른 사람의 전체 또는 미입상(NONE) 대회 기록 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<List<ContestRecordSimpleDTO.Response>> getOtherMemberContestRecord(
            @RequestParam Long memberId,
            @RequestParam(required = false) MedalType medalType
    ) {
        List<ContestRecordSimpleDTO.Response> response = contestQueryService.getMyContestRecordsByMedalType(memberId, medalType);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/members/{memberId}/medals")
    @Operation(summary = "다른 사람의 대회 메달 조회", description = "회원이 다른 사람의 메달 개수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ContestMedalSummaryDTO.Response> getOtherMemberMedals(
            @RequestParam Long memberId
    ) {
        ContestMedalSummaryDTO.Response response = contestQueryService.getMyMedalSummary(memberId);

        return BaseResponse.success(CommonSuccessCode.OK,response);
    }

    @PostMapping(value = "/contests/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "대회 이미지 업로드", description = "S3에 이미지를 업로드하고 이미지 URL과 imgKey를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "업로드 성공")
    public BaseResponse<List<ImageUploadResponseDTO>> uploadContestImages(
            //@AuthenticationPrincipal Long memberId
            @RequestPart("images") List<MultipartFile> images){

        Long memberId = 1L; // 임시값

        return BaseResponse.success(CommonSuccessCode.OK, imageService.uploadImages(images, ImgType.CONTEST));
    }

    private String extractKeyFromUrl(String url) {
        int startIndex = url.indexOf("contest-images/");
        return url.substring(startIndex);
    }
}
