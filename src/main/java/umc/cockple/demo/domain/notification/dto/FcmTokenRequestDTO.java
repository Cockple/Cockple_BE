package umc.cockple.demo.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequestDTO(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String fcmToken
) {
}
