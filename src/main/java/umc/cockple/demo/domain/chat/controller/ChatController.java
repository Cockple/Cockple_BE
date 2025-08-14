package umc.cockple.demo.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import umc.cockple.demo.domain.chat.dto.*;
import umc.cockple.demo.domain.chat.service.ChatCommandService;
import umc.cockple.demo.domain.chat.service.ChatFileService;
import umc.cockple.demo.domain.chat.service.ChatQueryService;
import umc.cockple.demo.global.response.BaseResponse;
import umc.cockple.demo.global.response.code.status.CommonSuccessCode;
import umc.cockple.demo.global.security.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
@Validated
@Tag(name = "Chat", description = "채팅 관리 API")
public class ChatController {

    private final ChatQueryService chatQueryService;
    private final ChatCommandService chatCommandService;
    private final ChatFileService chatFileService;

    @GetMapping(value = "/parties")
    @Operation(summary = "모임 채팅방 목록 조회", description = "회원이 자신의 모임 채팅방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<PartyChatRoomDTO.Response> getPartyChatRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        PartyChatRoomDTO.Response response = chatQueryService.getPartyChatRooms(memberId, page, size);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/parties/search")
    @Operation(summary = "모임 채팅방 이름 검색", description = "회원이 자신의 모임 채팅방을 이름으로 검색합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<PartyChatRoomDTO.Response> searchPartyChatRooms(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        PartyChatRoomDTO.Response response = chatQueryService.searchPartyChatRoomsByName(memberId, name, page, size);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @PostMapping(value = "/direct")
    @Operation(summary = "개인 채팅방 생성 및 참여", description = "개인 채팅방을 생성하고 참여합니다. 상대방은 대기 상태로 초대됩니다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    public BaseResponse<DirectChatRoomCreateDTO.Response> createDirectChatRoom(
            @RequestParam Long targetMemberId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        DirectChatRoomCreateDTO.Response response = chatCommandService.createDirectChatRoom(memberId, targetMemberId);
        return BaseResponse.success(CommonSuccessCode.CREATED, response);
    }

    @GetMapping(value = "/direct")
    @Operation(summary = "개인 채팅방 목록 조회", description = "회원이 자신의 개인 채팅방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<DirectChatRoomDTO.Response> getDirectChatRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        DirectChatRoomDTO.Response response = chatQueryService.getDirectChatRooms(memberId, page, size);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping(value = "/direct/search")
    @Operation(summary = "개인 채팅방 이름 검색", description = "회원이 자신의 개인 채팅방을 이름으로 검색합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<DirectChatRoomDTO.Response> searchDirectChatRooms(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        DirectChatRoomDTO.Response response = chatQueryService.searchDirectChatRoomsByName(memberId, name, page, size);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "초기 채팅방 조회", description = "채팅방의 정보와 회원이 참여한 채팅방의 메시지를 최근 50개만 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ChatRoomDetailDTO.Response> getChatRoomDetail(
            @PathVariable Long roomId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChatRoomDetailDTO.Response response = chatQueryService.getChatRoomDetail(roomId, memberId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/rooms/{roomId}/messages/previous")
    @Operation(summary = "채팅방 과거 메시지 조회", description = "채팅방의 과거 메시지를 페이징하여 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public BaseResponse<ChatMessageDTO.Response> getChatMessages(
            @PathVariable Long roomId,
            @RequestParam Long cursor,
            @RequestParam(defaultValue = "50") int size
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChatMessageDTO.Response response = chatQueryService.getChatMessages(roomId, memberId, cursor, size);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @PostMapping("/files/{fileId}/download-token")
    @Operation(summary = "채팅 파일 다운로드 토큰 발급", description = "채팅방에 업로드된 특정 파일을 다운로드할 수 있는 일회용 토큰을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "토큰 발급 성공")
    @ApiResponse(responseCode = "403", description = "파일 접근 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 파일")
    public BaseResponse<ChatDownloadTokenDTO.Response> issueDownloadToken(
            @PathVariable Long fileId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        ChatDownloadTokenDTO.Response response = chatFileService.issueDownloadToken(fileId, memberId);
        return BaseResponse.success(CommonSuccessCode.OK, response);
    }

    @GetMapping("/files/{fileId}/download")
    @Operation(summary = "채팅 파일 다운로드", description = "발급받은 다운로드 토큰을 검증하고, 유효할 경우 실제 파일 데이터를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "파일 다운로드 성공")
    @ApiResponse(responseCode = "403", description = "유효하지 않거나 만료된 토큰")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 파일")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            @RequestParam String token
    ) {
        return chatFileService.downloadFile(fileId, token);
    }
}
