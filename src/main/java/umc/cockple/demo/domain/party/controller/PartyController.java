package umc.cockple.demo.domain.party.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.party.dto.PartyCreateDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinActionDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinCreateDTO;
import umc.cockple.demo.domain.party.dto.PartyJoinDTO;
import umc.cockple.demo.domain.party.service.PartyCommandService;
import umc.cockple.demo.domain.party.service.PartyQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Party", description = "모임 API")
public class PartyController {

    private final PartyCommandService partyCommandService;
    private final PartyQueryService partyQueryService;

    @PostMapping(value = "/parties")
    @Operation(summary = "모임 생성",
            description = "새로운 모임을 생성합니다. 성공 시 사용자는 해당 모임의 모임장이 됩니다.")
    @ApiResponse(responseCode = "201", description = "모임 생성 성공")
    @ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패 또는 잘못된 요청 형식")
    @ApiResponse(responseCode = "403", description = "모임 생성 권한 없음")
    public BaseResponse<PartyCreateDTO.Response> createParty(
            @RequestBody @Valid PartyCreateDTO.Request request,
            Authentication authentication
    ){
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        //서비스 호출
        PartyCreateDTO.Response response = partyCommandService.createParty(memberId, request);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @PostMapping("/parties/{partyId}/join-requests")
    @Operation(summary = "모임 가입 신청",
            description = "사용자가 특정 모임에 가입을 신청합니다")
    @ApiResponse(responseCode = "201", description = "가입 신청 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임 또는 사용자")
    @ApiResponse(responseCode = "409", description = "이미 가입했거나 신청 대기 중인 상태")
    public BaseResponse<PartyJoinCreateDTO.Response> createJoinRequest(
            @PathVariable Long partyId,
            Authentication authentication
    ){
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 8L; // 임시값

        PartyJoinCreateDTO.Response response = partyCommandService.createJoinRequest(partyId, memberId);
        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @GetMapping("/parties/{partyId}/join-requests")
    @Operation(summary = "모임 가입 신청 조회",
            description = "모임에 가입을 신청한 사용자들의 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "가입 신청 조회 성공")
    @ApiResponse(responseCode = "403", description = "모임장 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    public BaseResponse<Slice<PartyJoinDTO.Response>> getJoinRequests(
            @PathVariable Long partyId,
            @RequestParam(name = "status") String status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ){
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        Slice<PartyJoinDTO.Response> response = partyQueryService.getJoinRequests(partyId, memberId, status, pageable);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @PatchMapping("parties/{partyId}/join-requests/{requestId}")
    @Operation(summary = "모임 가입 신청 처리",
        description = "모임장이 가입 신청을 승인하거나 거절합니다.")
    @ApiResponse(responseCode = "200", description = "가입 신청 처리 성공")
    @ApiResponse(responseCode = "403", description = "모임장 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임 또는 가입 신청")
    @ApiResponse(responseCode = "409", description = "이미 처리된 가입 신청")
    public BaseResponse<Void> actionJoinRequests(
            @PathVariable Long partyId,
            @PathVariable Long requestId,
            @RequestBody @Valid PartyJoinActionDTO.Request request,
            Authentication authentication
    ){
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        partyCommandService.actionJoinRequest(partyId, memberId, request, requestId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

}
