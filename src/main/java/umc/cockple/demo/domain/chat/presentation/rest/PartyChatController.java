package umc.cockple.demo.domain.chat.presentation.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomIdDTO;
import umc.cockple.demo.domain.chat.presentation.rest.api.PartyChatApi;
import umc.cockple.demo.domain.chat.service.query.PartyChatRoomIdQueryService;
import umc.cockple.demo.domain.chat.service.query.PartyChatRoomQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@Validated
public class PartyChatController implements PartyChatApi {

    private final PartyChatRoomQueryService partyChatRoomQueryService;
    private final PartyChatRoomIdQueryService partyChatRoomIdQueryService;

    @Override
    public BaseResponse<PartyChatRoomDTO.Response> getPartyChatRooms(int page, int size) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        PartyChatRoomDTO.Response response = partyChatRoomQueryService.getPartyChatRooms(memberId, page, size);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @Override
    public BaseResponse<PartyChatRoomDTO.Response> searchPartyChatRooms(String name, int page, int size) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        PartyChatRoomDTO.Response response = partyChatRoomQueryService.searchPartyChatRoomsByName(memberId, name, page, size);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @Override
    public BaseResponse<PartyChatRoomIdDTO> getChatRoomId(Long partyId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        PartyChatRoomIdDTO response = partyChatRoomIdQueryService.getChatRoomId(partyId, memberId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }
}
