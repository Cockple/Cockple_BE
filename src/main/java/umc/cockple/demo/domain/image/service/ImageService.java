package umc.cockple.demo.domain.image.service;

import com.google.cloud.storage.*;
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

    @Value("${gcs.bucket}")
    private String bucket;

    private final Storage storage;

    public ImageUploadDTO.Response uploadImage(MultipartFile image, DomainType domainType) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        log.info("[이미지 업로드 시작]");

        String originalFileName = image.getOriginalFilename();
        String key = getFileKey(image, domainType);
        String imgUrl = uploadToGcs(image, key);

        log.info("[이미지 업로드 완료]");
        return ImageUploadDTO.Response.builder()
                .imgUrl(imgUrl)
                .imgKey(key)
                .originalFileName(originalFileName)
                .fileSize(image.getSize())
                .fileType(image.getContentType())
                .build();
    }

    public FileUploadDTO.Response uploadFile(MultipartFile file, DomainType domainType) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        log.info("[파일 업로드 시작]");

        String originalFileName = file.getOriginalFilename();
        String key = getFileKey(file, domainType);
        String fileUrl = uploadToGcs(file, key);

        log.info("[파일 업로드 완료]");
        return FileUploadDTO.Response.builder()
                .fileKey(key)
                .fileUrl(fileUrl)
                .originalFileName(originalFileName)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .build();
    }

    public List<ImageUploadDTO.Response> uploadImages(List<MultipartFile> images, DomainType domainType) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return images.stream()
                .map(img -> uploadImage(img, domainType))
                .collect(Collectors.toList());
    }

    public void delete(String imgKey) {
        try {
            storage.delete(BlobId.of(bucket, imgKey));
            log.info("[GCS 삭제 성공] {}", imgKey);
        } catch (Exception e) {
            log.error("[GCS 삭제 실패] {}", e.getMessage());
            throw new S3Exception(S3ErrorCode.IMAGE_DELETE_EXCEPTION);
        }
    }

    private String uploadToGcs(MultipartFile file, String key) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, key)
                    .setContentType(file.getContentType())
                    .build();
            storage.create(blobInfo, file.getBytes());
            return String.format("https://storage.googleapis.com/%s/%s", bucket, key);
        } catch (IOException e) {
            log.error("[GCS 업로드 실패 - IO 예외] {}", e.getMessage());
            throw new S3Exception(S3ErrorCode.FILE_UPLOAD_IO_EXCEPTION);
        } catch (StorageException e) {
            log.error("[GCS 업로드 실패 - Storage 예외] {}", e.getMessage());
            throw new S3Exception(S3ErrorCode.FILE_UPLOAD_AMAZON_EXCEPTION);
        }
    }

    public String getFileKey(MultipartFile file, DomainType domainType) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();

        return domainType.getDirectory() + "/" + uuid + "." + extension;
    }

    public String getUrlFromKey(String key) {
        return String.format("https://storage.googleapis.com/%s/%s", bucket, key);
    }

    public Blob downloadFile(String fileKey) {
        Blob blob = storage.get(BlobId.of(bucket, fileKey));
        if (blob == null) {
            throw new S3Exception(S3ErrorCode.IMAGE_DELETE_EXCEPTION);
        }
        return blob;
    }
}