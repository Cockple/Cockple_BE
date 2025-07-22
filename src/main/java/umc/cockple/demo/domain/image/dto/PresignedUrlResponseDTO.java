package umc.cockple.demo.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Builder
public record PresignedUrlResponseDTO(

        @Schema(description = "이미지 업로드용 presigned URL")
        String presignedUrl,

        @Schema(description = "업로드 후 접근 가능한 이미지 URL")
        String fileUrl
) {


}