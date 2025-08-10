package umc.cockple.demo.domain.image.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import umc.cockple.demo.domain.image.dto.FileUploadDTO;
import umc.cockple.demo.domain.image.dto.ImageUploadResponseDTO;
import umc.cockple.demo.domain.image.exception.S3ErrorCode;
import umc.cockple.demo.domain.image.exception.S3Exception;
import umc.cockple.demo.global.enums.DomainType;

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


    public ImageUploadResponseDTO uploadImage(MultipartFile image, DomainType domainType) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        log.info("[이미지 업로드 시작]");

        String key = getFileKey(image, domainType); // 예: contest-images/uuid.jpg

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
        String imgUrl = amazonS3.getUrl(bucket, key).toString();
        return new ImageUploadResponseDTO(imgUrl, extractKeyFromUrl(imgUrl, domainType));
    }

    /**
     * 다중 이미지 업로드
     * @param images MultipartFile 이미지 리스트
     * @return 업로드된 이미지 URL 리스트
     */
    public List<ImageUploadResponseDTO> uploadImages(List<MultipartFile> images, DomainType domainType) {
        if (images == null || images.isEmpty()) {
            return List.of(); // 빈 리스트 반환
        }

        return images.stream()
                .map(img -> uploadImage(img, domainType))
                .collect(Collectors.toList());
    }

    public FileUploadDTO.Response uploadFile(MultipartFile file, DomainType domainType) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        log.info("[파일 업로드 시작]");

        String originalFileName = file.getOriginalFilename();
        String fileKey = domainType.name().toLowerCase() + "-files/" + UUID.randomUUID() + "_" + originalFileName;

        try {
            //파일은 메타데이터 필요.
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            amazonS3.putObject(new PutObjectRequest(bucket, fileKey, file.getInputStream(), metadata));

        } catch (AmazonServiceException e) {
            log.error("[S3 업로드 실패 - AWS 예외] {}", e.getMessage());
            throw new S3Exception(S3ErrorCode.FILE_UPLOAD_AMAZON_EXCEPTION);
        } catch (IOException e) {
            log.error("[S3 업로드 실패 - IO 예외] {}", e.getMessage());
            throw new S3Exception(S3ErrorCode.FILE_UPLOAD_IO_EXCEPTION);
        }

        log.info("[파일 업로드 완료]");
        String fileUrl = amazonS3.getUrl(bucket, fileKey).toString();
        return FileUploadDTO.Response.builder()
                .fileKey(fileKey)
                .fileUrl(fileUrl)
                .originalFileName(originalFileName)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .build();
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

    public String getFileKey(MultipartFile image, DomainType domainType) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        // 원본 파일명에서 확장자 추출
        String originalFilename = image.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);

        // UUID 기반 유니크 키 생성
        String uuid = UUID.randomUUID().toString();

        if (domainType == DomainType.CONTEST) {
            return "contest-images/" + uuid + "." + extension;
        } else if (domainType == DomainType.PROFILE) {
            return "profile-image/" + uuid + "." + extension;
        } else if (domainType == DomainType.CHAT) {
            return "chat-images/" + uuid + "." + extension;
        } else {
            return "party-images/" + uuid + "." + extension;
        }

    }

    public String extractKeyFromUrl(String url, DomainType domainType) {
        int startIndex;
        if (domainType == DomainType.CONTEST) {
            startIndex = url.indexOf("contest-images/");
        } else if (domainType == DomainType.PROFILE) {
            startIndex = url.indexOf("profile-image/");
        } else if (domainType == DomainType.CHAT) {
            startIndex = url.indexOf("chat-images/");
        } else {
            startIndex = url.indexOf("party-images/");
        }

        return url.substring(startIndex);
    }

    public String getUrlFromKey(String key) {
        return amazonS3.getUrl(bucket, key).toString();
    }

    public S3Object downloadFile(String fileKey) {
        return amazonS3.getObject(bucket, fileKey);
    }
}