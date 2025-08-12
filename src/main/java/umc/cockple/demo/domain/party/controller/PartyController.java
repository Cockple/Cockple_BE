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
import umc.cockple.demo.domain.party.dto.*;
import umc.cockple.demo.domain.party.service.PartyCommandService;
import umc.cockple.demo.domain.party.service.PartyQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Party", description = "모임 API")
public class PartyController {

    private final PartyCommandService partyCommandService;
    private final PartyQueryService partyQueryService;

    @GetMapping("/my/parties/simple")
    @Operation(summary = "내 모임 간략화 조회",
            description = "사용자가 가입한 내 모임을 간략화하여 조회합니다. ")
    @ApiResponse(responseCode = "200", description = "모임 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    public BaseResponse<Slice<PartySimpleDTO.Response>> getSimpleMyParties(
            @PageableDefault(page = 0, size = 10, sort = {"createdAt", "party.partyName"}, direction = Sort.Direction.DESC) Pageable pageable
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        Slice<PartySimpleDTO.Response> response = partyQueryService.getSimpleMyParties(memberId, pageable);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/my/parties")
    @Operation(summary = "내 모임 조회",
            description = "사용자가 가입한 내 모임을 조회합니다. ")
    @ApiResponse(responseCode = "200", description = "모임 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    public BaseResponse<Slice<PartyDTO.Response>> getMyParties(
            @RequestParam(required = false, defaultValue = "false") Boolean created,
            @RequestParam(required = false, defaultValue = "최신순") String sort,
            @PageableDefault(size = 10) Pageable pageable
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        Slice<PartyDTO.Response> response = partyQueryService.getMyParties(memberId, created, sort, pageable);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/my/parties/suggestions")
    @Operation(summary = "추천 모임 조회",
            description = "사용자에게 추천되는 모임 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "모임 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    public BaseResponse<Slice<PartyDTO.Response>> getRecommendedParties(
            @RequestParam(defaultValue = "true") boolean isCockpleRecommend,
            @RequestParam(required = false) String addr1,
            @RequestParam(required = false) String addr2,
            @RequestParam(required = false) List<String> level,
            @RequestParam(required = false) List<String> partyType,
            @RequestParam(required = false) List<String> activityDay,
            @RequestParam(required = false) List<String> activityTime,
            @RequestParam(required = false) List<String> keyword,
            @RequestParam(required = false, defaultValue = "최신순") String sort,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        PartyFilterDTO.Request filter = PartyFilterDTO.Request.builder()
                .addr1(addr1)
                .addr2(addr2)
                .level(level)
                .partyType(partyType)
                .activityDay(activityDay)
                .activityTime(activityTime)
                .keyword(keyword)
                .build();

        Slice<PartyDTO.Response> response = partyQueryService.getRecommendedParties(memberId, isCockpleRecommend, filter, sort, pageable);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/parties/{partyId}")
    @Operation(summary = "모임 상세 정보 조회",
            description = "특정 모임의 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "모임 상세 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임 또는 사용자")
    public BaseResponse<PartyDetailDTO.Response> getPartyDetails(
            @PathVariable Long partyId
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        PartyDetailDTO.Response response = partyQueryService.getPartyDetails(partyId, memberId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @PostMapping(value = "/parties")
    @Operation(summary = "모임 생성",
            description = "새로운 모임을 생성합니다. 성공 시 사용자는 해당 모임의 모임장이 됩니다.")
    @ApiResponse(responseCode = "201", description = "모임 생성 성공")
    @ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패 또는 잘못된 요청 형식")
    @ApiResponse(responseCode = "403", description = "모임 생성 권한 없음")
    public BaseResponse<PartyCreateDTO.Response> createParty(
            @RequestBody @Valid PartyCreateDTO.Request request
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        //서비스 호출
        PartyCreateDTO.Response response = partyCommandService.createParty(memberId, request);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @PatchMapping(value = "/parties/{partyId}")
    @Operation(summary = "모임 정보 수정",
            description = "특정 모임의 정보를 부분적으로 수정합니다.")
    @ApiResponse(responseCode = "200", description = "모임 정보 수정 성공")
    @ApiResponse(responseCode = "403", description = "모임장 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    public BaseResponse<Void> updateParty(
            @PathVariable Long partyId,
            @RequestBody @Valid PartyUpdateDTO.Request request
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        partyCommandService.updateParty(partyId, memberId, request);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @PatchMapping("/parties/{partyId}/status")
    @Operation(summary ="모임 삭제(비활성화)",
            description = "모임장이 모임을 삭제(비활성화)합니다.")
    public BaseResponse<Void> deleteParty(
            @PathVariable Long partyId
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        partyCommandService.deleteParty(partyId, memberId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @GetMapping("/parties/{partyId}/members")
    @Operation(summary = "모임 멤버 조회",
                description = "특정 모임의 멤버 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "모임 멤버 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    public BaseResponse<PartyMemberDTO.Response> getPartyMembers(
            @PathVariable Long partyId
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        PartyMemberDTO.Response response = partyQueryService.getPartyMembers(partyId, memberId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @DeleteMapping("/parties/{partyId}/members/my")
    @Operation(summary = "모임 탈퇴",
            description = "현재 로그인한 사용자가 소속된 모임에서 탈퇴합니다.")
    @ApiResponse(responseCode = "200", description = "모임 탈퇴 성공")
    @ApiResponse(responseCode = "403", description = "모임장 탈퇴 불가")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임 또는 사용자")
    public BaseResponse<Void> leaveParty(
            @PathVariable Long partyId
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        partyCommandService.leaveParty(partyId, memberId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @DeleteMapping("/parties/{partyId}/members/{memberId}")
    @Operation(summary = "모임 멤버 삭제",
            description = "모임장 또는 부모임장이 특정 멤버를 모임에서 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "모임 멤버 삭제 성공")
    @ApiResponse(responseCode = "403", description = "모임 멤버 삭제 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임 또는 멤버")
    public BaseResponse<Void> removeMember(
            @PathVariable Long partyId,
            @PathVariable("memberId") Long memberIdToRemove
    ) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();

        partyCommandService.removeMember(partyId, memberIdToRemove, currentMemberId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @PostMapping("/parties/{partyId}/join-requests")
    @Operation(summary = "모임 가입 신청",
            description = "사용자가 특정 모임에 가입을 신청합니다")
    @ApiResponse(responseCode = "201", description = "가입 신청 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임 또는 사용자")
    @ApiResponse(responseCode = "409", description = "이미 가입했거나 신청 대기 중인 상태")
    public BaseResponse<PartyJoinCreateDTO.Response> createJoinRequest(
            @PathVariable Long partyId
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

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
            @RequestParam(name = "status", defaultValue = "PENDING") String status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        Slice<PartyJoinDTO.Response> response = partyQueryService.getJoinRequests(partyId, memberId, status, pageable);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @PatchMapping("/parties/{partyId}/join-requests/{requestId}")
    @Operation(summary = "모임 가입 신청 처리",
        description = "모임장이 가입 신청을 승인하거나 거절합니다.")
    @ApiResponse(responseCode = "200", description = "가입 신청 처리 성공")
    @ApiResponse(responseCode = "403", description = "모임장 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임 또는 가입 신청")
    @ApiResponse(responseCode = "409", description = "이미 처리된 가입 신청")
    public BaseResponse<Void> actionJoinRequests(
            @PathVariable Long partyId,
            @PathVariable Long requestId,
            @RequestBody @Valid PartyJoinActionDTO.Request request
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        partyCommandService.actionJoinRequest(partyId, memberId, request, requestId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @GetMapping("/parties/{partyId}/members/suggestions")
    @Operation(summary = "신규 멤버 추천받기",
            description = "자신의 모임에 초대할 만한 신규 멤버를 추천받습니다.")
    public BaseResponse<Slice<PartyMemberSuggestionDTO.Response>> etRecommendedMembers(
            @PathVariable Long partyId,
            @RequestParam(required = false) String levelSearch,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Slice<PartyMemberSuggestionDTO.Response> response = partyQueryService.getRecommendedMembers(partyId, levelSearch, pageable);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @PostMapping("/parties/{partyId}/invitations")
    @Operation(summary = "신규 멤버 초대 보내기",
            description = "모임장이 특정 사용자에게 모임에 가입하도록 초대를 보냅니다.")
    @ApiResponse(responseCode = "201", description = "신규 멤버 초대 성공")
    @ApiResponse(responseCode = "403", description = "모임장 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임 또는 사용자")
    @ApiResponse(responseCode = "409", description = "이미 멤버이거나 초대 대기 중인 상태")
    public BaseResponse<PartyInviteCreateDTO.Response> createInvitation(
            @PathVariable Long partyId,
            @Valid @RequestBody PartyInviteCreateDTO.Request request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();

        PartyInviteCreateDTO.Response response = partyCommandService.createInvitation(partyId, request.userId(), memberId);
        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @PatchMapping("/parties/invitations/{invitationId}")
    @Operation(summary = "모임 초대 처리",
            description = "사용자가 모임 초대를 승인하거나 거절합니다.")
    @ApiResponse(responseCode = "200", description = "모임 초대 처리 성공")
    @ApiResponse(responseCode = "403", description = "해당 사용자의 초대가 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임 초대")
    @ApiResponse(responseCode = "409", description = "이미 처리된 모임 초대")
    public BaseResponse<Void> actionInvitation(
            @PathVariable Long invitationId,
            @RequestBody @Valid PartyInviteActionDTO.Request request
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        partyCommandService.actionInvitation(memberId, request, invitationId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @PostMapping("/parties/{partyId}/keywords")
    @Operation(summary = "모임 키워드 추가",
            description = "사용자가 모임에 키워드를 추가합니다.")
    @ApiResponse(responseCode = "200", description = "키워드 추가 성공")
    @ApiResponse(responseCode = "403", description = "모임장 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    @ApiResponse(responseCode = "409", description = "이미 추가된 키워드")
    public BaseResponse<Void> addKeyword(
            @PathVariable Long partyId,
            @RequestBody @Valid PartyKeywordDTO.Request request
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        partyCommandService.addKeyword(partyId, memberId, request);
        return BaseResponse.success(CommonSuccessCode.OK);
    }
}
