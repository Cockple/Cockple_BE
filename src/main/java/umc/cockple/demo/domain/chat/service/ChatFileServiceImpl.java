package umc.cockple.demo.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.cockple.demo.domain.chat.converter.ChatConverter;
import umc.cockple.demo.domain.chat.domain.ChatMessageFile;
import umc.cockple.demo.domain.chat.domain.DownloadToken;
import umc.cockple.demo.domain.chat.dto.ChatDownloadTokenDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatFileRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.DownloadTokenRepository;
import umc.cockple.demo.domain.image.service.ImageService;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatFileServiceImpl implements ChatFileService{

    private final ChatFileRepository chatFileRepository;
    private final DownloadTokenRepository downloadTokenRepository;
    private final ChatConverter chatConverter;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ImageService imageService;
    private static final int TOKEN_VALIDITY_SECONDS = 60;

    @Override
    public ChatDownloadTokenDTO.Response issueDownloadToken(Long fileId, Long memberId) {
        log.info("다운로드 토큰 발급 시작 - fileId: {}, memberId: {}", fileId, memberId);

        //채팅 파일 조회
        ChatMessageFile chatFile = findChatFileOrThrow(fileId);

        //사용자 검증
        validateMemberPermission(chatFile, memberId);

        //다운로드 토큰 생성 및 저장
        DownloadToken downloadToken = DownloadToken.create(fileId, memberId, TOKEN_VALIDITY_SECONDS);
        downloadTokenRepository.save(downloadToken);

        log.info("다운로드 토큰 발급 완료 - fileId: {}", fileId);
        return chatConverter.toDownloadTokenResponse(downloadToken, TOKEN_VALIDITY_SECONDS);
    }

    @Override
    public ResponseEntity<Resource> downloadFile(Long fileId, String token) {
        log.info("파일 다운로드 시작 - fileId: {}", fileId);

        //토큰 검증
        validateToken(fileId, token);
        //채팅 파일 조회
        ChatMessageFile chatFile = findChatFileOrThrow(fileId);

        //S3에서 파일 리소스 가져오기
        Resource resource = getResourceFromS3(chatFile.getFileKey());
        ResponseEntity<Resource> responseEntity = createDownloadResponseEntity(chatFile, resource);

        log.info("파일 다운로드 완료 - fileName: {}", chatFile.getOriginalFileName());
        return responseEntity;
    }

    private ChatMessageFile findChatFileOrThrow(Long fileId) {
        return chatFileRepository.findById(fileId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.FILE_NOT_FOUND));
    }

    private void validateMemberPermission(ChatMessageFile chatFile, Long memberId) {
        Long roomId = chatFile.getChatMessage().getChatRoom().getId();
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId))
            throw new ChatException(ChatErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
    }

    private void validateToken(Long fileId, String tokenValue) {
        DownloadToken token = downloadTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ChatException(ChatErrorCode.INVALID_DOWNLOAD_TOKEN));
        //토큰 유효성 검증 (만료 시간, 파일 ID)
        if (token.getExpiresAt().isBefore(LocalDateTime.now()) || !token.getFileId().equals(fileId)) {
            throw new ChatException(ChatErrorCode.INVALID_DOWNLOAD_TOKEN);
        }
        //사용된 토큰 삭제
        downloadTokenRepository.delete(token);
    }

    private Resource getResourceFromS3(String fileKey) {
        try {
            return imageService.downloadFile(fileKey);
        } catch (MalformedURLException e) {
            throw new ChatException(ChatErrorCode.FILE_NOT_FOUND);
        }
    }

    private ResponseEntity<Resource> createDownloadResponseEntity(ChatMessageFile chatFile, Resource resource) {
        try {
            String encodedFileName = URLEncoder.encode(chatFile.getOriginalFileName(), StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, chatFile.getFileType());
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(chatFile.getFileSize()));
            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (Exception e) {
            throw new ChatException(ChatErrorCode.RESPONSE_FAILED);
        }
    }
}
