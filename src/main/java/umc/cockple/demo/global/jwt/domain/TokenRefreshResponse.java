package umc.cockple.demo.global.jwt.domain;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {
}
