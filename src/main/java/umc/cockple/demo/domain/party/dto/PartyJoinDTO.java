package umc.cockple.demo.domain.party.dto;

import lombok.Builder;

import java.time.LocalDateTime;

public class PartyJoinDTO {

    @Builder
    public record Response(
            Long joinRequestId,
            Long userId,
            String nickname,
            String profileImageUrl,
            String gender,
            String level,
            LocalDateTime createdAt
    ) {
    }
}
