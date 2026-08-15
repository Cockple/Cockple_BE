package umc.cockple.demo.domain.chat.service.file;

import com.google.cloud.storage.Blob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.DownloadToken;
import umc.cockple.demo.domain.chat.dto.ChatDownloadTokenDTO;
import umc.cockple.demo.domain.chat.dto.ChatFileDownloadDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.DownloadTokenRepository;
import umc.cockple.demo.domain.chat.service.support.reader.ChatFileReader;
import umc.cockple.demo.domain.chat.service.support.reader.DownloadTokenReader;
import umc.cockple.demo.domain.file.service.FileService;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatFileService {

    private final ChatFileReader chatFileReader;
    private final DownloadTokenReader downloadTokenReader;
    private final DownloadTokenRepository downloadTokenRepository;
    private final ChatConverter chatConverter;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final FileService fileService;
    private static final int TOKEN_VALIDITY_SECONDS = 180;

    public ChatDownloadTokenDTO.Response issueDownloadToken(Long fileId, Long memberId) {
        log.info("다운로드 토큰 발급 시작 - fileId: {}, memberId: {}", fileId, memberId);

        ChatMessageFile chatFile = chatFileReader.read(fileId);
        validateMemberPermission(chatFile, memberId);

        DownloadToken downloadToken = DownloadToken.create(fileId, memberId, TOKEN_VALIDITY_SECONDS);
        downloadTokenRepository.save(downloadToken);

        log.info("다운로드 토큰 발급 완료 - fileId: {}", fileId);
        return chatConverter.toDownloadTokenResponse(downloadToken, TOKEN_VALIDITY_SECONDS);
    }

    public ChatFileDownloadDTO.Response downloadFile(Long fileId, String token) {
        log.info("파일 다운로드 시작 - fileId: {}", fileId);

        validateToken(fileId, token);
        ChatMessageFile chatFile = chatFileReader.read(fileId);

        Blob blob = fileService.downloadFile(chatFile.getFileKey());
        ChatFileDownloadDTO.Response response = new ChatFileDownloadDTO.Response(
                chatFile.getOriginalFileName(),
                blob.getContentType(),
                blob.getSize(),
                blob.getContent()
        );

        log.info("파일 다운로드 완료 - fileName: {}", chatFile.getOriginalFileName());
        return response;
    }

    private void validateMemberPermission(ChatMessageFile chatFile, Long memberId) {
        Long roomId = chatFile.getChatMessage().getChatRoom().getId();
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId))
            throw new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    private void validateToken(Long fileId, String tokenValue) {
        DownloadToken token = downloadTokenReader.read(tokenValue);
        //토큰 유효성 검증 (만료 시간, 파일 ID)
        if (token.getExpiresAt().isBefore(LocalDateTime.now()) || !token.getFileId().equals(fileId)) {
            throw new ChatException(ChatErrorCode.INVALID_DOWNLOAD_TOKEN);
        }
        //사용된 토큰 삭제
        downloadTokenRepository.delete(token);
    }

}
