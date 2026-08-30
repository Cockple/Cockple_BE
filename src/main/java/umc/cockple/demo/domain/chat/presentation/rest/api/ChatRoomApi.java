package umc.cockple.demo.domain.chat.presentation.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.ChatUnreadStatusDTO;
import umc.cockple.demo.global.response.BaseResponse;

@RequestMapping("/api/chats")
@ChatApiTag
public interface ChatRoomApi {

    @GetMapping("/unread-status")
    @Operation(summary = "채팅 안 읽은 메시지 여부 조회", description = "내비 바와 채팅 탭 표시를 위해 모임/개인 채팅의 안 읽은 메시지 존재 여부를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    BaseResponse<ChatUnreadStatusDTO.Response> getUnreadStatus();

    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "초기 채팅방 조회", description = "채팅방의 정보와 회원이 참여한 채팅방의 메시지를 최근 50개만 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    BaseResponse<ChatRoomDetailDTO.Response> getChatRoomDetail(
            @PathVariable Long roomId
    );

    @GetMapping("/rooms/{roomId}/messages/previous")
    @Operation(summary = "채팅방 과거 메시지 조회", description = "채팅방의 과거 메시지를 페이징하여 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    BaseResponse<ChatMessageDTO.Response> getChatMessages(
            @PathVariable Long roomId,
            @RequestParam Long cursor,
            @RequestParam(defaultValue = "50") int size
    );
}
