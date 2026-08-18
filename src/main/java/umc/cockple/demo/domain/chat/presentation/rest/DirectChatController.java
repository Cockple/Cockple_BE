package umc.cockple.demo.domain.chat.presentation.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomCreateDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.presentation.rest.api.DirectChatApi;
import umc.cockple.demo.domain.chat.service.command.DirectChatRoomCommandService;
import umc.cockple.demo.domain.chat.service.query.DirectChatRoomQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@Validated
public class DirectChatController implements DirectChatApi {

    private final DirectChatRoomCommandService directChatRoomCommandService;
    private final DirectChatRoomQueryService directChatRoomQueryService;

    @Override
    public BaseResponse<DirectChatRoomCreateDTO.Response> createDirectChatRoom(Long targetMemberId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        DirectChatRoomCreateDTO.Response response =
                directChatRoomCommandService.createDirectChatRoom(memberId, targetMemberId);
        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Override
    public BaseResponse<DirectChatRoomDTO.Response> getDirectChatRooms(int page, int size) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        DirectChatRoomDTO.Response response = directChatRoomQueryService.getDirectChatRooms(memberId, page, size);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @Override
    public BaseResponse<DirectChatRoomDTO.Response> searchDirectChatRooms(String name, int page, int size) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        DirectChatRoomDTO.Response response =
                directChatRoomQueryService.searchDirectChatRoomsByName(memberId, name, page, size);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }
}
