package umc.cockple.demo.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.member.dto.GetMyProfileResponseDTO;
import umc.cockple.demo.domain.member.dto.GetNowAddressResponseDTO;
import umc.cockple.demo.domain.member.dto.GetProfileResponseDTO;
import umc.cockple.demo.domain.member.dto.UpdateProfileRequestDTO;
import umc.cockple.demo.domain.member.service.MemberCommandService;
import umc.cockple.demo.domain.member.service.MemberQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

import java.io.IOException;

import static umc.cockple.demo.domain.member.dto.CreateMemberAddrDTO.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Member", description = "회원 API")
public class MemberController {

    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;


    @PatchMapping(value = "/member")
    @Operation(summary = "회원 탈퇴 API",
            description = "사용자 회원 탈퇴")
    public BaseResponse<String> withdraw() {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

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
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        return BaseResponse.success(CommonSuccessCode.OK, memberQueryService.getMyProfile(memberId));
    }


    @PatchMapping(value = "/my/profile")
    @Operation(summary = "프로필 수정 API",
            description = "사용자가 자신의 프로필 수정")
    public BaseResponse<Object> updateProfile(@RequestBody @Valid UpdateProfileRequestDTO requestDTO) throws IOException {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        memberCommandService.updateProfile(requestDTO, memberId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }


    // =========== 주소 관련 ==============


    @PostMapping("/my/profile/locations")
    @Operation(summary = "회원 주소 추가 API",
            description = "사용자가 자신의 주소 추가")
    public BaseResponse<CreateMemberAddrResponseDTO> createMemberAddress(@RequestBody CreateMemberAddrRequestDTO requestDto) {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        return BaseResponse.success(CommonSuccessCode.CREATED, memberCommandService.addMemberNewAddr(requestDto, memberId));
    }

    @PatchMapping("/my/profile/locations/{memberAddrId}")
    @Operation(summary = "대표 주소 변경 API",
            description = "사용자가 자신의 대표주소 변경")
    public BaseResponse<String> updateMainAddr(@PathVariable Long memberAddrId) {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        memberCommandService.updateMainAddr(memberId, memberAddrId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @DeleteMapping("/my/profile/locations/{memberAddrId}")
    @Operation(summary = "회원 주소 삭제 API",
            description = "사용자가 자신의 주소 중 원하는 주소를 삭제")
    public BaseResponse<String> deleteMemberAddr(@PathVariable Long memberAddrId) {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        memberCommandService.deleteMemberAddr(memberId, memberAddrId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @GetMapping("/my/location")
    @Operation(summary = "회원 현재 위치 조회 API",
            description = "사용자의 현재 위치를 조회 (홈 화면 상단에 해당 위치의 동을 띄울 때 사용)")
    public BaseResponse<GetNowAddressResponseDTO> getNowAddress() {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        return BaseResponse.success(CommonSuccessCode.OK, memberQueryService.getNowAddress(memberId));

    }

}
