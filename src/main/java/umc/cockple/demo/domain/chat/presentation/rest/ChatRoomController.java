package umc.cockple.demo.domain.chat.presentation.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.chat.dto.ChatMessageDTO;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.ChatUnreadStatusDTO;
import umc.cockple.demo.domain.chat.presentation.rest.api.ChatRoomApi;
import umc.cockple.demo.domain.chat.service.query.ChatMessageHistoryQueryService;
import umc.cockple.demo.domain.chat.service.query.ChatRoomDetailQueryService;
import umc.cockple.demo.domain.chat.service.query.ChatUnreadQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@Validated
public class ChatRoomController implements ChatRoomApi {

    private final ChatUnreadQueryService chatUnreadQueryService;
    private final ChatRoomDetailQueryService chatRoomDetailQueryService;
    private final ChatMessageHistoryQueryService chatMessageHistoryQueryService;

    @Override
    public BaseResponse<ChatUnreadStatusDTO.Response> getUnreadStatus() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChatUnreadStatusDTO.Response response = chatUnreadQueryService.getUnreadStatus(memberId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @Override
    public BaseResponse<ChatRoomDetailDTO.Response> getChatRoomDetail(Long roomId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChatRoomDetailDTO.Response response = chatRoomDetailQueryService.getChatRoomDetail(roomId, memberId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @Override
    public BaseResponse<ChatMessageDTO.Response> getChatMessages(Long roomId, Long cursor, int size) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChatMessageDTO.Response response =
                chatMessageHistoryQueryService.getChatMessages(roomId, memberId, cursor, size);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }
}
