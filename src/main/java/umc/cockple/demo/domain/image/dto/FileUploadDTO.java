package umc.cockple.demo.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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