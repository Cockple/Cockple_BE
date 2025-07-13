package umc.cockple.demo.domain.contest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.contest.dto.*;
import umc.cockple.demo.domain.contest.service.ContestCommandService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
@Tag(name = "Contest", description = "대회 기록 관리 API")
public class ContestController {

    private final ContestCommandService contestCommandService;

    @PostMapping(value = "/contests/my", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "대회 기록 등록", description = "회원이 자신의 대회 기록을 등록합니다.")
    @ApiResponse(responseCode = "201", description = "대회 기록 등록 성공")
    @ApiResponse(responseCode = "400", description = "입력값 오류")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    public BaseResponse<ContestRecordCreateResponseDTO> createContestRecord(
            //@AuthenticationPrincipal Long memberId,
            @RequestPart("request") @Valid ContestRecordCreateRequestDTO request,
            @RequestPart(value = "contestImg", required = false) List<MultipartFile> contestImgs
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        // 서비스 호출
        ContestRecordCreateResponseDTO response = contestCommandService.createContestRecord(memberId, contestImgs, request);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @PatchMapping(value = "/contests/my/{contestId}", consumes = {"multipart/form-data"})
    public BaseResponse<ContestRecordUpdateResponseDTO> updateContestRecord(
            //@AuthenticationPrincipal Long memberId
            @PathVariable Long contestId,
            @RequestPart("updateRequest") @Valid ContestRecordUpdateRequestDTO request,
            @RequestPart(value = "contestImg", required = false) List<MultipartFile> contestImgsToAdd
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        //서비스 호출
        ContestRecordUpdateResponseDTO response = contestCommandService.updateContestRecord(memberId, contestId, contestImgsToAdd, request);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

}
