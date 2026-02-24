package umc.cockple.demo.domain.chat.service;

import com.google.cloud.storage.Blob;
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
import umc.cockple.demo.domain.chat.domain.ChatMessageImg;
import umc.cockple.demo.domain.chat.domain.DownloadToken;
import umc.cockple.demo.domain.chat.dto.ChatDownloadTokenDTO;
import umc.cockple.demo.domain.chat.exception.ChatErrorCode;
import umc.cockple.demo.domain.chat.exception.ChatException;
import umc.cockple.demo.domain.chat.repository.ChatImageRepository;
import umc.cockple.demo.domain.chat.repository.ChatRoomMemberRepository;
import umc.cockple.demo.domain.chat.repository.DownloadTokenRepository;
import umc.cockple.demo.domain.image.service.ImageService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatImageServiceImpl implements ChatImageService{

    private final ChatImageRepository chatImageRepository;
    private final DownloadTokenRepository downloadTokenRepository;
    private final ChatConverter chatConverter;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ImageService imageService;
    private static final int TOKEN_VALIDITY_SECONDS = 180;

    @Override
    public ChatDownloadTokenDTO.Response issueDownloadToken(Long fileId, Long memberId) {
        log.info("다운로드 토큰 발급 시작 - fileId: {}, memberId: {}", fileId, memberId);

        //이미지 파일 조회
        ChatMessageImg chatImage = findChatImageOrThrow(fileId);

        //사용자 검증
        validateMemberPermission(chatImage, memberId);

        //다운로드 토큰 생성 및 저장
        DownloadToken downloadToken = DownloadToken.create(fileId, memberId, TOKEN_VALIDITY_SECONDS);
        downloadTokenRepository.save(downloadToken);

        log.info("다운로드 토큰 발급 완료 - fileId: {}", fileId);
        return chatConverter.toDownloadTokenResponse(downloadToken, TOKEN_VALIDITY_SECONDS);
    }

    @Override
    public ResponseEntity<Resource> downloadImage(Long imageId, String token) {
        log.info("이미지 다운로드 시작 - imageId: {}", imageId);

        //토큰 검증
        validateToken(imageId, token);
        //채팅 파일 조회
        ChatMessageImg chatImage = findChatImageOrThrow(imageId);

        //GCS에서 파일 객체 직접 가져오기
        Blob blob = imageService.downloadFile(chatImage.getImgKey());
        ResponseEntity<Resource> responseEntity = createDownloadResponseEntity(chatImage, blob);

        log.info("이미지 다운로드 완료 - imageName: {}", chatImage.getOriginalFileName());
        return responseEntity;
    }

    private ChatMessageImg findChatImageOrThrow(Long imageId) {
        return chatImageRepository.findById(imageId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.FILE_NOT_FOUND));
    }

    private void validateMemberPermission(ChatMessageImg chatImage, Long memberId) {
        Long roomId = chatImage.getChatMessage().getChatRoom().getId();
        if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(roomId, memberId))
            throw new ChatException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    private void validateToken(Long ImageId, String tokenValue) {
        DownloadToken token = downloadTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ChatException(ChatErrorCode.INVALID_DOWNLOAD_TOKEN));
        //토큰 유효성 검증 (만료 시간, 이미지 ID)
        if (token.getExpiresAt().isBefore(LocalDateTime.now()) || !token.getFileId().equals(ImageId)) {
            throw new ChatException(ChatErrorCode.INVALID_DOWNLOAD_TOKEN);
        }
        //사용된 토큰 삭제
        downloadTokenRepository.delete(token);
    }

    private ResponseEntity<Resource> createDownloadResponseEntity(ChatMessageImg chatMessageImg, Blob blob) {
        //GCS 객체에서 직접 메타데이터를 가져오기
        long contentLength = blob.getSize();
        String contentType = blob.getContentType();
        Resource resource = new InputStreamResource(new java.io.ByteArrayInputStream(blob.getContent()));

        //헤더 생성
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(chatMessageImg.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(contentLength);

        return ResponseEntity.ok().headers(headers).body(resource);
    }
}