package umc.cockple.demo.domain.image.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.image.exception.S3ErrorCode;
import umc.cockple.demo.domain.image.exception.S3Exception;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final AmazonS3 amazonS3;


    //이미지 업로드 임시 코드
    public String uploadImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        log.info("[이미지 업로드 시작]");

        String key = getFileKey(image); // 예: contest-images/uuid.jpg

        try {
            amazonS3.putObject(
                    bucket,
                    key,
                    image.getInputStream(),
                    null // metadata 생략 가능
            );

        } catch (AmazonServiceException e) {
            log.error("[S3 업로드 실패 - AWS 예외] {}", e.getMessage());
            throw new S3Exception(S3ErrorCode.IMAGE_UPLOAD_AMAZON_EXCEPTION);
        } catch (IOException e) {
            log.error("[S3 업로드 실패 - IO 예외] {}", e.getMessage());
            throw new S3Exception(S3ErrorCode.IMAGE_UPLOAD_IO_EXCEPTION);
        }

        log.info("[이미지 업로드 완료]");

        // 업로드된 이미지의 전체 URL 반환
        return amazonS3.getUrl(bucket, key).toString();
    }

    /**
     * 다중 이미지 업로드
     * @param images MultipartFile 이미지 리스트
     * @return 업로드된 이미지 URL 리스트
     */
    public List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of(); // 빈 리스트 반환
        }

        return images.stream()
                .map(this::uploadImage)
                .collect(Collectors.toList());
    }

    public void delete(String imgKey) {
        try {
            amazonS3.deleteObject(bucket, imgKey);
            log.info("[S3 삭제 성공] {}", imgKey);
        } catch (Exception e) {
            log.error("[S3 삭제 실패] {}", e.getMessage());
            throw new S3Exception(S3ErrorCode.IMAGE_DELETE_EXCEPTION);
        }
    }

    public String getFileKey(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        // 원본 파일명에서 확장자 추출
        String originalFilename = image.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);

        // UUID 기반 유니크 키 생성
        String uuid = UUID.randomUUID().toString();

        // 예: contest-images/uuid.jpg
        return "contest-images/" + uuid + "." + extension;
    }
}