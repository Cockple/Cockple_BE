package umc.cockple.demo.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.member.dto.*;
import umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO;
import umc.cockple.demo.domain.member.service.MemberCommandService;
import umc.cockple.demo.domain.member.service.MemberQueryService;
import umc.cockple.demo.global.jwt.domain.TokenRefreshResponse;
import umc.cockple.demo.global.oauth2.service.KakaoOauthService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static umc.cockple.demo.domain.member.dto.CreateMemberAddrDTO.*;
import static umc.cockple.demo.domain.member.dto.kakao.KakaoLoginDTO.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Member", description = "회원 API")
public class MemberController {

    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;
    private final KakaoOauthService kakaoOauthService;

    @PostMapping("/oauth/login")
    @Operation(summary = "카카오 소셜 로그인 API",
            description = "카카오에서 인가코드를 발급받아 요청으로 넣어주세요. 기존 회원의 경우 로그인, 신규 회원의 경우 회원가입을 합니다.")
    public ResponseEntity<KakaoLoginResponseDTO> login(@RequestBody @Valid KakaoLoginRequestDTO requestDTO) {

        KakaoLoginResponseDTO response = kakaoOauthService.signup(requestDTO.code());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("None")
                .build()
                ;

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new KakaoLoginResponseDTO(
                        response.accessToken(),
                        null,
                        response.memberId(),
                        response.nickname(),
                        response.isNewMember()
                ));
    }


    @PostMapping("/my/details")
    @Operation(summary = "로그인 후 상세 정보 받기 API",
            description = "로그인 후 추가적인 상세 정보를 받습니다.")
    public BaseResponse<String> memberDetailInfo(@RequestBody @Valid MemberDetailInfoRequestDTO requestDTO) {

        Long memberId = 2L;

        memberCommandService.memberDetailInfo(memberId, requestDTO);

        return BaseResponse.success(CommonSuccessCode.OK);
    }


    @PostMapping("/auth/refresh")
    @Operation(summary = "리프레시 토큰 재발급 API",
            description = "리프레시 토큰이 만료되었을 경우 (1주일) 재발급 해주는 api입니다. 리프레시토큰은 헤더에 쿠키로 들어갑니다.")
    public ResponseEntity<TokenRefreshResponse> refresh(@CookieValue("refreshToken") String refreshToken) {
        // 리프레시 토큰 유효성 검사
        TokenRefreshResponse response = kakaoOauthService.validateMember(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenRefreshResponse(response.accessToken(), null));
    }


    @PatchMapping(value = "/member")
    @Operation(summary = "회원 탈퇴 API",
            description = "사용자 회원 탈퇴")
    public BaseResponse<String> withdraw() {

        Long memberId = SecurityUtil.getCurrentMemberId();

        memberCommandService.withdrawMember(memberId);
        return BaseResponse.success(CommonSuccessCode.NO_CONTENT);
    }


    // ============== 프로필 관련 =================


    @GetMapping(value = "/profile/{memberId}")
    @Operation(summary = "프로필 조회 API",
            description = "사용자가 다른 사람의 프로필을 조회")
    public BaseResponse<GetProfileResponseDTO> getProfile(@PathVariable Long memberId) {

        return BaseResponse.success(CommonSuccessCode.OK, memberQueryService.getProfile(memberId));
    }

    @GetMapping(value = "/my/profile")
    @Operation(summary = "내 프로필 조회 API",
            description = "사용자가 자신의 프로필을 조회")
    public BaseResponse<GetMyProfileResponseDTO> getMyProfile() {

        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.OK, memberQueryService.getMyProfile(memberId));
    }


    @PatchMapping(value = "/my/profile")
    @Operation(summary = "프로필 수정 API",
            description = "사용자가 자신의 프로필 수정")
    public BaseResponse<Object> updateProfile(@RequestBody @Valid UpdateProfileRequestDTO requestDTO) throws IOException {

        Long memberId = SecurityUtil.getCurrentMemberId();

        memberCommandService.updateProfile(requestDTO, memberId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }


    // =========== 주소 관련 ==============


    @PostMapping("/my/profile/locations")
    @Operation(summary = "회원 주소 추가 API",
            description = "사용자가 자신의 주소 추가")
    public BaseResponse<CreateMemberAddrResponseDTO> createMemberAddress(@RequestBody @Valid CreateMemberAddrRequestDTO requestDto) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.CREATED, memberCommandService.addMemberNewAddr(requestDto, memberId));
    }

    @PatchMapping("/my/profile/locations/{memberAddrId}")
    @Operation(summary = "대표 주소 변경 API",
            description = "사용자가 자신의 대표주소 변경")
    public BaseResponse<String> updateMainAddr(@PathVariable Long memberAddrId) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        memberCommandService.updateMainAddr(memberId, memberAddrId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @DeleteMapping("/my/profile/locations/{memberAddrId}")
    @Operation(summary = "회원 주소 삭제 API",
            description = "사용자가 자신의 주소 중 원하는 주소를 삭제")
    public BaseResponse<String> deleteMemberAddr(@PathVariable Long memberAddrId) {

        Long memberId = SecurityUtil.getCurrentMemberId();

        memberCommandService.deleteMemberAddr(memberId, memberAddrId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @GetMapping("/my/location")
    @Operation(summary = "회원 현재 위치 조회 API",
            description = "사용자의 현재 위치를 조회 (홈 화면 상단에 해당 위치의 동을 띄울 때 사용)")
    public BaseResponse<GetNowAddressResponseDTO> getNowAddress() {

        Long memberId = SecurityUtil.getCurrentMemberId();

        return BaseResponse.success(CommonSuccessCode.OK, memberQueryService.getNowAddress(memberId));

    }

    @GetMapping("/my/profile/locations")
    @Operation(summary = "회원 주소 전체 조회 API",
            description = "사용자가 등록한 모든 주소를 조회")
    public BaseResponse<List<GetAllAddressResponseDTO>> getAllAddress() {

        Long memberId = SecurityUtil.getCurrentMemberId();

        List<GetAllAddressResponseDTO> addresses = memberQueryService.getAllAddress(memberId);
        return BaseResponse.success(CommonSuccessCode.OK, addresses);
    }
}
