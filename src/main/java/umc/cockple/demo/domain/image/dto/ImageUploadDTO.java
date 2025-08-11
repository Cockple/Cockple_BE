package umc.cockple.demo.domain.image.dto;

import lombok.Builder;

public class ImageUploadDTO{
    @Builder
    public record Response(
            String imgUrl,
            String imgKey
    ) {}
}
