package umc.cockple.demo.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.chat.dto.ChatRoomDetailDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomCreateDTO;
import umc.cockple.demo.domain.chat.dto.DirectChatRoomDTO;
import umc.cockple.demo.domain.chat.dto.PartyChatRoomDTO;
import umc.cockple.demo.domain.chat.enums.Direction;
import umc.cockple.demo.domain.chat.service.ChatCommandService;
import umc.cockple.demo.domain.chat.service.ChatQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
@Tag(name = "Chat", description = "채팅 관리 API")
public class ChatController {

    private final ChatQueryService chatQueryService;
    private final ChatCommandService chatCommandService;

    @PostMapping(value = "/chats/direct")
    @Operation(summary = "개인 채팅방 생성 및 참여", description = "개인 채팅방을 생성하고 상대방과 함께 참여합니다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    public BaseResponse<DirectChatRoomCreateDTO.Response> createDirectChatRoom(
            //@AuthenticationPrincipal Long memberId,
            @RequestParam Long targetMemberId
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값
        DirectChatRoomCreateDTO.Response response = chatCommandService.createDirectChatRoom(memberId, targetMemberId);
        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @GetMapping(value = "/chats/parties")
    @Operation(summary = "모임 채팅방 목록 조회", description = "회원이 자신의 모임 채팅방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<PartyChatRoomDTO.Response> getPartyChatRooms(
            //@AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "DESC") Direction direction
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값
        PartyChatRoomDTO.Response response = chatQueryService.getPartyChatRooms(memberId, cursor, size, direction);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/chats/parties/search")
    @Operation(summary = "모임 채팅방 이름 검색", description = "회원이 자신의 모임 채팅방을 이름으로 검색합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<PartyChatRoomDTO.Response> searchPartyChatRooms(
            //@AuthenticationPrincipal Long memberId,
            @RequestParam String name,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "DESC") Direction direction
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값
        PartyChatRoomDTO.Response response = chatQueryService.searchPartyChatRoomsByName(memberId, name, cursor, size, direction);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/chats/direct")
    @Operation(summary = "개인 채팅방 목록 조회", description = "회원이 자신의 개인 채팅방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<DirectChatRoomDTO.Response> getDirectChatRooms(
            //@AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "DESC") Direction direction
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값
        DirectChatRoomDTO.Response response = chatQueryService.getDirectChatRooms(memberId, cursor, size, direction);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/chats/direct/search")
    @Operation(summary = "개인 채팅방 이름 검색", description = "회원이 자신의 개인 채팅방을 이름으로 검색합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<DirectChatRoomDTO.Response> searchDirectChatRooms(
            //@AuthenticationPrincipal Long memberId,
            @RequestParam String name,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "DESC") Direction direction
    ) {
        // TODO: JWT 인증 구현 후 교체 예정
        Long memberId = 1L; // 임시값
        DirectChatRoomDTO.Response response = chatQueryService.searchDirectChatRoomsByName(memberId, name, cursor, size, direction);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/chats/rooms/{roomId}")
    @Operation(summary = "초기 채팅방 조회", description = "채팅방의 정보와 회원이 참여한 채팅방의 메시지를 최근 50개만 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ChatRoomDetailDTO.Response> getChatRoom(
            @PathVariable Long roomId
    ){
        Long memberId = SecurityUtil.getCurrentMemberId();

        ChatRoomDetailDTO.Response response = chatQueryService.getChatRoom(roomId, memberId);

        return BaseResponse.success(CommonSuccessCode.OK, response);
    }
}
