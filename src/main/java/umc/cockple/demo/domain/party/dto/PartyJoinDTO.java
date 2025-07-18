package umc.cockple.demo.domain.party.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public class PartyJoinDTO {

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Response(
            Long joinRequestId,
            Long userId,
            String nickname,
            String profileImageUrl,
            String gender,
            String level,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}