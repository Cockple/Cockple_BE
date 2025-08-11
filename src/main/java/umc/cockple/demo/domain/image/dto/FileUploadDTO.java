package umc.cockple.demo.domain.image.dto;

import lombok.Builder;

public class FileUploadDTO {
    @Builder
    public record Response(
            String fileKey,
            String fileUrl,
            String originalFileName,
            Long fileSize,
            String fileType
    ) {}
}