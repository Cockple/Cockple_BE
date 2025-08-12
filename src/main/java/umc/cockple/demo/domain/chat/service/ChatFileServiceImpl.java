package umc.cockple.demo.domain.chat.service;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private static final int TOKEN_VALIDITY_SECONDS = 180;

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

        //S3에서 파일 객체 직접 가져오기
        S3Object s3Object = imageService.downloadFile(chatFile.getFileKey());
        ResponseEntity<Resource> responseEntity = createDownloadResponseEntity(chatFile, s3Object);

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

    private ResponseEntity<Resource> createDownloadResponseEntity(ChatMessageFile chatFile, S3Object s3Object) {
        //S3 객체에서 직접 메타데이터를 가져오기
        long contentLength = s3Object.getObjectMetadata().getContentLength();
        String contentType = s3Object.getObjectMetadata().getContentType();
        S3ObjectInputStream inputStream = s3Object.getObjectContent();
        Resource resource = new InputStreamResource(inputStream);

        //헤더 생성
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(chatFile.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(contentLength);

        return ResponseEntity.ok().headers(headers).body(resource);
    }
}
