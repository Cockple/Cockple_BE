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
import umc.cockple.demo.domain.image.dto.ImageUploadDTO;
import umc.cockple.demo.domain.image.exception.S3ErrorCode;
import umc.cockple.demo.domain.image.exception.S3Exception;
import umc.cockple.demo.global.enums.DomainType;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final AmazonS3 amazonS3;


    public ImageUploadDTO.Response uploadImage(MultipartFile image, DomainType domainType) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        log.info("[이미지 업로드 시작]");

        String key = getFileKey(image, domainType); // 예: contest-images/uuid.jpg
        String imgUrl = uploadToS3(image, key, false);

        log.info("[이미지 업로드 완료]");
        return ImageUploadDTO.Response.builder()
                .imgUrl(imgUrl)
                .imgKey(key)
                .build();
    }

    public FileUploadDTO.Response uploadFile(MultipartFile file, DomainType domainType) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        log.info("[파일 업로드 시작]");

        String originalFileName = file.getOriginalFilename();
        String key = getFileKey(file, domainType);
        String fileUrl = uploadToS3(file, key, false);

        log.info("[파일 업로드 완료]");
        return FileUploadDTO.Response.builder()
                .fileKey(key)
                .fileUrl(fileUrl)
                .originalFileName(originalFileName)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .build();
    }

    /**
     * 다중 이미지 업로드
     * @param images MultipartFile 이미지 리스트
     * @return 업로드된 이미지 URL 리스트
     */
    public List<ImageUploadDTO.Response> uploadImages(List<MultipartFile> images, DomainType domainType) {
        if (images == null || images.isEmpty()) {
            return List.of(); // 빈 리스트 반환
        }

        return images.stream()
                .map(img -> uploadImage(img, domainType))
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

    private String uploadToS3(MultipartFile file, String key, boolean useMetadata) {
        try {
            if (useMetadata) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(file.getSize());
                metadata.setContentType(file.getContentType());
                amazonS3.putObject(new PutObjectRequest(bucket, key, file.getInputStream(), metadata));
            } else {
                amazonS3.putObject(new PutObjectRequest(bucket, key, file.getInputStream(), null));
            }
            return amazonS3.getUrl(bucket, key).toString();
        } catch (AmazonServiceException e) {
            log.error("[S3 업로드 실패 - AWS 예외] {}", e.getMessage());
            throw new S3Exception(S3ErrorCode.FILE_UPLOAD_AMAZON_EXCEPTION);
        } catch (IOException e) {
            log.error("[S3 업로드 실패 - IO 예외] {}", e.getMessage());
            throw new S3Exception(S3ErrorCode.FILE_UPLOAD_IO_EXCEPTION);
        }
    }

    public String getFileKey(MultipartFile file, DomainType domainType) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 원본 파일명에서 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        // UUID 기반 유니크 키 생성
        String uuid = UUID.randomUUID().toString();

        return domainType.getDirectory() + "/" + uuid + "." + extension;
    }

    public String getUrlFromKey(String key) {
        return amazonS3.getUrl(bucket, key).toString();
    }

    public S3Object downloadFile(String fileKey) {
        return amazonS3.getObject(bucket, fileKey);
    }
}