package umc.cockple.demo.domain.party.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.party.dto.PartyCreateRequestDTO;
import umc.cockple.demo.domain.party.dto.PartyCreateResponseDTO;
import umc.cockple.demo.domain.party.service.PartyCommandService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "Party", description = "모임 관리 API")
public class PartyController {

    private final PartyCommandService partyCommandService;

    @PostMapping(value = "/parties", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "모임 생성",
            description = "새로운 모임을 생성합니다. 성공 시 사용자는 해당 모임의 모임장이 됩니다.")
    @ApiResponse(responseCode = "201", description = "모임 생성 성공")
    @ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패 또는 잘못된 요청 형식")
    @ApiResponse(responseCode = "403", description = "모임 생성 권한 없음")
    public BaseResponse<PartyCreateResponseDTO> createParty(
            //사진 파일을 함께 보내기 위해 @RequestPart로 구현
            @RequestPart("createpartyRequest") @Valid PartyCreateRequestDTO request,
            @RequestPart(value = "profileImg", required = false) MultipartFile profileImage,
            Authentication authentication
    ){
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값

        //서비스 호출
        PartyCreateResponseDTO response = partyCommandService.createParty(memberId, profileImage, request);

        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }
}
