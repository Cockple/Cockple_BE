package umc.cockple.demo.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    private final ObjectMapper objectMapper;

    @PatchMapping(value = "/my/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 수정 API",
            description = "사용자가 자신의 프로필 수정")
    public BaseResponse<Object> updateProfile(@RequestPart("updateProfileRequestDto") String  updateProfileRequestDto,
                                              @RequestPart(value = "profileImg", required = false) MultipartFile profileImage) throws IOException {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        UpdateProfileRequestDTO requestDto = objectMapper.readValue(updateProfileRequestDto, UpdateProfileRequestDTO.class);

        memberCommandService.updateProfile(requestDto, profileImage, memberId);
        return BaseResponse.success(CommonSuccessCode.OK);
    }

    @PostMapping("/my/profile/locations")
    @Operation(summary = "회원 주소 추가 API",
            description = "사용자가 자신의 주소 추가")
    public BaseResponse<CreateMemberAddrResponseDTO> createMemberAddress(@RequestBody CreateMemberAddrRequestDTO requestDto) {
        // 추후 시큐리티를 통해 id 가져옴
        Long memberId = 1L;

        return BaseResponse.success(CommonSuccessCode.CREATED, memberCommandService.addMemberNewAddr(requestDto, memberId));
    }

}
