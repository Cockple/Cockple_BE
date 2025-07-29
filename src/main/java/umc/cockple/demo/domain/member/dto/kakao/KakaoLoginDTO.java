package umc.cockple.demo.domain.member.dto.kakao;

import lombok.Builder;

public class KakaoLoginDTO {

    @Builder
    public record KakaoLoginRequestDTO(
            String code
    ){}

    @Builder
    public record KakaoLoginResponseDTO(
            String accessToken,
            Long memberId,
            String nickname,
            Boolean isNewMember
    ){}
}
