package umc.cockple.demo.domain.chat.presentation.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomIdDTO;
import umc.cockple.demo.global.response.BaseResponse;

@RequestMapping("/api/chats/parties")
@ChatApiTag
public interface PartyChatApi {

    @GetMapping
    @Operation(summary = "모임 채팅방 목록 조회", description = "회원이 자신의 모임 채팅방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    BaseResponse<PartyChatRoomDTO.Response> getPartyChatRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/search")
    @Operation(summary = "모임 채팅방 이름 검색", description = "회원이 자신의 모임 채팅방을 이름으로 검색합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    BaseResponse<PartyChatRoomDTO.Response> searchPartyChatRooms(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/{partyId}")
    @Operation(summary = "모임 채팅방 ID 조회")
    @ApiResponse(responseCode = "200", description = "채팅방 ID 조회 성공")
    @ApiResponse(responseCode = "403", description = "채팅방 접근 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 모임 또는 채팅방")
    BaseResponse<PartyChatRoomIdDTO> getChatRoomId(
            @PathVariable Long partyId
    );
}
