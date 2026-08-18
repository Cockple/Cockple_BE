package umc.cockple.demo.domain.chat.presentation.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomCreateDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.global.response.BaseResponse;

@RequestMapping("/api/chats/direct")
@ChatApiTag
public interface DirectChatApi {

    @PostMapping
    @Operation(summary = "개인 채팅방 생성 및 참여", description = "개인 채팅방을 생성하고 참여합니다. 상대방은 대기 상태로 초대됩니다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    BaseResponse<DirectChatRoomCreateDTO.Response> createDirectChatRoom(
            @RequestParam Long targetMemberId
    );

    @GetMapping
    @Operation(summary = "개인 채팅방 목록 조회", description = "회원이 자신의 개인 채팅방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    BaseResponse<DirectChatRoomDTO.Response> getDirectChatRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/search")
    @Operation(summary = "개인 채팅방 이름 검색", description = "회원이 자신의 개인 채팅방을 이름으로 검색합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    BaseResponse<DirectChatRoomDTO.Response> searchDirectChatRooms(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );
}
