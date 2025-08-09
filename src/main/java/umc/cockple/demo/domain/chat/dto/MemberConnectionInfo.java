package umc.cockple.demo.domain.chat.dto;

import lombok.Builder;

@Builder
public record MemberConnectionInfo(
        Long memberId,
        String memberName
) {
}
